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


import java.util.ArrayList;
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
class DuplicateTargetGroup implements Comparable<DuplicateTargetGroup>, SweeperPoll {
    
    private final Collection<SweeperTarget> targets;
    
    private final long size;
    
    private final String hash;
    
    private boolean polled;
    
    private boolean targetMarked;
    
    DuplicateTargetGroup(Collection<SweeperTargetImpl> collection) {
        Preconditions.checkNotNull(collection);
        Preconditions.checkArgument(!collection.isEmpty());
        
        String hashValue = null;
        long sizeValue = -1;
        
        for (SweeperTargetImpl target : collection) {
            Preconditions.checkArgument(target.isHashed());
            if (hashValue == null) {
                hashValue = target.getHash();
                sizeValue = target.getSize();
            }
            Preconditions.checkArgument(hashValue.equals(target.getHash()));
            Preconditions.checkArgument(sizeValue == target.getSize());
        }
        
        targets = new ArrayList<SweeperTarget>(collection);
        hash = hashValue;
        size = sizeValue;

        for (SweeperTargetImpl target : collection) {
            target.setDuplicateTargetGroup(this);
        }
    }
    
    public Collection<SweeperTarget> getTargets() {
        return targets;
    }

    long getSize() {
        return size;
    }
    
    String getHash() {
        return hash;
    }
    
    boolean isPolled() {
        return polled;
    }

    void setPolled(boolean polled) {
        this.polled = polled;
    }
    
    void setTargetMarked(boolean value) {
        targetMarked = value;
    }
    
    boolean isTargetMarked() {
        return targetMarked;
    }

    public int compareTo(DuplicateTargetGroup other) {
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
        DuplicateTargetGroup other = (DuplicateTargetGroup) obj;
        return hash.equals(other.hash);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("size", size).add("hash", hash).toString();
    }
    
}