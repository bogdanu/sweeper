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
import static org.mockito.Mockito.*;

import gg.pistol.sweeper.core.SweeperTarget.Mark;
import gg.pistol.sweeper.core.SweeperTarget.Type;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SweeperTargetImpl.class)
public class SweeperTargetImplTest {
    
    private File file1;;
    private File file1Copy;
    private File file2;
    private File folder;

    private SweeperTargetImpl target1;
    private SweeperTargetImpl target1Copy;
    private SweeperTargetImpl target2;
    private SweeperTargetImpl targetFolder;
    private SweeperTargetImpl targetRoot;
    
    @Before
    public void setUp() throws Exception {
        file1 = mock(File.class);
        file1Copy = mock(File.class);
        file2 = mock(File.class);
        folder = mock(File.class);
        
        when(file1.getPath()).thenReturn("foo");
        when(file1Copy.getPath()).thenReturn("foo");
        when(file2.getPath()).thenReturn("bar");
        when(folder.getPath()).thenReturn("baz");
        
        when(file1.getCanonicalFile()).thenReturn(file1);
        when(file1Copy.getCanonicalFile()).thenReturn(file1Copy);
        when(file2.getCanonicalFile()).thenReturn(file2);
        when(folder.getCanonicalFile()).thenReturn(folder);
        
        when(file1.isFile()).thenReturn(true);
        when(file1Copy.isFile()).thenReturn(true);
        when(file2.isFile()).thenReturn(true);
        when(folder.isFile()).thenReturn(false);
        when(folder.isDirectory()).thenReturn(true);
        
        target1 = new SweeperTargetImpl(file1);
        target1Copy = new SweeperTargetImpl(file1Copy);
        target2 = new SweeperTargetImpl(file2);
        targetFolder = new SweeperTargetImpl(folder);
        targetRoot = new SweeperTargetImpl(Arrays.asList(new File[] {file1, file1Copy, file2, folder}));
    }

    @Test
    public void testEquals() throws IOException {
        assertEquals(target1, target1Copy);
        assertFalse(target1.equals(target2));
        assertFalse(target1.equals(null));
        assertFalse(target1.equals(targetRoot));
        assertFalse(targetRoot.equals(target1));
    }
    
    @Test
    public void testHashCode() throws IOException {
        assertEquals(target1.hashCode(), target1Copy.hashCode());
    }
        
    @Test
    public void testCompareTo() throws IOException {
        assertEquals(0, target1.compareTo(target1Copy));
        assertTrue(target1.compareTo(target2) > 0);
        assertTrue(target2.compareTo(target1) < 0);
        assertTrue(target1.compareTo(targetRoot) > 0);
        assertTrue(targetRoot.compareTo(target1) < 0);
        assertTrue(targetRoot.compareTo(null) > 0);
        assertTrue(targetRoot.compareTo(targetFolder) < 0);
        assertTrue(targetFolder.compareTo(targetRoot) > 0);
        assertTrue(target1.compareTo(targetFolder) > 0);
        assertTrue(targetFolder.compareTo(target1) < 0);
    }
    
