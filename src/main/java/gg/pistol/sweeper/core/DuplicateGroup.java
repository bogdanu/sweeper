/*
 * Sweeper - Duplicate file/folder cleaner
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

import java.util.Collection;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * A collection of targets that have the same content (effectively being duplicates).
 *
 * @author Bogdan Pistol
 */
// package private
class DuplicateGroup implements Comparable<DuplicateGroup> {

    private final Collection<? extends Target> targets;

    private final long size;

    private final String hash;


    DuplicateGroup(Collection<TargetImpl> collection) {
        Preconditions.checkNotNull(collection);
        Preconditions.checkArgument(!collection.isEmpty());

        String hashValue = null;
        long sizeValue = -1;

        for (TargetImpl target : collection) {
            Preconditions.checkArgument(target.isHashed());
            if (hashValue == null) {
                hashValue = target.getHash();
                sizeValue = target.getSize();
            }
            Preconditions.checkArgument(hashValue.equals(target.getHash()));
            Preconditions.checkArgument(sizeValue == target.getSize());
        }

        targets = collection;
        hash = hashValue;
        size = sizeValue;
    }

    public Collection<? extends Target> getTargets() {
        return targets;
    }

    long getSize() {
        return size;
    }

    String getHash() {
        return hash;
    }

    /**
     * Order descending by size.
     */
    public int compareTo(DuplicateGroup other) {
        Preconditions.checkNotNull(other);
        return ComparisonChain.start().compare(size, other.getSize(), Ordering.natural().reverse())
                .compare(hash, other.hash).result();
    }

    @Override
    public int hashCode() {
        return hash.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DuplicateGroup other = (DuplicateGroup) obj;
        return hash.equals(other.hash);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("size", size).add("hash", hash).toString();
    }

}
