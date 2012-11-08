/*
 * Sweeper - Duplicate file cleaner
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

// package private
class SweeperCountImpl implements SweeperCount {

    private final int totalTargets;
    private final int totalTargetFiles;
    private final long totalSize;

    private final int duplicateTargets;
    private final int duplicateTargetFiles;
    private final long duplicateSize;

    private int toDeleteTargets;
    private int toDeleteTargetFiles;
    private long toDeleteSize;

    SweeperCountImpl(int totalTargets, int totalTargetFiles, long totalSize, int duplicateTargets,
                     int duplicateTargetFiles, long duplicateSize) {
        this.totalTargets = totalTargets;
        this.totalTargetFiles = totalTargetFiles;
        this.totalSize = totalSize;

        this.duplicateTargets = duplicateTargets;
        this.duplicateTargetFiles = duplicateTargetFiles;
        this.duplicateSize = duplicateSize;
    }

    public int getTotalTargets() {
        return totalTargets;
    }

    public int getTotalTargetFiles() {
        return totalTargetFiles;
    }

    public int getTotalTargetDirectories() {
        return totalTargets - totalTargetFiles;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public int getDuplicateTargets() {
        return duplicateTargets;
    }

    public int getDuplicateTargetFiles() {
        return duplicateTargetFiles;
    }

    public int getDuplicateTargetDirectories() {
        return duplicateTargets - duplicateTargetFiles;
    }

    public long getDuplicateSize() {
        return duplicateSize;
    }

    public int getToDeleteTargets() {
        return toDeleteTargets;
    }

    void setToDeleteTargets(int value) {
        toDeleteTargets = value;
    }

    public int getToDeleteTargetFiles() {
        return toDeleteTargetFiles;
    }

    void setToDeleteTargetFiles(int value) {
        toDeleteTargetFiles = value;
    }

    public int getToDeleteTargetDirectories() {
        return toDeleteTargets - toDeleteTargetFiles;
    }

    public long getToDeleteSize() {
        return toDeleteSize;
    }

    void setToDeleteSize(long value) {
        toDeleteSize = value;
    }

}