    @Test
    public void testConstructor() {
        assertNotNull(targetRoot.getChildren());
        assertEquals(3, targetRoot.getChildren().size());
        assertTrue(targetRoot.isExpanded());
        assertEquals("foo", target1.getName());
        assertEquals(Type.ROOT, targetRoot.getType());
        assertEquals(Type.FILE, target1.getType());
        assertEquals(Type.FOLDER, targetFolder.getType());
        assertNull(targetRoot.getName());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorException1() throws IOException {
        File f = mock(File.class);
        when(f.getCanonicalFile()).thenReturn(f);
        new SweeperTargetImpl(f);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorException2() throws IOException {
        new SweeperTargetImpl(Collections.<File>emptyList());
    }
    
    @Test(expected = IllegalStateException.class)
    public void testGetSizeException() {
        target1.getSize();
    }
    
    @Test
    public void testComputeFile() throws Exception {
        when(file1.length()).thenReturn(10L);
        when(file1.lastModified()).thenReturn(20L);
        target1 = PowerMockito.spy(target1);
        PowerMockito.doReturn(new ByteArrayInputStream("foo".getBytes())).when(target1, "getResourceInputStream");
        assertFalse(target1.isComputed());
        for (int i = 0; i < 2; i++) {
            target1.compute();
            assertTrue(target1.isComputed());
            assertEquals(10L, target1.getSize());
            assertEquals(20L, target1.getModificationDate().getMillis());
            assertEquals("0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33", target1.getHash());
        }
    }
    
    @Test(expected = IllegalStateException.class)
    public void testComputeException() throws Exception {
        targetFolder.compute();
    }
    
    @Test(expected = NoSuchAlgorithmException.class)
    public void testComputeException2() throws Exception {
        target1 = PowerMockito.spy(target1);
        PowerMockito.doReturn(null).when(target1, "getSha1Hash");
        PowerMockito.doReturn(new ByteArrayInputStream(new byte[] {})).when(target1, "getResourceInputStream");
        target1.compute();
    }
    
    @Test(expected = IllegalStateException.class)
    public void testComputeFolderException() throws Exception {
        targetFolder = spy(targetFolder);
        doReturn(Arrays.asList(new SweeperTargetImpl[] {target1})).when(targetFolder).getChildren();
        doReturn(true).when(targetFolder).isExpanded();
        targetFolder.compute();
    }
    
    @Test
    public void testComputeFolder() throws Exception {
        target1 = spy(target1);
        target2 = spy(target2);
        targetFolder = spy(targetFolder);
        doReturn(10L).when(target1).getSize();
        doReturn(new DateTime(20L)).when(target1).getModificationDate();
        doReturn("foo").when(target1).getHash();
        doReturn(true).when(target1).isComputed();
        
        doReturn(11L).when(target2).getSize();
        doReturn(new DateTime(21L)).when(target2).getModificationDate();
        doReturn("bar").when(target2).getHash();
        doReturn(true).when(target2).isComputed();
        
        doReturn(Arrays.asList(new SweeperTargetImpl[] {target1, target2})).when(targetFolder).getChildren();
        doReturn(true).when(targetFolder).isExpanded();
        assertFalse(targetFolder.isComputed());
        for (int i = 0; i < 2; i++) {
            targetFolder.compute();
            assertTrue(targetFolder.isComputed());
            assertEquals(21L, targetFolder.getSize());
            assertEquals(21L, targetFolder.getModificationDate().getMillis());
            assertEquals("60518c1c11dc0452be71a7118a43ab68e3451b82", targetFolder.getHash());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testGetModificationDateException() throws Exception {
        target1.getModificationDate();
    }

    @Test
    public void testGetResource() {
        assertEquals(file1, target1.getResource());
    }

    @Test
    public void testMark() {
        assertEquals(Mark.DECIDE_LATER, target1.getMark());
        target1.setMark(Mark.DELETE);
        assertEquals(Mark.DELETE, target1.getMark());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetHashException() {
        target1.getHash();
    }

    @Test
    public void testExpand() throws IOException {
        when(folder.listFiles()).thenReturn(new File[] {file1, file2});
        assertFalse(targetFolder.isExpanded());
        for (int i = 0; i < 2; i++) {
            targetFolder.expand();
            assertTrue(targetFolder.isExpanded());
            assertNotNull(targetFolder.getChildren());
            assertEquals(2, targetFolder.getChildren().size());
            assertEquals(file2, targetFolder.getChildren().get(0).getResource());
            assertEquals(file1, targetFolder.getChildren().get(1).getResource());
        }
    }
    
    @Test
    public void testToString() {
        assertNotNull(target1.toString());
        assertNotNull(targetFolder.toString());
        assertNotNull(targetRoot.toString());
    }

}
