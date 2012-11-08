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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * The poll provides access and de-duplication resolution methods to a {@link DuplicateGroup}.
 *
 * @author Bogdan Pistol
 */
// package private
class Poll implements SweeperPoll, Cloneable {

    private final DuplicateGroup duplicateGroup;

    // A duplicate group target subset that does not have ancestors marked for deletion.
    private final Set<? extends Target> targets;

    /*
     * Targets marked in this poll.
     */
    private final Set<TargetImpl> toDeleteTargets;
    private final Set<TargetImpl> retainedTargets;

    // Controls whether this poll can be marked anymore, a closed poll cannot be marked.
    private boolean opened;


    Poll(DuplicateGroup duplicateGroup, Collection<? extends Target> targets) {
        Preconditions.checkNotNull(duplicateGroup);
        Preconditions.checkNotNull(targets);
        Preconditions.checkArgument(!targets.isEmpty());

        this.duplicateGroup = duplicateGroup;
        this.targets = new LinkedHashSet<Target>(targets);
        toDeleteTargets = new LinkedHashSet<TargetImpl>();
        retainedTargets = new LinkedHashSet<TargetImpl>();
        opened = true;
    }

    public Collection<? extends Target> getTargets() {
        return targets;
    }

    public void mark(Target target, Mark mark) {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(mark);
        Preconditions.checkState(opened, "The poll is closed");
        Preconditions.checkState(targets.contains(target), "The target is not from this poll");

        switch (mark) {
            case DELETE:
                retainedTargets.remove(target);
                toDeleteTargets.add((TargetImpl) target);
                break;
            case RETAIN:
                toDeleteTargets.remove(target);
                retainedTargets.add((TargetImpl) target);
                break;
            case DECIDE_LATER:
                toDeleteTargets.remove(target);
                retainedTargets.remove(target);
                break;
        }
    }

    public Mark getMark(Target target) {
        Preconditions.checkNotNull(target);
        Preconditions.checkState(targets.contains(target), "The target is not from this poll");
        if (toDeleteTargets.contains(target)) {
            return Mark.DELETE;
        } else if (retainedTargets.contains(target)) {
            return Mark.RETAIN;
        } else {
            return Mark.DECIDE_LATER;
        }
    }

    void close() {
        opened = false;
    }

    Collection<TargetImpl> getToDeleteTargets() {
        return toDeleteTargets;
    }

    Collection<TargetImpl> getRetainedTargets() {
        return retainedTargets;
    }

    DuplicateGroup getDuplicateGroup() {
        return duplicateGroup;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Poll other = (Poll) obj;
        return duplicateGroup.equals(other.duplicateGroup) && targets.equals(other.targets)
                && toDeleteTargets.equals(other.toDeleteTargets) && retainedTargets.equals(other.retainedTargets);
    }

    @Override
    public Poll clone() {
        Poll clone = new Poll(duplicateGroup, targets);
        clone.toDeleteTargets.addAll(toDeleteTargets);
        clone.retainedTargets.addAll(retainedTargets);
        return clone;
    }

}
