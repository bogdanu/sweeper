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

/**
 * A collection of duplicate targets that need resolution.
 *
 * <p>A target can be marked in the following ways:
 *
 * <ul><li>Mark the target for deletion, the entire hierarchy of descendants will be considered undesired duplicates
 * and will not appear in future polls.</li>
 *
 * <li>Mark the target for retention, the entire hierarchy of descendants will be considered desired originals and will
 * have priority over other targets, they will appear in future polls only contending with other targets also marked
 * for retention.</li>
 *
 * <li>Do not mark the target at all, decide later.</li></ul>
 *
 * @author Bogdan Pistol
 */
public interface SweeperPoll {

    /**
     * Retrieve the collection of duplicate targets.
     *
     * @return the duplicate targets
     */
    Collection<Target> getTargets();

    /**
     * Mark the target for deletion and consider the hierarchy of descendants undesired duplicates.
     *
     * @param target
     *            the target of the mark
     */
    void markForDeletion(Target target);

    /**
     * Mark the target for retention and consider the hierarchy of descendants desired originals.
     *
     * @param target
     *            the target of the mark
     */
    void markForRetention(Target target);

}
