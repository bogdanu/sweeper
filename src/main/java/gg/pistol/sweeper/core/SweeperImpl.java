/*
 * Sweeper
 * Copyright (C) 2012 Bogdan Pistol
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package gg.pistol.sweeper.core;

import gg.pistol.sweeper.core.SweeperPoll.Mark;
import gg.pistol.sweeper.core.resource.Resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * Sweeper implementation.
 *
 * @author Bogdan Pistol
 */
public class SweeperImpl implements Sweeper {

    private final Analyzer analyzer;
    private boolean analyzed;

    private final Set<TargetImpl> toDeleteTargets;
    private final Set<TargetImpl> retainedTargets;

    @Nullable private NavigableSet<DuplicateGroup> duplicates;

    private int pollHistoryIdx = -1;
    private final List<Poll> polls;
    @Nullable private Poll currentPoll;

    @Nullable private SweeperCountImpl count;


    public SweeperImpl() throws SweeperException {
        this(new Analyzer());
    }

    SweeperImpl(Analyzer analyzer) throws SweeperException {
        Preconditions.checkNotNull(analyzer);

        this.analyzer = analyzer;
        toDeleteTargets = new LinkedHashSet<TargetImpl>();
        retainedTargets = new HashSet<TargetImpl>();
        polls = new ArrayList<Poll>();
    }

    public void analyze(Collection<? extends Resource> targetResources, SweeperOperationListener listener)
            throws SweeperAbortException {
        Preconditions.checkNotNull(targetResources);
        Preconditions.checkNotNull(listener);
        Preconditions.checkArgument(!targetResources.isEmpty(), "The targetResources is empty");

        analyzed = false;
        duplicates = analyzer.compute(targetResources, listener);

        toDeleteTargets.clear();
        retainedTargets.clear();

        pollHistoryIdx = -1;
        polls.clear();
        currentPoll = null;

        count = analyzer.getCount();
        analyzed = true;
    }

    public void abortAnalysis() {
        analyzer.abort();
    }

    public SweeperPoll nextPoll() {
        Preconditions.checkState(analyzed, "not analyzed");

        saveCurrentPoll();

        if (pollHistoryIdx != -1) {
            if (pollHistoryIdx + 1 < polls.size()) {
                pollHistoryIdx++;
                currentPoll = polls.get(pollHistoryIdx).clone();
                return currentPoll;
            }

            pollHistoryIdx = -1;
        }

        Poll next = null;
        DuplicateGroup dup = null;
        if (!polls.isEmpty()) {
            dup = polls.get(polls.size() - 1).getDuplicateGroup();
        }

        for (int i = 1; i < duplicates.size(); i++) {
            dup = getNextDuplicateGroup(dup);
            if (dup == null) {
                break;
            }

            next = generatePoll(dup);
            if (next != null) {
                break;
            }
        }

        updateCurrentPoll(next);
        return next;
    }

    private Poll generatePoll(DuplicateGroup dup) {
        List<Target> targets = new ArrayList<Target>();
        Set<Target> retained = new LinkedHashSet<Target>();

        for (Target t : dup.getTargets()) {
            Mark mark = getTargetAncestorMark((TargetImpl) t, true);
            if (mark != Mark.DELETE) {
                targets.add(t);
            }
            if (mark == Mark.RETAIN) {
                retained.add(t);
            }
        }

        if (targets.size() <= 1) {
            return null;
        }

        Poll poll = new Poll(dup, targets);
        if (retained.size() == 1) {
            poll.mark(retained.iterator().next(), Mark.RETAIN);
        }
        if (!retained.isEmpty()) {
            for (Target t : targets) {
                if (!retained.contains(t)) {
                    poll.mark(t, Mark.DELETE);
                }
            }
        }
        return poll;
    }

    private Mark getTargetAncestorMark(TargetImpl target, boolean includeTarget) {
        if (!includeTarget) {
            target = target.getParent();
        }
        while (target != null) {
            if (toDeleteTargets.contains(target)) {
                return Mark.DELETE;
            }
            if (retainedTargets.contains(target)) {
                return Mark.RETAIN;
            }
            target = target.getParent();
        }
        return Mark.DECIDE_LATER;
    }

    private void saveCurrentPoll() {
        if (currentPoll == null) {
            return;
        }
        currentPoll.close();

        if (pollHistoryIdx != -1) {
            if (polls.get(pollHistoryIdx).equals(currentPoll)) {
                return;
            }

            for (int i = polls.size() - 1; i >= pollHistoryIdx; i--) {
                undoPoll(polls.remove(i));
            }
        }

        polls.add(currentPoll);
        applyPoll(currentPoll);
    }

    private void undoPoll(Poll poll) {
        toDeleteTargets.removeAll(poll.getToDeleteTargets());
        retainedTargets.removeAll(poll.getRetainedTargets());
        updateToDeleteCount(poll, -1);
    }

    private void applyPoll(Poll poll) {
        toDeleteTargets.addAll(poll.getToDeleteTargets());
        retainedTargets.addAll(poll.getRetainedTargets());
        updateToDeleteCount(poll, +1);
    }

    private void updateToDeleteCount(Poll poll, int countSign) {
        for (TargetImpl t : poll.getToDeleteTargets()) {
            if (getTargetAncestorMark(t, false) != Mark.DELETE) {
                count.setToDeleteTargets(count.getToDeleteTargets() + countSign * t.getTotalTargets());
                count.setToDeleteTargetFiles(count.getToDeleteTargetFiles() + countSign * t.getTotalTargetFiles());
                count.setToDeleteSize(count.getToDeleteSize() + countSign * t.getSize());
            }
        }
    }

    @Nullable
    private DuplicateGroup getNextDuplicateGroup(@Nullable DuplicateGroup previous) {
        if (duplicates.isEmpty()) {
            return null;
        }

        if (previous == null) {
            return duplicates.first();
        }

        DuplicateGroup dup = duplicates.higher(previous);
        if (dup == null && duplicates.size() > 1) {
            dup = duplicates.first();
        }
        return dup;
    }

    public SweeperPoll previousPoll() {
        Preconditions.checkState(analyzed, "not analyzed");

        saveCurrentPoll();
        Poll previous = null;

        if (polls.size() > 1 && (pollHistoryIdx == -1 || pollHistoryIdx - 1 >= 0)) {
            if (pollHistoryIdx == -1) {
                pollHistoryIdx = polls.size() - 1;
            }
            pollHistoryIdx--;
            previous = polls.get(pollHistoryIdx).clone();
        }

        updateCurrentPoll(previous);
        return previous;
    }

    private void updateCurrentPoll(Poll newPoll) {
        if (newPoll != null) {
            currentPoll = newPoll;
        } else if (currentPoll != null) {
            currentPoll = currentPoll.clone();
            if (pollHistoryIdx == -1) {
                pollHistoryIdx = polls.size() - 1;
            }
        }
    }

    public SweeperPoll getCurrentPoll() {
        Preconditions.checkState(analyzed, "not analyzed");
        return currentPoll;
    }

    public SweeperCountImpl getCount() {
        Preconditions.checkState(analyzed, "not analyzed");
        return count;
    }

    public Collection<? extends Target> getToDeleteTargets() {
        Preconditions.checkState(analyzed, "not analyzed");
        return toDeleteTargets;
    }

    public void delete(Collection<? extends Target> toDeleteTargets, SweeperOperationListener listener) {
    }

    public void abortDeletion() {
    }

}
