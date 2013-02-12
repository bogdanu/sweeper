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

import gg.pistol.sweeper.core.resource.Resource;

import java.util.Collection;

import javax.annotation.Nullable;

/**
 * Duplicate file/directory cleaner.
 *
 * <p>The implementations of this interface are thread safe.
 *
 * <p>In order to clean a set of targets perform the following steps:
 *
 * <ol><li>Find the duplicates by calling the {@link #analyze} method on the resources.</li>
 * <li>Retrieve and resolve the duplicate polls with {@link #nextPoll}.</li>
 * <li>Optionally, to correct a previous choice it is possible to walk back with {@link #previousPoll}.</li>
 * <li>Retrieve and review the targets marked for deletion with {@link #getToDeleteTargets}.</li>
 * <li>Delete the undesired duplicate targets with {@link #delete}.</li></ol>
 *
 * @author Bogdan Pistol
 */
public interface Sweeper {

    /**
     * Perform an analysis to find duplicate targets. In case the analysis is already in progress then calling this
     * method will block until the currently running analysis finishes.
     *
     * @param resources
     *         perform the analysis on these resources and their descendants
     * @param listener
     *         the provided listener will be called back with progress notifications
     * @throws SweeperAbortException
     *         in case the analysis is aborted this exception will be thrown, afterwards the analysis can be
     *         restarted with a (possibly different) set of resources
     */
    void analyze(Collection<? extends Resource> resources, SweeperOperationListener listener) throws SweeperAbortException;

    /**
     * Request to abort the analysis. This method returns immediately, the analysis will be aborted as soon as possible.
     */
    void abortAnalysis();

    /**
     * Determine the next duplicate poll. In case the analysis is running then this method will block until the analysis
     * finishes.
     *
     * @return the next duplicate poll or {@code null} if there are no more polls
     */
    @Nullable
    SweeperPoll nextPoll();

    /**
     * Return to the previous poll. In case the analysis is running then this method will block until the analysis
     * finishes.
     *
     * @return the previous duplicate poll or {@code null} if there are no more previous polls
     */
    @Nullable
    SweeperPoll previousPoll();

    /**
     * Retrieve the current poll instance. In case the analysis is running then this method will block until the analysis
     * finishes.
     *
     * @return the current poll or {@code null} if there is no current poll yet
     */
    @Nullable
    SweeperPoll getCurrentPoll();

    /**
     * Retrieve counters for total targets, duplicate targets and to delete targets.
     *
     * @return the counters
     */
    SweeperCount getCount();

    /**
     * Retrieve the collection of targets marked for deletion.
     *
     * @return the targets marked for deletion as an unmodifiable collection
     */
    Collection<? extends Target> getToDeleteTargets();

    /**
     * Delete the provided {@code toDeleteTargets}. In case the analysis or the deletion is already in progress then
     * calling this method will block until the currently running operation finishes.
     *
     * <p><b>Warning</b>: this operation is not reversible. Aborting the deletion stops the operation, but does not
     * recover the already deleted targets.
     *
     * @param toDeleteTargets
     *         the targets of the delete operation
     * @param listener
     *         the provided listener will be called back with progress notifications
     * @throws SweeperAbortException
     *         in case the deletion is aborted this exception will be thrown
     */
    void delete(Collection<? extends Target> toDeleteTargets, SweeperOperationListener listener) throws SweeperAbortException;

    /**
     * Request to abort the deletion. This method returns immediately, the deletion will be aborted as soon as possible.
     * This operation only stops the deletion in progress, it does not recover the already deleted targets.
     */
    void abortDeletion();

}
