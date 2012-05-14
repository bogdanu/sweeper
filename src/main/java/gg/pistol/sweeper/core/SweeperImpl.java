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
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class SweeperImpl implements Sweeper {
    
    private static final int INITIAL_EXPAND_LIMIT = 100;
    
    private final Lock lock = new ReentrantLock();
    
    private volatile boolean abort;
    
    private Sweeper.State state = State.BEFORE_ANALYSIS;
    
    private SweeperTargetImpl rootTarget;
    
    private int operationProgress;
    
    private int phaseProgress;
    
    private int totalTargets;
    
    private List<DuplicateTargetGroup> duplicates;
    
    public void analyze(List<File> targets, SweeperOperationListener listener) {
        Preconditions.checkNotNull(targets);
        Preconditions.checkNotNull(listener);
        Preconditions.checkArgument(!targets.isEmpty(), "The targets list is empty");
        
        lock.lock();
        try {
            doAnalyze(targets, listener);
            listener.operationFinished();
        } catch (SweeperAbortException e) {
            state = State.BEFORE_ANALYSIS;
            listener.operationAborted();
        } finally {
            lock.unlock();
        }
    }
    
    private void doAnalyze(List<File> targets, SweeperOperationListener listener) throws SweeperAbortException {
        if (state != State.BEFORE_ANALYSIS) {
            throw new IllegalStateException("The current state should be " + State.BEFORE_ANALYSIS);
        }
        
        abort = false;
        state = State.ANALYSIS;
        operationProgress = 0;
        phaseProgress = 0;
        totalTargets = 0;
        
        traverseFilesystem(targets, listener);
        List<SweeperTargetImpl> list = computeSize(listener);
        Multimap<Long, SweeperTargetImpl> sizeDups = computeDuplicateSize(list, listener);
        
        computeHash(sizeDups.values(), listener);
        duplicates = computeDuplicateHash(sizeDups.values(), listener);
        state = State.POLL;
    }

    private void traverseFilesystem(List<File> targets, SweeperOperationListener listener) throws SweeperAbortException {
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
    
    private List<SweeperTargetImpl> computeSize(final SweeperOperationListener listener) throws SweeperAbortException {
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
    
    private <T> Multimap<T, SweeperTargetImpl> computeDuplicates(Iterable<SweeperTargetImpl> targets, Function<SweeperTargetImpl, T> indexFunction) throws SweeperAbortException {
        Multimap<T, SweeperTargetImpl> dups = ArrayListMultimap.create();
        for (SweeperTargetImpl target : targets) {
            dups.put(indexFunction.apply(target), target);
            checkAbortFlag();
        }
        
        Iterator<T> iterator = dups.keySet().iterator();
        while (iterator.hasNext()) {
            T key = iterator.next();
            if (dups.get(key).size() == 1) {
                iterator.remove();
            }
            checkAbortFlag();
        }
        return dups;
    }
    
    private Multimap<Long, SweeperTargetImpl> computeDuplicateSize(Iterable<SweeperTargetImpl> targets, SweeperOperationListener listener) throws SweeperAbortException {
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
    
    private List<DuplicateTargetGroup> computeDuplicateHash(Iterable<SweeperTargetImpl> targets, SweeperOperationListener listener) throws SweeperAbortException {
        listener.updateOperationPhase(SweeperOperationPhase.HASH_DEDUPLICATION);
        
        Multimap<String, SweeperTargetImpl> hashDups = computeDuplicates(targets, new Function<SweeperTargetImpl, String>() {
            public String apply(SweeperTargetImpl input) {
                return input.getHash();
            }
        });
        hashDups.removeAll(SweeperTargetImpl.DEFAULT_HASH);
        
        List<DuplicateTargetGroup> list = new ArrayList<DuplicateTargetGroup>();
        for (String key : hashDups.keySet()) {
            Collection<SweeperTargetImpl> dups = hashDups.get(key);
            Set<SweeperTargetImpl> childTargets = filterChildTargets(dups);
            dups.removeAll(childTargets);
            
            list.add(new DuplicateTargetGroup(dups));
            checkAbortFlag();
        }
        Collections.sort(list);
        
        updateOperationProgress(listener, SweeperOperationPhase.HASH_DEDUPLICATION);
        return list;
    }
    
    private void computeHash(Collection<SweeperTargetImpl> targets, final SweeperOperationListener listener) throws SweeperAbortException {
        listener.updateOperationPhase(SweeperOperationPhase.HASH_COMPUTATION);
        long totalHashSize = 0;
        for (SweeperTargetImpl target : targets) {
            totalHashSize += target.getSize();
            checkAbortFlag();
        }
        
        Set<SweeperTargetImpl> childTargets = filterChildTargets(targets);
        for (SweeperTargetImpl target : childTargets) {
            totalHashSize -= target.getSize();
            checkAbortFlag();
        }
        
        for (SweeperTargetImpl target : targets) {
            traverseBottomUp(target, getHashVisitorMethod(listener, totalHashSize));
        }
        updateOperationProgress(listener, SweeperOperationPhase.HASH_COMPUTATION);
    }

    private TargetVisitorMethod getHashVisitorMethod(final SweeperOperationListener listener, final long totalHashSize) {
        return new TargetVisitorMethod() {
            int currentSize = 0;
            
            public void visit(SweeperTargetImpl target, int idx) {
                target.computeHash(listener);
                if (target.getType() == Type.FILE) {
                    currentSize += target.getSize();
                    int progress = normalizeProgress(SweeperOperationPhase.HASH_COMPUTATION, currentSize, totalHashSize);
                    updatePhaseProgress(listener, progress);
                }
            }
        };
    }
    
    private Set<SweeperTargetImpl> filterChildTargets(Collection<SweeperTargetImpl> targets) throws SweeperAbortException {
        Set<SweeperTargetImpl> set = new HashSet<SweeperTargetImpl>(targets);
        Set<SweeperTargetImpl> ret = new LinkedHashSet<SweeperTargetImpl>();

        for (SweeperTargetImpl target : targets) {
            while (target.getParent() != null) {
                target = target.getParent();
                if (set.contains(target)) {
                    ret.add(target);
                    break;
                }
            }
            checkAbortFlag();
        }
        return ret;
    }

    private void checkAbortFlag() throws SweeperAbortException {
        if (abort) {
            throw new SweeperAbortException();
        }
    }
    
    private void traverseBottomUp(SweeperTargetImpl rootTarget, TargetVisitorMethod visitor) throws SweeperAbortException {
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
            checkAbortFlag();
        }
    }
    
    private int normalizeProgress(SweeperOperationPhase action, long progress, long totalProgress) {
        return totalProgress == 0 ? 0 : (int) (action.getPercentQuota() * progress / totalProgress);
    }
    
    private List<SweeperTargetImpl> expand(SweeperTargetImpl root, int limit, SweeperOperationListener listener) throws SweeperAbortException {
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
            checkAbortFlag();
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
