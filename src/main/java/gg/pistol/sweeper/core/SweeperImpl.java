/*
 * Sweeper - Duplicate file cleaner
 * Copyright (C) 2012 Bogdan Ciprian Pistol
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

    /*
     * Marked targets.
     */
    private final Set<TargetImpl> toDeleteTargets;
    private final Set<TargetImpl> retainedTargets;

    // All the duplicates resulted from the analysis.
    @Nullable private NavigableSet<DuplicateGroup> duplicates;

    // Index used when walking the previous poll history.
    private int pollHistoryIdx = -1;

    // The stack of polls traversed.
    private final List<Poll> polls;

    // The currently opened poll.
    @Nullable private Poll currentPoll;

    @Nullable private SweeperCountImpl count;


    public SweeperImpl() throws SweeperException {
        this(new Analyzer());
    }

    // package private
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
        duplicates = analyzer.analyze(targetResources, listener);

        toDeleteTargets.clear();
        retainedTargets.clear();

        pollHistoryIdx = -1;
        polls.clear();
        currentPoll = null;

        count = analyzer.getCount();
        analyzed = true;
    }

    public void abortAnalysis() {
        analyzer.abortAnalysis();
    }

    public SweeperPoll nextPoll() {
        Preconditions.checkState(analyzed, "not analyzed");

        saveCurrentPoll();

        if (pollHistoryIdx != -1) {
            if (pollHistoryIdx + 1 < polls.size()) { // walking forward in the poll history
                pollHistoryIdx++;
                currentPoll = polls.get(pollHistoryIdx).clone();
                return currentPoll;
            }

            // Reached the last poll from history, the next step forward will not be from history anymore, so switching
            // the history index off.
            pollHistoryIdx = -1;
        }

        Poll next = null;
        DuplicateGroup dup = null;

        if (!polls.isEmpty()) {
            // In case there is a last poll, then start searching for the next duplicate group starting from
            // that last poll.
            dup = polls.get(polls.size() - 1).getDuplicateGroup();
        }

        /*
         *  Generate the next poll from the next duplicate group that has some targets not marked for deletion.
         *  In case this is the first poll then the first duplicate group will be good, otherwise search for a suitable
         *  duplicate group (wrapping around if the last one is reached) until a maximum of duplicates.size() - 1
         *  candidates are tried (to exclude the duplicate group of the current poll).
         */
        for (int i = 1; i < duplicates.size() || dup == null; i++) {
            dup = getNextDuplicateGroup(dup);
            if (dup == null) { // no more duplicates to solve
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

        // Use all the targets not marked for deletion (including their ancestors).
        for (Target t : dup.getTargets()) {
            Mark mark = getTargetAncestorMark((TargetImpl) t);
            if (mark != Mark.DELETE) {
                targets.add(t);
            }
            if (mark == Mark.RETAIN) {
                retained.add(t);
            }
        }

        if (targets.size() <= 1) { // to have duplication there is a need for minimum 2 targets
            return null;
        }

        Poll poll = new Poll(dup, targets);

        /*
         * In case there is more than one retained target it is necessary to de-duplicate the retained targets between
         * themselves and their default marking will be DECIDE_LATER. Otherwise if there is only one retained target it
         * will be marked with RETAIN.
         */
        if (retained.size() == 1) {
            poll.mark(retained.iterator().next(), Mark.RETAIN);
        }

        if (!retained.isEmpty()) {
            // If there are retained targets then all the other targets will be marked by default with DELETE.
            for (Target t : targets) {
                if (!retained.contains(t)) {
                    poll.mark(t, Mark.DELETE);
                }
            }
        }
        return poll;
    }

    private Mark getTargetAncestorMark(TargetImpl target) {
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
        currentPoll.close(); // to ensure no more changes will happen after saving

        if (pollHistoryIdx != -1) {
            if (polls.get(pollHistoryIdx).equals(currentPoll)) {
                // walk in the history with no changes
                return;
            }

            // The history was walked backwards and a change happened, this will make the current point in history
            // the latest one.
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

    /**
     * Update the delete counters. Depending on the provided {@code countSign} (+1 or -1) the counters will be
     * incremented or decremented.
     */
    private void updateToDeleteCount(Poll poll, int countSign) {
        /*
         * In case a target from the poll has a descendant that is already deleted then counting that target will also
         * include the descendant's counters. In this situation the counters of the already deleted descendants need to
         * be removed from the global "count" object. It is not possible to have a situation where a target from
         * the poll could have a deleted ancestor.
         */
        Set<TargetImpl> pollSet = new HashSet<TargetImpl>(poll.getToDeleteTargets());
        for (TargetImpl target : toDeleteTargets) {
            TargetImpl parent = target.getParent();
            while (parent != null) {
                if (pollSet.contains(parent)) {
                    // found a deleted descendant, removing its counters
                    count.setToDeleteTargets(count.getToDeleteTargets() - countSign * target.getTotalTargets());
                    count.setToDeleteTargetFiles(count.getToDeleteTargetFiles() - countSign * target.getTotalTargetFiles());
                    count.setToDeleteSize(count.getToDeleteSize() - countSign * target.getSize());
                }
                parent = parent.getParent();
            }
        }

        // counting the targets from the poll
        for (TargetImpl target : poll.getToDeleteTargets()) {
            count.setToDeleteTargets(count.getToDeleteTargets() + countSign * target.getTotalTargets());
            count.setToDeleteTargetFiles(count.getToDeleteTargetFiles() + countSign * target.getTotalTargetFiles());
            count.setToDeleteSize(count.getToDeleteSize() + countSign * target.getSize());
        }
    }

    /**
     * Return the next duplicate group after the provided {@code duplicateGroup}. This method wraps around when
     * reaching the end of the list of duplicate groups.
     */
    @Nullable
    private DuplicateGroup getNextDuplicateGroup(@Nullable DuplicateGroup duplicateGroup) {
        if (duplicates.isEmpty()) {
            return null;
        }

        if (duplicateGroup == null) {
            return duplicates.first();
        }

        DuplicateGroup dup = duplicates.higher(duplicateGroup);
        if (dup == null) {
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
                // the first step backwards in the history of polls
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
            // In case the newPoll is null and there is a currentPoll (saved and closed) then the currentPoll will
            // be updated to be a new clone of the saved currentPoll (the newly created clone will be opened and it
            // will be possible to mark targets).
            currentPoll = currentPoll.clone();

            // In case we are not in history mode then configure the history index to be the latest poll (when not in
            // history mode the latest poll is the current poll, this is the reason for configuring the history to be
            // the latest poll).
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

    public void delete(Collection<? extends Target> toDeleteTargets, SweeperOperationListener listener)
            throws SweeperAbortException {
        Preconditions.checkNotNull(toDeleteTargets);
        Preconditions.checkNotNull(listener);
        Preconditions.checkArgument(!toDeleteTargets.isEmpty());
        analyzer.delete(toDeleteTargets, listener);
    }

    public void abortDeletion() {
        analyzer.abortDeletion();
    }

}
