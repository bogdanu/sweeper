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
import java.text.Normalizer.Form;
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

    private long size;
    private int totalTargets;
    private int totalTargetFiles;

    @Nullable private String hash;
    @Nullable private DateTime modificationDate;

    @Nullable private DuplicateGroup duplicateTargetGroup;
    private Mark mark = Mark.DECIDE_LATER;
    private boolean poll;

    private boolean partiallyExpanded;
    private boolean expanded;
    private boolean partiallySized;
    private boolean sized;
    private boolean partiallyHashed;
    private boolean hashed;

    TargetImpl(Set<Resource> targetResources) {
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

        resource = targetResource;
        name = resource.getName();
        this.parent = parent;

        if (resource instanceof ResourceFile) {
            type = Type.FILE;
            partiallyExpanded = true;
            expanded = true;
            children = Collections.emptyList();
        } else if (resource instanceof ResourceDirectory) {
            type = Type.DIRECTORY;
            children = new ArrayList<TargetImpl>();
        } else {
            throw new IllegalArgumentException("targetResource class <" + resource.getClass().getSimpleName() + "> should be a ResourceFile or a ResourceDirectory");
        }
    }

    /**
     * Expands the immediate children
     */
    void expand(SweeperOperationListener listener) {
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

    private void doExpand(Collection<Resource> targetResources) {
        for (Resource res : targetResources) {
            TargetImpl child = new TargetImpl(res, this);
            children.add(child);
        }
    }

    /**
     * Compute the size.
     * <p>
     * The {@link #expand()} method must have been called.
     */
    void computeSize(SweeperOperationListener listener) {
        Preconditions.checkNotNull(listener);
        Preconditions.checkState(isPartiallyExpanded(), "Should be expanded");
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
                sized = sized && computeDirectorySize();
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

    private boolean computeDirectorySize() {
        size = 0;
        totalTargets = type == Type.ROOT ? 0 : 1;
        totalTargetFiles = 0;
        boolean ret = true;
        for (TargetImpl child : getChildren()) {
            Preconditions.checkState(child.isPartiallySized(), "All the children need to be size computed");
            size += child.getSize();
            totalTargets += child.getTotalTargets();
            totalTargetFiles += child.getTotalFiles();
            if (!child.isSized()) {
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Computes the hash and the last modified date.
     * <p>
     * The {@link #computeSize()} method must have been called.
     */
    void computeHash(OperationTrackingListener listener, HashFunction hashFunction, AtomicBoolean abort) throws SweeperAbortException {
        Preconditions.checkNotNull(listener);
        Preconditions.checkNotNull(hashFunction);
        Preconditions.checkNotNull(abort);
        Preconditions.checkState(isPartiallySized(), "Size not computed");
        if (isPartiallyHashed()) {
            return;
        }
        partiallyHashed = true;
        listener.updateTargetAction(this, TargetAction.COMPUTE_HASH);
        if (!isSized()) {
            listener.updateTargetException(this, TargetAction.COMPUTE_HASH, new SweeperException("Cannot compute hash as the size operation failed"));
            return;
        }

        hashed = true;
        try {
            if (type == Type.FILE) {
                computeFileHash(listener, hashFunction, abort);
            } else {
                computeDirectoryHash(hashFunction);
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

    private void computeFileHash(OperationTrackingListener listener, HashFunction hashFunction, AtomicBoolean abort) throws IOException, SweeperAbortException {
        ResourceFile res = (ResourceFile) resource;

        modificationDate = res.getModificationDate();
        InputStream stream = res.getInputStream();
        try {
            hash = getSize() + hashFunction.compute(stream, listener, abort);
        } finally {
            Closeables.closeQuietly(stream);
        }
    }

    private void computeDirectoryHash(HashFunction hashFunction) throws IOException, SweeperAbortException {
        modificationDate = null;
        List<String> hashes = new ArrayList<String>();
        for (TargetImpl child : getChildren()) {
            Preconditions.checkState(child.isPartiallyHashed(), "All the children need to be hash computed");
            if (!child.isHashed()) {
                throw new IOException("Cannot compute hash as the children experienced errors while hashing");
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
            hash = getSize() + hashFunction.compute(stream);
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

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

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

    int getTotalFiles() {
        Preconditions.checkState(isPartiallySized(), "not computed");
        return totalTargetFiles;
    }

    String getHash() {
        Preconditions.checkState(isHashed(), "not computed");
        return hash;
    }

    public DateTime getModificationDate() {
        Preconditions.checkState(isHashed(), "not computed");
        return modificationDate;
    }

    public Mark getMark() {
        return mark;
    }

    public void setMark(Mark mark) {
        Preconditions.checkNotNull(mark);
        Preconditions.checkState(isPoll(), "not in poll");
        this.mark = mark;
    }

    public boolean isPoll() {
        return poll;
    }

    void setPoll(boolean poll) {
        this.poll = poll;
    }

    void setDuplicateTargetGroup(DuplicateGroup duplicateTargetGroup) {
        Preconditions.checkNotNull(duplicateTargetGroup);
        this.duplicateTargetGroup = duplicateTargetGroup;
    }

}
