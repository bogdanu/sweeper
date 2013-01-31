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

/**
 * Listener for {@link Sweeper} operation progress.
 *
 * @author Bogdan Pistol
 */
public interface SweeperOperationListener {

    /**
     * Notification that a new operation is started.
     *
     * @param operation
     *         the newly started operation
     */
    void updateOperation(SweeperOperation operation);

    /**
     * Indication that the operation progressed further.
     *
     * @param progress
     *         the current progress of the operation
     * @param maxProgress
     *         the maximum progress that the current operation will reach when completed
     * @param percentGlobal
     *         the global percentage of completion of all the operations
     */
    void updateOperationProgress(long progress, long maxProgress, int percentGlobal);

    /**
     * Notification that the specified <code>target</code> is the subject of the operation.
     *
     * @param target
     *         the subject of the operation
     */
    void updateTarget(Target target);

    /**
     * Notification that an exception occurred while executing the operation for a target.
     *
     * @param target
     *         the subject of the operation
     * @param e
     *         the encountered exception
     */
    void updateException(Target target, SweeperException e);

}
