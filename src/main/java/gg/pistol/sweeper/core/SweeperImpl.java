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

import java.util.Collection;
import java.util.List;
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

    @Nullable private List<DuplicateGroup> duplicates;
    @Nullable private SweeperCountImpl count;

    public SweeperImpl() throws SweeperException {
        analyzer = new Analyzer();
    }

    public void analyze(Set<Resource> targetResources, SweeperOperationListener listener) throws SweeperAbortException {
        Preconditions.checkNotNull(targetResources);
        Preconditions.checkNotNull(listener);
        Preconditions.checkArgument(!targetResources.isEmpty(), "The targetResources is empty");
        duplicates = null;
        count = null;

        duplicates = analyzer.compute(targetResources, listener);
        count = analyzer.getCount();
    }

    public void abortAnalysis() {
        analyzer.abort();
    }

    public SweeperPoll nextDuplicatePoll() {
        // TODO Auto-generated method stub
        return null;
    }

    public SweeperPoll previousDuplicatePoll() {
        // TODO Auto-generated method stub
        return null;
    }

    public SweeperCountImpl getCount() {
        Preconditions.checkState(count != null, "not analyzed");
        return count;
    }

    public Collection<Target> getToDeleteTargets() {
        // TODO Auto-generated method stub
        return null;
    }

    public void delete(SweeperOperationListener listener) {
        // TODO Auto-generated method stub
    }

    public void abortDeletion() {
        // TODO Auto-generated method stub
    }

}
