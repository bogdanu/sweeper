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

/**
 * Counters for total target files/directories and duplicate target files/directories.
 *
 * <p>The values returned by the methods {@link #getDuplicateTargets}, {@link #getDuplicateTargetFiles} and
 * {@link #getDuplicateTargetDirectories} should be considered estimates and not real correct values.
 * <br>This is because the calculation is done considering that a duplicate target is a copy of an original
 * target, so depending on which target is considered the original one the calculation could be different.
 *
 * <p>For example in the following hierarchy of empty files and directories:
 * <pre>
 *      --emptyDir
 *     /
 * root---emptyFile1
 *     \
 *      --dir---emptyFile2
 *           \
 *            --emptyFile3
 * </pre>
 * All the files and directories emptyDir, emptyFile1, dir, emptyFile2 and emptyFile3 are equal (the root is not
 * considered because it is not a real file or directory). Notice that files can be equal with directories, this is on
 * purpose to have more flexibility when cleaning duplicate content.
 *
 * <p>Depending on the target that is considered the original one the following duplicate targets can be considered:
 *
 * <ul><li>If the original target is considered to be "emptyDir" then the other 4 targets are duplicates of
 * the original one.</li>
 * <li>On the other hand if the original target is considered to be "dir" then the other 2 targets (emptyDir and
 * emptyFile1) are duplicates.</li>
 * <li>Or if "emptyFile2" is considered the original one then the other 3 targets are copies (emptyDir, emptyFile1 and
 * emptyFile3 &mdash; without "dir" because this is an upper directory of "emptyFile2").</li></ul>
 *
 * <p>The implementation of the methods {@link #getDuplicateTargets}, {@link #getDuplicateTargetFiles} and
 * {@link #getDuplicateTargetDirectories} chooses a random original target and because of this the values of
 * the methods should be considered approximations (although the returned value is a correct value from the point of
 * view of the chosen original target).
 *
 * @author Bogdan Pistol
 */
public interface SweeperCount {

    /**
     * Getter for total targets (files or directories).
     *
     * @return the number of total files and directories
     */
    int getTotalTargets();

    /**
     * Getter for total files.
     *
     * @return total files
     */
    int getTotalTargetFiles();

    /**
     * Getter for total directories.
     *
     * @return total directories
     */
    int getTotalTargetDirectories();

    /**
     * Getter for total size.
     *
     * @return total size in bytes
     */
    long getTotalSize();

    /**
     * Getter for total duplicate targets (files and directories).
     *
     * @return an estimation of the total duplicate files and directories
     */
    int getDuplicateTargets();

    /**
     * Getter for total duplicate files.
     *
     * @return an estimation of the total duplicate files
     */
    int getDuplicateTargetFiles();

    /**
     * Getter for total duplicate directories.
     *
     * @return an estimation of the total duplicate directories
     */
    int getDuplicateTargetDirectories();

    /**
     * Getter for the total duplicate size.
     *
     * @return the total duplicate size in bytes
     */
    long getDuplicateSize();

}
