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

import gg.pistol.sweeper.core.SweeperTarget.Type;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class SweeperImpl implements Sweeper {
    
    private static final int INITIAL_EXPAND_LIMIT = 100;
    
    private Sweeper.State state = State.BEFORE_ANALYSIS;
    
    private SweeperTargetImpl rootTarget;
    
    private int operationProgress;
    
    private int phaseProgress;
    
    private int totalTargets;
    
    public void analyze(List<File> targets, SweeperOperationListener listener) {
        if (state != State.BEFORE_ANALYSIS) {
            throw new IllegalStateException("The current state should be " + State.BEFORE_ANALYSIS);
        }
        Preconditions.checkNotNull(targets);
        Preconditions.checkNotNull(listener);
        Preconditions.checkArgument(!targets.isEmpty(), "The targets list is empty");
        
        state = State.ANALYSIS;
        operationProgress = 0;
        phaseProgress = 0;
        totalTargets = 0;
        
        traverseFilesystem(targets, listener);
        List<SweeperTargetImpl> list = computeSize(listener);
        Multimap<Long, SweeperTargetImpl> sizeDups = computeDuplicateSize(list, listener);
        
        computeHash(sizeDups.values(), listener);
        Multimap<String, SweeperTargetImpl> hashDups = computeDuplicateHash(sizeDups.values(), listener);
    }

    private void traverseFilesystem(List<File> targets, SweeperOperationListener listener) {
        listener.updateOperationPhase(SweeperOperationPhase.FILESYSTEM_TRAVERSING);
        rootTarget = new SweeperTargetImpl(targets, listener);
        totalTargets += rootTarget.getChildren().size();
        
        List<SweeperTargetImpl> nodes = expand(rootTarget, INITIAL_EXPAND_LIMIT, listener);
        for (int i = 0; i < nodes.size(); i++) {
            expand(nodes.get(i), -1, listener);
            int progress = normalizeProgress(SweeperOperationPhase.FILESYSTEM_TRAVERSING, i + 1, nodes.size());
            updatePhaseProgress(listener, progress);
        }
        
        updateOperationProgress(listener, SweeperOperationPhase.FILESYSTEM_TRAVERSING);
    }
    
    private void updatePhaseProgress(SweeperOperationListener listener, int progress) {
        if (progress > phaseProgress) {
            phaseProgress = progress;
            listener.updateOperationProgress(operationProgress + phaseProgress);
        }
    }
    
    private void updateOperationProgress(SweeperOperationListener listener, SweeperOperationPhase phase) {
        operationProgress += phase.getPercentQuota();        
        if (phaseProgress < phase.getPercentQuota()) {
            listener.updateOperationProgress(operationProgress);
        }
        phaseProgress = 0;
    }
    
    private List<SweeperTargetImpl> computeSize(final SweeperOperationListener listener) {
        final List<SweeperTargetImpl> list = new ArrayList<SweeperTargetImpl>();
        listener.updateOperationPhase(SweeperOperationPhase.SIZE_COMPUTATION);
        
        traverseBottomUp(rootTarget, new TargetVisitorMethod() {
            public void visit(SweeperTargetImpl target, int idx) {
                target.computeSize(listener);
                int progress = normalizeProgress(SweeperOperationPhase.SIZE_COMPUTATION, idx, totalTargets);
                updatePhaseProgress(listener, progress);
            }
        });
        
        updateOperationProgress(listener, SweeperOperationPhase.SIZE_COMPUTATION);
        return list;
    }
    
    private <T> Multimap<T, SweeperTargetImpl> computeDuplicates(Iterable<SweeperTargetImpl> targets, Function<SweeperTargetImpl, T> indexFunction) {
        Multimap<T, SweeperTargetImpl> dups = Multimaps.index(targets, indexFunction);
        Iterator<T> iterator = dups.keySet().iterator();
        while (iterator.hasNext()) {
            T key = iterator.next();
            if (dups.get(key).size() == 1) {
                iterator.remove();
            }
        }
        return dups;
    }
    
    private Multimap<Long, SweeperTargetImpl> computeDuplicateSize(Iterable<SweeperTargetImpl> targets, SweeperOperationListener listener) {
        listener.updateOperationPhase(SweeperOperationPhase.SIZE_DEDUPLICATION);
        Multimap<Long, SweeperTargetImpl> sizeDups = computeDuplicates(targets, new Function<SweeperTargetImpl, Long>() {
            public Long apply(SweeperTargetImpl input) {
                return input.getSize();
            }
        });
        sizeDups.removeAll(SweeperTargetImpl.DEFAULT_SIZE);
        updateOperationProgress(listener, SweeperOperationPhase.SIZE_DEDUPLICATION);
        return sizeDups;
    }
    
    private Multimap<String, SweeperTargetImpl> computeDuplicateHash(Iterable<SweeperTargetImpl> targets, SweeperOperationListener listener) {
        listener.updateOperationPhase(SweeperOperationPhase.HASH_DEDUPLICATION);
        Multimap<String, SweeperTargetImpl> hashDups = computeDuplicates(targets, new Function<SweeperTargetImpl, String>() {
            public String apply(SweeperTargetImpl input) {
                return input.getHash();
            }
        });
        hashDups.removeAll(SweeperTargetImpl.DEFAULT_HASH);
        updateOperationProgress(listener, SweeperOperationPhase.HASH_DEDUPLICATION);
        return hashDups;
    }
    
    private void computeHash(Collection<SweeperTargetImpl> targets, final SweeperOperationListener listener) {
        listener.updateOperationPhase(SweeperOperationPhase.HASH_COMPUTATION);
        long totalSize = computeTotalSizeHash(targets);
        for (SweeperTargetImpl target : targets) {
            traverseBottomUp(target, getHashVisitorMethod(listener, totalSize));
        }
        updateOperationProgress(listener, SweeperOperationPhase.HASH_COMPUTATION);
    }

    private TargetVisitorMethod getHashVisitorMethod(final SweeperOperationListener listener, final long totalSize) {
        return new TargetVisitorMethod() {
            int currentSize = 0;
            
            public void visit(SweeperTargetImpl target, int idx) {
                target.computeHash(listener);
                if (target.getType() == Type.FILE) {
                    currentSize += target.getSize();
                    int progress = normalizeProgress(SweeperOperationPhase.HASH_COMPUTATION, currentSize, totalSize);
                    updatePhaseProgress(listener, progress);
                }
            }
        };
    }

    private long computeTotalSizeHash(Collection<SweeperTargetImpl> targets) {
        Set<SweeperTargetImpl> set = new HashSet<SweeperTargetImpl>(targets);
        long totalSize = 0;
        for (SweeperTargetImpl target : targets) {
            long targetSize = target.getSize();
            while (target.getParent() != null) {
                target = target.getParent();
                if (set.contains(target)) {
                    targetSize = 0;
                    break;
                }
            }
            totalSize += targetSize;
        }
        return totalSize;
    }
    
    private void traverseBottomUp(SweeperTargetImpl rootTarget, TargetVisitorMethod visitor) {
        Deque<SweeperTargetImpl> stack = new LinkedList<SweeperTargetImpl>();
        Set<SweeperTargetImpl> childrenPushed = new HashSet<SweeperTargetImpl>();
        stack.push(rootTarget);
        int i = 1;
        
        while (!stack.isEmpty()) {
            SweeperTargetImpl target = stack.peek();
            
            if (!childrenPushed.contains(target)) {
                childrenPushed.add(target);
                for (SweeperTargetImpl child : target.getChildren()) {
                    stack.push(child);
                }
            } else {
                visitor.visit(target, i);
                stack.pop();
            }
        }
    }
    
    private int normalizeProgress(SweeperOperationPhase action, long progress, long totalProgress) {
        return totalProgress == 0 ? 0 : (int) (action.getPercentQuota() * progress / totalProgress);
    }
    
    private List<SweeperTargetImpl> expand(SweeperTargetImpl root, int limit, SweeperOperationListener listener) {
        LinkedList<SweeperTargetImpl> next = new LinkedList<SweeperTargetImpl>();
        next.add(root);
        totalTargets--;
        int levelSize = 1;
        
        while (!next.isEmpty() && (limit == -1 || next.size() < limit || levelSize > 0)) {
            if (levelSize == 0) {
                levelSize = next.size();
            }
            
            SweeperTargetImpl target = next.poll();
            target.expand(listener);
            next.addAll(target.getChildren());
            
            levelSize--;
            totalTargets++;
        }
        return next;
    }

    public void abortAnalysis() {
        // TODO Auto-generated method stub

    }

    public int getDuplicateCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getSolvedDuplicateCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getDuplicateSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public long getSolvedDuplicateSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getPollCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getPollIndex() {
        // TODO Auto-generated method stub
        return 0;
    }

    public SweeperPoll nextPoll() {
        // TODO Auto-generated method stub
        return null;
    }

    public SweeperPoll previousPoll() {
        // TODO Auto-generated method stub
        return null;
    }

    public void delete(SweeperOperationListener listener) {
        // TODO Auto-generated method stub

    }

    public void abortDeletion() {
        // TODO Auto-generated method stub

    }

    public State getState() {
        // TODO Auto-generated method stub
        return null;
    }
    
    private static interface TargetVisitorMethod {
        
        public void visit(SweeperTargetImpl target, int idx);
        
    }

}
