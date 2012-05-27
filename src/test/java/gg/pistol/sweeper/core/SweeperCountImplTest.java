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

import static org.junit.Assert.*;
import gg.pistol.sweeper.core.SweeperCountImpl;

import org.junit.Before;
import org.junit.Test;

public class SweeperCountImplTest {
        
    private int totalTargets = 10;
    private int totalTargetFiles = 6;
    private long totalSize = 100;
    
    private int duplicateTargets = 3;
    private int duplicateTargetFiles = 2;
    private long duplicateSize = 30;
    
    private SweeperCountImpl count;
    
    @Before
    public void setUp() {
        count = new SweeperCountImpl(totalTargets, totalTargetFiles, totalSize, duplicateTargets, duplicateTargetFiles, duplicateSize);
    }

    @Test
    public void testGetTotalTargets() {
        assertEquals(totalTargets, count.getTotalTargets());
    }

    @Test
    public void testGetTotalTargetFiles() {
        assertEquals(totalTargetFiles, count.getTotalTargetFiles());
    }

    @Test
    public void testGetTotalTargetDirectories() {
        assertEquals(totalTargets - totalTargetFiles, count.getTotalTargetDirectories());
    }

    @Test
    public void testGetTotalSize() {
        assertEquals(totalSize, count.getTotalSize());
    }

    @Test
    public void testGetDuplicateTargets() {
        assertEquals(duplicateTargets, count.getDuplicateTargets());
    }

    @Test
    public void testGetDuplicateTargetFiles() {
        assertEquals(duplicateTargetFiles, count.getDuplicateTargetFiles());
    }

    @Test
    public void testGetDuplicateTargetDirectories() {
        assertEquals(duplicateTargets - duplicateTargetFiles, count.getDuplicateTargetDirectories());
    }

    @Test
    public void testGetDuplicateSize() {
        assertEquals(duplicateSize, count.getDuplicateSize());
    }

    @Test
    public void testSetToDeleteTargets() {
        count.setToDeleteTargets(5);
        assertEquals(5, count.getToDeleteTargets());
    }

    @Test
    public void testSetToDeleteTargetFiles() {
        count.setToDeleteTargetFiles(5);
        assertEquals(5, count.getToDeleteTargetFiles());
    }

    @Test
    public void testGetToDeleteTargetDirectories() {
        count.setToDeleteTargets(5);
        count.setToDeleteTargetFiles(3);
        assertEquals(2, count.getToDeleteTargetDirectories());
    }

    @Test
    public void testSetToDeleteSize() {
        count.setToDeleteSize(5);
        assertEquals(5, count.getToDeleteSize());
    }

}
