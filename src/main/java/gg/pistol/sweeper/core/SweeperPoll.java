/*
 * Sweeper - Duplicate file/folder cleaner
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

/**
 * A collection of duplicate targets that need resolution.
 *
 * <p>A target can be marked in the following ways:
 *
 * <ul><li>Mark the target for deletion, the entire hierarchy of descendants will be considered undesired duplicates
 * and will not appear in future polls.</li>
 *
 * <li>Mark the target for retention, the entire hierarchy of descendants will be considered desired originals and in
 * future polls they will have priority over other non-marked for retention targets (which will be marked by default
 * for deletion, although the default mark value can be changed if desired).</li>
 *
 * <li>Mark the target to decide later.</li></ul>
 *
 * @author Bogdan Pistol
 */
public interface SweeperPoll {

    /**
     * Retrieve the collection of duplicate targets.
     *
     * @return the duplicate targets
     */
    Collection<? extends Target> getTargets();

    /**
     * Mark the target.
     *
     * @param target
     *            the target
     * @param mark
     *            the target mark
     */
    void mark(Target target, Mark mark);

    /**
     * Retrieve the mark for the target.
     *
     *<p>The default value is computed by taking into account the previous polls. See the class description for more
     * details.
     *
     * @param target
     *            the target to query
     * @return the mark of the target
     */
    Mark getMark(Target target);

    /**
     * Target marks.
     */
    enum Mark {
        DELETE, RETAIN, DECIDE_LATER
    }

}
