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
import gg.pistol.sweeper.core.resource.ResourceDirectory;
import gg.pistol.sweeper.core.resource.ResourceFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.joda.time.DateTime;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.io.Closeables;

// package private
class TargetImpl implements Target {

    private final String name;
    private final Type type;
    @Nullable private final Resource resource;
    @Nullable private final TargetImpl parent;

    private final Collection<TargetImpl> children;

    /*
     * The size, the total targets, the total targets that are files (totalTargetFiles), the hash and the modification
     * date are counted recursively by taking into account all the children.
     */
    private long size;
    private int totalTargets;
    private int totalTargetFiles;
    @Nullable private String hash;
    @Nullable private DateTime modificationDate;

    /*
     * Partially expanded, partially sized and partially hashed are representing states when the expand(), computeSize()
     * or computeHash() operations have been called but exceptions prevented the full computation and the full states
     * expanded, sized or hashed are false.
     */
    private boolean partiallyExpanded;
    private boolean expanded;
    private boolean partiallySized;
    private boolean sized;
    private boolean partiallyHashed;
    private boolean hashed;


    TargetImpl(Set<? extends Resource> targetResources) {
        Preconditions.checkNotNull(targetResources);
        Preconditions.checkArgument(!targetResources.isEmpty(), "targetResources is empty");

        name = "";
        type = Type.ROOT;
        resource = null;
        parent = null;
        children = new ArrayList<TargetImpl>();

        partiallyExpanded = true;
        expanded = true;
        doExpand(targetResources);
    }

    TargetImpl(Resource targetResource, TargetImpl parent) {
        Preconditions.checkNotNull(targetResource);
        Preconditions.checkNotNull(parent);

        name = targetResource.getName();
        resource = targetResource;
        this.parent = parent;

        if (resource instanceof ResourceFile) {
            type = Type.FILE;
            children = Collections.emptyList();
            partiallyExpanded = true;
            expanded = true;
        } else if (resource instanceof ResourceDirectory) {
            type = Type.DIRECTORY;
            children = new ArrayList<TargetImpl>();
        } else {
            throw new IllegalArgumentException("targetResource class <" + resource.getClass().getSimpleName() +
                    "> should be a ResourceFile or a ResourceDirectory");
        }
    }

    /**
     * Expand the immediate children.
     */
    void expand(OperationTrackingListener listener) {
        Preconditions.checkNotNull(listener);
        if (isPartiallyExpanded()) {
            return;
        }
        partiallyExpanded = true;
        expanded = true;
        listener.updateTargetAction(this, TargetAction.EXPAND);

        ResourceDirectory.ResourceCollectionResponse response = ((ResourceDirectory) resource).getSubresources();
        if (!response.getExceptions().isEmpty()) {
            expanded = false;
            for (Exception e : response.getExceptions()) {
                listener.updateTargetException(this, TargetAction.EXPAND, new SweeperException(e));
            }
        }
        doExpand(response.getResources());
    }

    private void doExpand(Collection<? extends Resource> targetResources) {
        for (Resource res : targetResources) {
            TargetImpl child = new TargetImpl(res, this);
            children.add(child);
        }
    }

    /**
     * Compute the size. In case this is a directory and there are errors computing the size of the children, then
     * the computed size will include only the available children for estimation purposes.
     *
     * <p>The {@link #expand} method must have been called previously.
     */
    void computeSize(OperationTrackingListener listener) {
        Preconditions.checkNotNull(listener);
        Preconditions.checkState(isPartiallyExpanded(), "Should be partially expanded");
        if (isPartiallySized()) {
            return;
        }
        partiallySized = true;
        listener.updateTargetAction(this, TargetAction.COMPUTE_SIZE);

        sized = isExpanded();
        try {
            if (type == Type.FILE) {
                totalTargets = 1;
                totalTargetFiles = 1;
                size = ((ResourceFile) resource).getSize();
            } else {
                boolean dirSized = computeDirectorySize();
                sized = sized && dirSized;
            }
        } catch (IllegalStateException e) {
            partiallySized = false;
            sized = false;
            throw e;
        } catch (RuntimeException e) {
            sized = false;
            listener.updateTargetException(this, TargetAction.COMPUTE_SIZE, new SweeperException(e));
        }
    }

