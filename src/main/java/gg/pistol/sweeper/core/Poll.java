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

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Preconditions;

// package private
class Poll implements SweeperPoll {

    private final Set<Target> targets;
    private final Set<TargetImpl> toDeleteTargets;
    private final Set<TargetImpl> retainedTargets;

    private boolean opened;


    Poll(DuplicateGroup duplicates) {
        Preconditions.checkNotNull(duplicates);

        targets = new TreeSet<Target>(duplicates.getTargets());
        toDeleteTargets = new TreeSet<TargetImpl>();
        retainedTargets = new TreeSet<TargetImpl>();
    }

    public Collection<Target> getTargets() {
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

    void open() {
        opened = true;
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

}
