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

import gg.pistol.sweeper.core.resource.Resource;

import java.util.Set;

import javax.annotation.Nullable;

/**
 * Duplicate file/directory cleaner.
 *
 * <p>This class is not thread safe and must be called from the same thread or using synchronization techniques,
 * the only exception are the {@link #abortAnalysis} and {@link #abortDeletion} methods which are thread safe and can
 * be called from any thread.
 *
 * @author Bogdan Pistol
 */
public interface Sweeper {

    /**
     * Perform an analysis to find duplicate targets.
     *
     * @param resources
     *            perform the analysis on these resources and their descendants
     * @param listener
     *            the provided listener will be called back with progress notifications
     * @throws SweeperAbortException
     *             in case the analysis is aborted this exception will be thrown
     */
    void analyze(Set<Resource> resources, SweeperOperationListener listener) throws SweeperAbortException;

    /**
     * Abort the analysis.
     *
     * <p>This method is thread safe, it can be called from another thread at the same time while performing
     * the analysis.
     */
    void abortAnalysis();

    boolean isAnalyzed();

    @Nullable
    SweeperPoll nextPoll();

    @Nullable
    SweeperPoll previousPoll();

    void delete(SweeperOperationListener listener) throws SweeperAbortException;

    void abortDeletion();

    SweeperCount getCount();

}