    /**
     * Compute the size of the directory and return whether all the children were sized.
     */
    private boolean computeDirectorySize() {
        size = 0;
        totalTargets = type == Type.ROOT ? 0 : 1;
        totalTargetFiles = 0;
        boolean ret = true;

        for (TargetImpl child : getChildren()) {
            Preconditions.checkState(child.isPartiallySized(), "All the children need to be partially sized");
            size += child.getSize();
            totalTargets += child.getTotalTargets();
            totalTargetFiles += child.getTotalTargetFiles();
            if (!child.isSized()) {
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Compute the hash and the last modified date.
     *
     * <p>The {@link #computeSize} method must have been called previously.
     */
    void computeHash(HashFunction hashFunction, OperationTrackingListener listener, AtomicBoolean abortFlag)
            throws SweeperAbortException {
        Preconditions.checkNotNull(hashFunction);
        Preconditions.checkNotNull(listener);
        Preconditions.checkNotNull(abortFlag);
        Preconditions.checkState(isSized(), "Not sized");
        if (isPartiallyHashed()) {
            return;
        }
        partiallyHashed = true;
        listener.updateTargetAction(this, TargetAction.COMPUTE_HASH);

        hashed = true;
        try {
            if (type == Type.FILE) {
                computeFileHash(hashFunction, listener, abortFlag);
            } else {
                computeDirectoryHash(hashFunction, abortFlag);
            }
        } catch (SweeperAbortException e) {
            partiallyHashed = false;
            hashed = false;
            throw e;
        } catch (IllegalStateException e) {
            partiallyHashed = false;
            hashed = false;
            throw e;
        } catch (Exception e) {
            hashed = false;
            listener.updateTargetException(this, TargetAction.COMPUTE_HASH, new SweeperException(e));
        }
    }

    private void computeFileHash(HashFunction hashFunction, OperationTrackingListener listener, AtomicBoolean abort)
            throws IOException, SweeperAbortException {
        ResourceFile res = (ResourceFile) resource;

        modificationDate = res.getModificationDate();
        InputStream stream = res.getInputStream();
        try {
            hash = getSize() + hashFunction.compute(stream, listener, abort);
        } finally {
            Closeables.closeQuietly(stream);
        }
    }

    private void computeDirectoryHash(HashFunction hashFunction, AtomicBoolean abortFlag)
            throws IOException, SweeperAbortException, SweeperException {
        modificationDate = null;
        List<String> hashes = new ArrayList<String>();

        for (TargetImpl child : getChildren()) {
            Preconditions.checkState(child.isPartiallyHashed(), "All the children need to be partially hashed");
            if (!child.isHashed()) {
                throw new SweeperException("Cannot compute hash because at least one child resource could not be hashed");
            }
            if (child.getSize() == 0) {
                continue;
            }
            if (modificationDate == null || child.getModificationDate().isAfter(modificationDate)) {
                modificationDate = child.getModificationDate();
            }
            hashes.add(child.getHash());
        }
        if (hashes.size() == 1) {
            hash = hashes.get(0);
        } else {
            Collections.sort(hashes);
            ByteArrayInputStream stream = new ByteArrayInputStream(Joiner.on("-").join(hashes).getBytes());
            hash = getSize() + hashFunction.compute(stream, OperationTrackingListener.NOOP_LISTENER, abortFlag);
        }
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TargetImpl other = (TargetImpl) obj;
        return name.equals(other.name);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("name", name).toString();
    }

    public int compareTo(Target other) {
        Preconditions.checkNotNull(other);
        return ComparisonChain.start().compare(name, other.getName()).result();
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    @Nullable
    public Resource getResource() {
        return resource;
    }

    @Nullable
    TargetImpl getParent() {
        return parent;
    }

    Collection<TargetImpl> getChildren() {
        return children;
    }

    public long getSize() {
        Preconditions.checkState(isPartiallySized(), "not computed");
        return size;
    }

    int getTotalTargets() {
        Preconditions.checkState(isPartiallySized(), "not computed");
        return totalTargets;
    }

    int getTotalTargetFiles() {
        Preconditions.checkState(isPartiallySized(), "not computed");
        return totalTargetFiles;
    }

    String getHash() {
        Preconditions.checkState(isHashed(), "not computed");
        return hash;
    }

    @Nullable
    public DateTime getModificationDate() {
        Preconditions.checkState(isHashed(), "not computed");
        return modificationDate;
    }

    boolean isPartiallyExpanded() {
        return partiallyExpanded;
    }

    boolean isExpanded() {
        return expanded;
    }

    boolean isPartiallySized() {
        return partiallySized;
    }

    boolean isSized() {
        return sized;
    }

    boolean isPartiallyHashed() {
        return partiallyHashed;
    }

    boolean isHashed() {
        return hashed;
    }

}
