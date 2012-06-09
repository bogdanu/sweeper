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

import gg.pistol.lumberjack.JackLogger;
import gg.pistol.lumberjack.JackLoggerFactory;
import gg.pistol.sweeper.core.Target.Type;
import gg.pistol.sweeper.core.resource.Resource;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Analyzes a set of targets to find duplicates.
 * <p>
 * The {@link #compute(Set, SweeperOperationListener)} method is not thread safe and must be called from the same
 * thread or using synchronization techniques. The {@link #abort()} method is thread safe and can be called from any
 * thread.
 *
 * @author Bogdan Pistol
 */
// package private
class Analyzer {

    /*
     * Expand initially up to a limit and afterwards track the progress of expanding the remaining directories.
     * For more details see the traverseResources() method.
     */
    private static final int INITIAL_EXPAND_LIMIT = 100;

    private final JackLogger log;

    private final HashFunction hashFunction;

    // It is atomic to be able to abort the analysis from another thread.
    private final AtomicBoolean abortFlag;

    // Can be null when the compute() method is not called or aborted.
    @Nullable private SweeperCountImpl count;

    // the number of total targets calculated at the beginning (before sizing the targets) by traverseResources()
    private int totalTargets;


    Analyzer() throws SweeperException {
        try {
            hashFunction = new HashFunction();
        } catch (NoSuchAlgorithmException e) {
            throw new SweeperException(e);
        }
        abortFlag = new AtomicBoolean();
        log = JackLoggerFactory.getLogger(LoggerFactory.getLogger(Analyzer.class));
    }

    /**
     * Compute the analysis.
     *
     * @return the list of all {@link DuplicateGroup}s sorted decreasingly by size.
     */
    List<DuplicateGroup> compute(Set<Resource> targetResources, SweeperOperationListener listener)
            throws SweeperAbortException {
        Preconditions.checkNotNull(targetResources);
        Preconditions.checkNotNull(listener);
        Preconditions.checkArgument(!targetResources.isEmpty());

        log.trace("Computing the analysis for the resources {}", targetResources);
        abortFlag.set(false);
        OperationTrackingListener trackingListener = new OperationTrackingListener(listener);

        TargetImpl root = traverseResources(targetResources, trackingListener);
        Collection<TargetImpl> sized = computeSize(root, totalTargets, trackingListener);
        Multimap<Long, TargetImpl> sizeDups = filterDuplicateSize(sized, trackingListener);

        computeHash(sizeDups.values(), trackingListener);
        Multimap<String, TargetImpl> hashDups = filterDuplicateHash(sizeDups.values(), trackingListener);

        count = computeCount(root, hashDups, trackingListener);
        List<DuplicateGroup> duplicates = createDuplicateGroups(hashDups, trackingListener);
        return duplicates;
    }

    /**
     * Traverse the resources and expand them.
     * <p>
     * Because it is not known in advance how many resources need to be traversed, it is not possible to provide
     * accurate progress on the traversed resources. In order to have some estimative progress, the tree of resources
     * is traversed up to a limit {@link #INITIAL_EXPAND_LIMIT} without any progress indication and a collection of not
     * yet expanded targets is filled (the leafs of the limited tree traversal).
     * Afterwards the collection of not yet expanded targets is traversed with progress indication relative to the size
     * of collection.
     *
     * @return a root target that wraps the <code>targetResources</code>
     */
    private TargetImpl traverseResources(Set<Resource> targetResources, OperationTrackingListener listener)
            throws SweeperAbortException {
        log.trace("Traversing the resources");
        listener.updateOperation(SweeperOperation.RESOURCE_TRAVERSING);
        TargetImpl root = new TargetImpl(targetResources);
        totalTargets = 1;

        Collection<TargetImpl> nextTargets = new ArrayList<TargetImpl>();
        totalTargets += expand(root, INITIAL_EXPAND_LIMIT, nextTargets, listener);

        Iterator<TargetImpl> iterator = nextTargets.iterator();
        listener.setOperationMaxProgress(nextTargets.size());

        // expand with progress indication
        for (int i = 1; iterator.hasNext(); i++) {
            TargetImpl target = iterator.next();
            totalTargets += expand(target, -1, null, listener);
            listener.incrementOperationProgress(i);
        }

        listener.operationCompleted();
        return root;
    }

    /**
     * Expand recursively a target up to the specified <code>limit</code> (the limit value -1 specifies no limit).
     * <p>
     * This method has a side effect: if you specify a non-null <code>nextTargets</code> collection parameter then
     * the collection will be filled with the remaining not yet expanded targets (that will possibly exist only if
     * an expansion limit is specified).
     *
     * @return the number of traversed children targets
     */
    private int expand(TargetImpl target, int limit, @Nullable Collection<TargetImpl> nextTargets,
            OperationTrackingListener listener) throws SweeperAbortException {
        log.trace("Expanding {} with limit <{}>", target, limit);
        LinkedList<TargetImpl> next = new LinkedList<TargetImpl>();
        next.add(target);

        int targets = -1;  // exclude the first target from counting (only count the children)

        int levelSize = 1; // the number of targets on the current BFS level

        /*
         * BFS traversal of the tree of targets.
         * If there is a limit then the traversal will stop on the BFS level that has at least "limit" targets.
         */
        while (!next.isEmpty() && (limit == -1 || next.size() < limit || levelSize > 0)) {

            if (levelSize == 0) { // next BFS level reached
                levelSize = next.size();
            }

            TargetImpl currentTarget = next.poll();
            currentTarget.expand(listener);
            next.addAll(currentTarget.getChildren());

            levelSize--;
            targets++;
            checkAbortFlag();
        }

        if (nextTargets != null) {
            nextTargets.addAll(next);
        }

        // Count also all the not yet expanded targets, because every one of these will be expanded and not be
        // counted as the first target of the expansion.
        targets += next.size();
        return targets;
    }

    private void checkAbortFlag() throws SweeperAbortException {
        if (abortFlag.get()) {
            log.info("Detected that the abort flag is set");
            throw new SweeperAbortException();
        }
    }

    /**
     * Compute the size recursively with progress indication (the maximum progress is specified by
     * the <code>totalTargets</code> parameter).
     *
     * @return the collection of all the targets with computed size traversed from the <code>root</code>
     */
    private Collection<TargetImpl> computeSize(TargetImpl root, int totalTargets,
            final OperationTrackingListener listener) throws SweeperAbortException {
        log.trace("Computing the size for {} that has <{}> total sub-targets", root, totalTargets);
        final Collection<TargetImpl> ret = new ArrayList<TargetImpl>();
        listener.updateOperation(SweeperOperation.SIZE_COMPUTATION);
        listener.setOperationMaxProgress(totalTargets);

        traverseBottomUp(root, new TargetVisitorMethod() {
            public void visit(TargetImpl target, int targetIndex) {
                target.computeSize(listener);
                ret.add(target);
                listener.incrementOperationProgress(targetIndex);
            }
        });

        listener.operationCompleted();
        return ret;
    }

    /*
     * Bottom-up traversal of the tree of targets.
     *
     * For example the following tree has the bottom-up traversal: A, B, C, D, E, F, root.
     *
     *          --E
     *         /
     * root---F---D
     *     \
     *      --C---B
     *         \
     *          --A
     */
    private void traverseBottomUp(TargetImpl root, TargetVisitorMethod visitor) throws SweeperAbortException {
        Deque<TargetImpl> stack = new LinkedList<TargetImpl>(); // DFS style stack
        Set<TargetImpl> childrenPushed = new HashSet<TargetImpl>(); // targets with the children pushed on the stack
        stack.push(root);
        int targetIndex = 1; // counter for the n-th visited target

        while (!stack.isEmpty()) {
            TargetImpl target = stack.peek();

            if (!childrenPushed.contains(target)) {
                childrenPushed.add(target);
                for (TargetImpl child : target.getChildren()) {
                    stack.push(child);
                }
            } else {
                visitor.visit(target, targetIndex);
                targetIndex++;
                stack.pop();
            }
            checkAbortFlag();
        }
    }

    /**
     * Select all the targets for which there is at least another target with the same size.
     * <p>
     * The {@link Target.Type#ROOT} target is excluded.
     *
     * @return a multimap with sizes as keys and the targets with that same size as values for the key
     */
    private Multimap<Long, TargetImpl> filterDuplicateSize(Collection<TargetImpl> list,
            OperationTrackingListener listener) throws SweeperAbortException {
        log.trace("Deduplicating the size");
        listener.updateOperation(SweeperOperation.SIZE_DEDUPLICATION);

        Multimap<Long, TargetImpl> sizeDups = filterDuplicates(list, new Function<TargetImpl, Long>() {
            @Nullable public Long apply(TargetImpl input) {
                // all the null return values will be ignored
                return input.isSized() && input.getType() != Target.Type.ROOT ? input.getSize() : null;
            }
        });

        listener.operationCompleted();
        return sizeDups;
    }

    /**
     * Select all the duplicates from the targets based on a criteria function.
     * If the function returns the same value for two input targets then those targets are considered duplicates (in
     * the context of the criteria function).
     *
     * @return a multimap with function values as keys and the targets that are considered duplicates as values for
     * the key
     */
    private <T> Multimap<T, TargetImpl> filterDuplicates(Collection<TargetImpl> targets,
            Function<TargetImpl, T> indexFunction) throws SweeperAbortException {

        // Dumping all the targets into the multimap (Multimaps.index() doesn't work because it does not support
        // skipping null function values and also because of checking the abort flag).
        Multimap<T, TargetImpl> map = ArrayListMultimap.create();
        for (TargetImpl target : targets) {
            T key = indexFunction.apply(target);
            if (key != null) { // ignore null values
                map.put(key, target);
            }
            checkAbortFlag();
        }

        // Filtering the targets (Multimaps.filterKeys() and/or Multimaps.filterValues() don't work because of checking
        // the abort flag).
        Multimap<T, TargetImpl> ret = ArrayListMultimap.create();
        for (T key : map.keySet()) {
            checkAbortFlag();
            Collection<TargetImpl> collection = map.get(key);

            // Ignore all the targets that are not duplicates.
            if (collection.size() == 1) {
                continue;
            }

            // Ignore all the targets that are a single child of a directory. In this case the directory will represent
            // the child's content.
            Collection<TargetImpl> values = new ArrayList<TargetImpl>();
            for (TargetImpl target : collection) {
                if (target.getParent() == null || target.getParent().getChildren().size() > 1) {
                    values.add(target);
                }
            }

            if (values.size() > 1) {
                ret.putAll(key, values);
            }
        }
        return ret;
    }

    /**
     * Compute the hash recursively for the specified targets.
     */
    private void computeHash(Collection<TargetImpl> targets, final OperationTrackingListener listener)
            throws SweeperAbortException {
        log.trace("Computing the hash for targets {}", targets);
        listener.updateOperation(SweeperOperation.HASH_COMPUTATION);

        // Filter the targets that are not the children of other targets. All the children targets will have the hash
        // computed recursively from the parent target.
        targets = filterUpperTargets(targets);

        // Compute the total size of the targets to hash for progress tracking purposes.
        long totalHashSize = 0;
        for (TargetImpl target : targets) {
            totalHashSize += target.getSize();
            checkAbortFlag();
        }
        listener.setOperationMaxProgress(totalHashSize);

        for (TargetImpl target : targets) {
            traverseBottomUp(target, getHashVisitorMethod(listener));
        }
        listener.operationCompleted();
    }

    /**
     * Filter the targets that are not the children of other targets.
     *
     * @return the collection of filtered targets
     */
    private Collection<TargetImpl> filterUpperTargets(Collection<TargetImpl> targets) throws SweeperAbortException {
        Set<TargetImpl> set = new HashSet<TargetImpl>();
        Collection<TargetImpl> ret = new ArrayList<TargetImpl>();

        for (TargetImpl target : targets) {
            set.add(target);
            checkAbortFlag();
        }

        for (TargetImpl target : targets) {
            TargetImpl parent = target;

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

    private TargetVisitorMethod getHashVisitorMethod(final OperationTrackingListener listener) {
        return new TargetVisitorMethod() {
            long currentSize = 0;

            public void visit(TargetImpl target, int targetIndex) throws SweeperAbortException {
                target.computeHash(listener, hashFunction, abortFlag);

                // Keep track of file sizes only as directories only re-hash the hash of their children which should be
                // fast compared to reading I/O operations and hashing of potentially very large files.
                if (target.getType() == Type.FILE) {
                    currentSize += target.getSize();
                    listener.incrementOperationProgress(currentSize);
                }
            }
        };
    }

    /**
     * Select duplicate targets (having the same hash).
     *
     * @return a multimap with hashes as keys and duplicate targets as values for the key
     */
    private Multimap<String, TargetImpl> filterDuplicateHash(Collection<TargetImpl> targets,
            OperationTrackingListener listener) throws SweeperAbortException {
        log.trace("Deduplicating the hash for targets {}", targets);
        listener.updateOperation(SweeperOperation.HASH_DEDUPLICATION);

        Multimap<String, TargetImpl> hashDups = filterDuplicates(targets,
                new Function<TargetImpl, String>() {
                    @Nullable public String apply(TargetImpl input) {
                        // all the null return values will be ignored
                        return input.isHashed() ? input.getHash() : null;
                    }
                });

        listener.operationCompleted();
        return hashDups;
    }

    private SweeperCountImpl computeCount(TargetImpl root, Multimap<String, TargetImpl> hashDups,
            OperationTrackingListener listener) throws SweeperAbortException {
        log.trace("Counting the root {} and the hash duplicates {}", root, hashDups);
        listener.updateOperation(SweeperOperation.COUNTING);

        int totalTargets = root.getTotalTargets();
        int totalTargetFiles = root.getTotalFiles();
        long totalSize = root.getSize();

        int duplicateTargets = 0;
        int duplicateTargetFiles = 0;
        long duplicateSize = 0;

        // Filter the upper targets in order to have correct aggregate counting of duplicates. The hashDups can contain
        // targets that are children of other targets.
        Collection<TargetImpl> hashDupUpperTargets = filterUpperTargets(hashDups.values());

        // Group the duplicate targets by hash.
        Multimap<String, TargetImpl> dups = filterDuplicateHash(hashDupUpperTargets, OperationTrackingListener.NOOP_LISTENER);

        for (String key : dups.keySet()) {
            Iterator<TargetImpl> iterator = dups.get(key).iterator();

            // Jump over the first value from a duplicate group because deleting all the others will make this one
            // a non-duplicate.
            iterator.next();

            while (iterator.hasNext()) {
                TargetImpl target = iterator.next();
                duplicateTargets += target.getTotalTargets();
                duplicateTargetFiles += target.getTotalFiles();
                duplicateSize += target.getSize();

                checkAbortFlag();
            }
        }

        SweeperCountImpl count = new SweeperCountImpl(totalTargets, totalTargetFiles, totalSize, duplicateTargets,
                duplicateTargetFiles, duplicateSize);

        listener.operationCompleted();
        return count;
    }

    private List<DuplicateGroup> createDuplicateGroups(Multimap<String, TargetImpl> hashDups,
            OperationTrackingListener listener) throws SweeperAbortException {
        log.trace("Duplicate grouping");
        listener.updateOperation(SweeperOperation.DUPLICATE_GROUPING);

        List<DuplicateGroup> ret = new ArrayList<DuplicateGroup>();
        for (String key : hashDups.keySet()) {
            Collection<TargetImpl> values = hashDups.get(key);
            DuplicateGroup dup = new DuplicateGroup(values);
            ret.add(dup);
        }

        // Sort the groups of duplicates decreasingly based on the size (the sorting criteria and order is provided by
        // the Comparable interface implemented by DuplicateGroup).
        Collections.sort(ret);

        listener.operationCompleted();
        return ret;
    }

    /**
     * Abort the analysis.
     * <p>
     * This method is thread safe.
     */
    void abort() {
        log.trace("Aborting the analysis");
        abortFlag.set(true);
    }

    @Nullable
    SweeperCountImpl getCount() {
        return count;
    }

    /**
     * Visitor pattern interface for hierarchies of targets.
     */
    private static interface TargetVisitorMethod {
        void visit(TargetImpl target, int targetIndex) throws SweeperAbortException;
    }

}
