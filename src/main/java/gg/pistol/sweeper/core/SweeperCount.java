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
 * Counters for different Sweeper target types.
 * 
 * @author Bogdan Pistol
 */
public interface SweeperCount {

    int getTotalTargets();

    int getTotalTargetFiles();

    int getTotalTargetDirectories();

    long getTotalSize();

    int getDuplicateTargets();

    int getDuplicateTargetFiles();

    int getDuplicateTargetDirectories();

    long getDuplicateSize();

    int getToDeleteTargets();

    int getToDeleteTargetFiles();

    int getToDeleteTargetDirectories();

    long getToDeleteSize();

}
