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
import gg.pistol.sweeper.core.resource.Resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

class SweeperAnalyzer {

    private static final int INITIAL_EXPAND_LIMIT = 100;

    private volatile boolean abort;

    @Nullable private SweeperCountImpl count;

    private int totalTargets;

    List<DuplicateTargetGroup> compute(Set<Resource> targetResources, OperationTrackingListener listener)
            throws SweeperAbortException {
        Preconditions.checkNotNull(targetResources);
        Preconditions.checkNotNull(listener);
        Preconditions.checkArgument(!targetResources.isEmpty());
        abort = false;

        SweeperTargetImpl root = traverseFilesystem(targetResources, listener);
        Collection<SweeperTargetImpl> sized = computeSize(root, totalTargets, listener);
        Multimap<Long, SweeperTargetImpl> sizeDups = filterDuplicateSize(sized, listener);

        computeHash(sizeDups.values(), listener);
        Multimap<String, SweeperTargetImpl> hashDups = filterDuplicateHash(sizeDups.values(), listener);

        count = computeCount(root, hashDups, listener);
        List<DuplicateTargetGroup> duplicates = createDuplicateGroups(hashDups, listener);
        return duplicates;
    }

    private SweeperTargetImpl traverseFilesystem(Set<Resource> targetResources, OperationTrackingListener listener)
            throws SweeperAbortException {
        listener.updateOperationPhase(SweeperOperationPhase.FILESYSTEM_TRAVERSING);
        SweeperTargetImpl root = new SweeperTargetImpl(targetResources);

        Collection<SweeperTargetImpl> nextTargets = new ArrayList<SweeperTargetImpl>();
        totalTargets = expand(root, INITIAL_EXPAND_LIMIT, nextTargets, listener);
        Iterator<SweeperTargetImpl> iterator = nextTargets.iterator();

        for (int i = 1; iterator.hasNext(); i++) {
            SweeperTargetImpl target = iterator.next();
            totalTargets += expand(target, -1, null, listener);
            listener.incrementPhase(SweeperOperationPhase.FILESYSTEM_TRAVERSING, i, nextTargets.size());
        }

        listener.incrementOperation(SweeperOperationPhase.FILESYSTEM_TRAVERSING);
        return root;
    }

    private int expand(SweeperTargetImpl root, int limit, Collection<SweeperTargetImpl> nextTargets,
            OperationTrackingListener listener) throws SweeperAbortException {
        LinkedList<SweeperTargetImpl> next = new LinkedList<SweeperTargetImpl>();
        next.add(root);
        int targets = -1;
        int levelSize = 1;

        while (!next.isEmpty() && (limit == -1 || next.size() < limit || levelSize > 0)) {
            if (levelSize == 0) {
                levelSize = next.size();
            }

            SweeperTargetImpl target = next.poll();
            target.expand(listener);
            next.addAll(target.getChildren());

            levelSize--;
            targets++;
            checkAbortFlag();
        }

        if (nextTargets != null) {
            nextTargets.addAll(next);
        }
        return targets;
    }

    private void checkAbortFlag() throws SweeperAbortException {
        if (abort) {
            throw new SweeperAbortException();
        }
    }

    private Collection<SweeperTargetImpl> computeSize(SweeperTargetImpl root, final int targets,
            final OperationTrackingListener listener) throws SweeperAbortException {
        final Collection<SweeperTargetImpl> ret = new ArrayList<SweeperTargetImpl>();
        listener.updateOperationPhase(SweeperOperationPhase.SIZE_COMPUTATION);

        traverseBottomUp(root, new TargetVisitorMethod() {
            public void visit(SweeperTargetImpl target, int idx) {
                target.computeSize(listener);
                ret.add(target);
                listener.incrementPhase(SweeperOperationPhase.SIZE_COMPUTATION, idx, targets);
            }
        });

        listener.incrementOperation(SweeperOperationPhase.SIZE_COMPUTATION);
        return ret;
    }

    private void traverseBottomUp(SweeperTargetImpl root, TargetVisitorMethod visitor) throws SweeperAbortException {
        Deque<SweeperTargetImpl> stack = new LinkedList<SweeperTargetImpl>();
        Set<SweeperTargetImpl> childrenPushed = new HashSet<SweeperTargetImpl>();
        stack.push(root);
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
                i++;
                stack.pop();
            }
            checkAbortFlag();
        }
    }

    private Multimap<Long, SweeperTargetImpl> filterDuplicateSize(Collection<SweeperTargetImpl> list,
            OperationTrackingListener listener) throws SweeperAbortException {
        listener.updateOperationPhase(SweeperOperationPhase.SIZE_DEDUPLICATION);

        Multimap<Long, SweeperTargetImpl> sizeDups = filterDuplicates(list, new Function<SweeperTargetImpl, Long>() {
            public Long apply(SweeperTargetImpl input) {
                return input.isSized() ? input.getSize() : null;
            }
        });

        listener.incrementOperation(SweeperOperationPhase.SIZE_DEDUPLICATION);
        return sizeDups;
    }

    private <T> Multimap<T, SweeperTargetImpl> filterDuplicates(Collection<SweeperTargetImpl> targets,
            Function<SweeperTargetImpl, T> indexFunction) throws SweeperAbortException {
        Multimap<T, SweeperTargetImpl> dups = ArrayListMultimap.create();
        for (SweeperTargetImpl target : targets) {
            T key = indexFunction.apply(target);
            if (key != null) {
                dups.put(key, target);
            }
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

    private void computeHash(Collection<SweeperTargetImpl> targets, final OperationTrackingListener listener)
            throws SweeperAbortException {
        listener.updateOperationPhase(SweeperOperationPhase.HASH_COMPUTATION);
        targets = filterUpperTargets(targets);

        long totalHashSize = 0;
        for (SweeperTargetImpl target : targets) {
            totalHashSize += target.getSize();
            checkAbortFlag();
        }

        for (SweeperTargetImpl target : targets) {
            traverseBottomUp(target, getHashVisitorMethod(listener, totalHashSize));
        }
        listener.incrementOperation(SweeperOperationPhase.HASH_COMPUTATION);
    }

    private List<SweeperTargetImpl> filterUpperTargets(Collection<SweeperTargetImpl> targets)
            throws SweeperAbortException {
        Set<SweeperTargetImpl> set = new HashSet<SweeperTargetImpl>();
        List<SweeperTargetImpl> ret = new ArrayList<SweeperTargetImpl>();

        for (SweeperTargetImpl target : targets) {
            set.add(target);
            checkAbortFlag();
        }

        for (SweeperTargetImpl target : targets) {
            SweeperTargetImpl parent = target;

            boolean isUpper = true;
            while ((parent = parent.getParent()) != null) {
                if (set.contains(parent)) {
                    isUpper = false;
                    break;
                }
            }
            if (isUpper) {
                ret.add(target);
            }

            checkAbortFlag();
        }

        return ret;
    }

    private TargetVisitorMethod getHashVisitorMethod(final OperationTrackingListener listener, final long totalHashSize) {
        return new TargetVisitorMethod() {
            long currentSize = 0;

            public void visit(SweeperTargetImpl target, int idx) {
                target.computeHash(listener);
                if (target.getType() == Type.FILE) {
                    currentSize += target.getSize();
                    listener.incrementPhase(SweeperOperationPhase.HASH_COMPUTATION, currentSize, totalHashSize);
                }
            }
        };
    }

    private Multimap<String, SweeperTargetImpl> filterDuplicateHash(Collection<SweeperTargetImpl> targets,
            OperationTrackingListener listener) throws SweeperAbortException {
        listener.updateOperationPhase(SweeperOperationPhase.HASH_DEDUPLICATION);

        Multimap<String, SweeperTargetImpl> hashDups = filterDuplicates(targets,
                new Function<SweeperTargetImpl, String>() {
                    public String apply(SweeperTargetImpl input) {
                        return input.isHashed() ? input.getHash() : null;
                    }
                });

        listener.incrementOperation(SweeperOperationPhase.HASH_DEDUPLICATION);
        return hashDups;
    }

    private SweeperCountImpl computeCount(SweeperTargetImpl root, Multimap<String, SweeperTargetImpl> hashDups,
            OperationTrackingListener listener) throws SweeperAbortException {
        listener.updateOperationPhase(SweeperOperationPhase.COUNTING);

        int totalTargets = root.getTotalTargets();
        int totalFiles = root.getTotalFiles();
        long totalSize = root.getSize();

        int totalDuplicateTargets = 0;
        int totalDuplicateFiles = 0;
        long totalDuplicateSize = 0;
        List<SweeperTargetImpl> dups = filterUpperTargets(hashDups.values());

        for (SweeperTargetImpl target : dups) {
            totalDuplicateTargets += target.getTotalTargets();
            totalDuplicateFiles += target.getTotalFiles();
            totalDuplicateSize += target.getSize();

            checkAbortFlag();
        }

        SweeperCountImpl count = new SweeperCountImpl(totalTargets, totalFiles, totalSize, totalDuplicateTargets,
                totalDuplicateFiles, totalDuplicateSize);

        listener.incrementOperation(SweeperOperationPhase.COUNTING);
        return count;
    }

    private List<DuplicateTargetGroup> createDuplicateGroups(Multimap<String, SweeperTargetImpl> hashDups,
            OperationTrackingListener listener) throws SweeperAbortException {
        listener.updateOperationPhase(SweeperOperationPhase.DUPLICATE_GROUPING);

        List<DuplicateTargetGroup> ret = new ArrayList<DuplicateTargetGroup>();
        for (String key : hashDups.keySet()) {
            Collection<SweeperTargetImpl> values = hashDups.get(key);
            values = filterUpperTargets(values);

            DuplicateTargetGroup dup = new DuplicateTargetGroup(values);
            ret.add(dup);
        }

        Collections.sort(ret);

        listener.incrementOperation(SweeperOperationPhase.DUPLICATE_GROUPING);
        return ret;
    }

    void abort() {
        abort = true;
    }

    SweeperCountImpl getCount() {
        return count;
    }

    private static interface TargetVisitorMethod {
        void visit(SweeperTargetImpl target, int idx);
    }

}
