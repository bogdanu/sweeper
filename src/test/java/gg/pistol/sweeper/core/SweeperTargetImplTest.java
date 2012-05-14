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
    private File folder1;
    private File folder2;

    private SweeperTargetImpl target1;
    private SweeperTargetImpl target1Copy;
    private SweeperTargetImpl target2;
    private SweeperTargetImpl targetFolder1;
    private SweeperTargetImpl targetFolder2;
    private SweeperTargetImpl targetRoot;
    private SweeperTargetImpl mockTarget;
    
    private SweeperOperationListener listener;
    
    @Before
    public void setUp() throws Exception {
        file1 = mock(File.class);
        file1Copy = mock(File.class);
        file2 = mock(File.class);
        folder1 = mock(File.class);
        folder2 = mock(File.class);
        listener = mock(SweeperOperationListener.class);
        mockTarget = mock(SweeperTargetImpl.class);
        
        when(file1.getPath()).thenReturn("foo");
        when(file1Copy.getPath()).thenReturn("foo");
        when(file2.getPath()).thenReturn("bar");
        when(folder1.getPath()).thenReturn("baz");
        when(folder2.getPath()).thenReturn("bat");
        
        when(file1.getCanonicalFile()).thenReturn(file1);
        when(file1Copy.getCanonicalFile()).thenReturn(file1Copy);
        when(file2.getCanonicalFile()).thenReturn(file2);
        when(folder1.getCanonicalFile()).thenReturn(folder1);
        when(folder2.getCanonicalFile()).thenReturn(folder2);
        
        when(file1.isFile()).thenReturn(true);
        when(file1Copy.isFile()).thenReturn(true);
        when(file2.isFile()).thenReturn(true);
        when(folder1.isFile()).thenReturn(false);
        when(folder1.isDirectory()).thenReturn(true);
        when(folder2.isFile()).thenReturn(false);
        when(folder2.isDirectory()).thenReturn(true);
        
        target1 = new SweeperTargetImpl(file1, mockTarget);
        target1Copy = new SweeperTargetImpl(file1Copy, mockTarget);
        target2 = new SweeperTargetImpl(file2, mockTarget);
        targetFolder1 = new SweeperTargetImpl(folder1, mockTarget);
        targetFolder2 = new SweeperTargetImpl(folder2, mockTarget);
        targetRoot = new SweeperTargetImpl(Arrays.asList(new File[] {file1, file1Copy, file2, folder1}), listener);
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
        
        assertTrue(targetRoot.compareTo(targetFolder1) < 0);
        assertTrue(targetFolder1.compareTo(targetRoot) > 0);
        
        assertTrue(target1.compareTo(targetFolder1) > 0);
        assertTrue(targetFolder1.compareTo(target1) < 0);
    }
    
    @Test(expected = NullPointerException.class)
    public void testCompareToException() throws IOException {
        target1.compareTo(null);
    }
    
    @Test
    public void testConstructor() {
        assertNotNull(targetRoot.getChildren());
        assertEquals(3, targetRoot.getChildren().size());
        assertEquals(mockTarget, target1.getParent());
        
        assertTrue(targetRoot.isExpanded());
        
        assertEquals("foo", target1.getName());
        assertEquals("", targetRoot.getName());

        assertEquals(Type.ROOT, targetRoot.getType());
        assertEquals(Type.FILE, target1.getType());
        assertEquals(Type.FOLDER, targetFolder1.getType());
        
        verify(listener, times(3)).updateTargetAction(isA(SweeperTargetImpl.class), eq(SweeperTargetAction.OPEN));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorException() throws IOException {
        File f = mock(File.class);
        when(f.getCanonicalFile()).thenReturn(f);
        new SweeperTargetImpl(f, mockTarget);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRootConstructorException() throws IOException {
        new SweeperTargetImpl(Collections.<File>emptyList(), listener);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testGetSizeException() {
        target1.getSize();
    }
    
    @Test(expected = IllegalStateException.class)
    public void testGetTotalFilesException() {
        target1.getTotalFiles();
    }
    
    @Test
    public void testComputeSizeFile() throws Exception {
        when(file1.length()).thenReturn(10L);
        target1 = PowerMockito.spy(target1);
        assertFalse(target1.isSizeComputed());
        
        for (int i = 0; i < 2; i++) {
            target1.computeSize(listener);
            
            assertTrue(target1.isSizeComputed());
            assertEquals(10L, target1.getSize());
            assertEquals(1, target1.getTotalFiles());
        }
        verify(listener).updateTargetAction(target1, SweeperTargetAction.COMPUTE_SIZE);
    }
    
    @Test
    public void testComputeHashFile() throws Exception {
        when(file1.lastModified()).thenReturn(20L);
        target1 = PowerMockito.spy(target1);
        PowerMockito.doReturn(new ByteArrayInputStream("foo".getBytes())).when(target1, "getResourceInputStream");
        when(target1.isSizeComputed()).thenReturn(true);
        assertFalse(target1.isHashComputed());
        
        for (int i = 0; i < 2; i++) {
            target1.computeHash(listener);
            
            assertTrue(target1.isHashComputed());
            assertEquals(20L, target1.getModificationDate().getMillis());
            assertEquals("0--1-0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33", target1.getHash());
        }
        verify(listener).updateTargetAction(target1, SweeperTargetAction.COMPUTE_HASH);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testComputeSizeException() throws Exception {
        targetFolder1.computeSize(listener);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testComputeHashExpandException() throws Exception {
        targetFolder1.computeHash(listener);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testComputeHashSizeException() throws Exception {
        targetFolder1 = spy(targetFolder1);
        when(targetFolder1.isExpanded()).thenReturn(true);
        targetFolder1.computeHash(listener);
    }
    
    @Test
    public void testComputeHashAlgorithmException() throws Exception {
        target1 = PowerMockito.spy(target1);
        PowerMockito.doReturn(null).when(target1, "getSha1Algorithm");
        PowerMockito.doReturn(new ByteArrayInputStream(new byte[] {})).when(target1, "getResourceInputStream");
        when(target1.isSizeComputed()).thenReturn(true);
        
        target1.computeHash(listener);
        verify(listener).updateTargetException(eq(target1), eq(SweeperTargetAction.COMPUTE_HASH),
                argThat(new SweeperExceptionMatcher(NoSuchAlgorithmException.class)));
    }
    
    @Test
    public void testComputeSizeFolderException() throws Exception {
        targetFolder1 = spy(targetFolder1);
        doReturn(Arrays.asList(new SweeperTargetImpl[] {target1})).when(targetFolder1).getChildren();
        doReturn(true).when(targetFolder1).isExpanded();
        
        targetFolder1.computeSize(listener);
        verify(listener).updateTargetException(eq(targetFolder1), eq(SweeperTargetAction.COMPUTE_SIZE),
                argThat(new SweeperExceptionMatcher(IllegalStateException.class)));
        assertEquals(SweeperTargetImpl.DEFAULT_SIZE, targetFolder1.getSize());
    }
    
    @Test
    public void testComputeHashFolderException() throws Exception {
        targetFolder1 = spy(targetFolder1);
        doReturn(Arrays.asList(new SweeperTargetImpl[] {target1})).when(targetFolder1).getChildren();
        doReturn(true).when(targetFolder1).isExpanded();
        doReturn(true).when(targetFolder1).isSizeComputed();
        
        targetFolder1.computeHash(listener);
        verify(listener).updateTargetException(eq(targetFolder1), eq(SweeperTargetAction.COMPUTE_HASH),
                argThat(new SweeperExceptionMatcher(IllegalStateException.class)));
        assertEquals(SweeperTargetImpl.DEFAULT_HASH, targetFolder1.getHash());
    }
    
    @Test
    public void testComputeSizeFolder() throws Exception {
        target1 = spy(target1);
        target2 = spy(target2);
        targetFolder1 = spy(targetFolder1);
        doReturn(10L).when(target1).getSize();
        doReturn(3).when(target1).getTotalFiles();
        doReturn(true).when(target1).isSizeComputed();
        
        doReturn(11L).when(target2).getSize();
        doReturn(4).when(target2).getTotalFiles();
        doReturn(true).when(target2).isSizeComputed();
        
        doReturn(Arrays.asList(new SweeperTargetImpl[] {target1, target2})).when(targetFolder1).getChildren();
        doReturn(true).when(targetFolder1).isExpanded();
        assertFalse(targetFolder1.isSizeComputed());
        
        for (int i = 0; i < 2; i++) {
            targetFolder1.computeSize(listener);
            
            assertTrue(targetFolder1.isSizeComputed());
            assertEquals(21L, targetFolder1.getSize());
            assertEquals(7, targetFolder1.getTotalFiles());
        }
    }
    
    @Test
    public void testComputeHashFolder() throws Exception {
        target1 = spy(target1);
        target2 = spy(target2);
        targetFolder1 = spy(targetFolder1);
        targetFolder2 = spy(targetFolder2);
        doReturn(new DateTime(20L)).when(target1).getModificationDate();
        doReturn("foo").when(target1).getHash();
        doReturn(true).when(target1).isHashComputed();
        
        doReturn(new DateTime(21L)).when(target2).getModificationDate();
        doReturn("bar").when(target2).getHash();
        doReturn(true).when(target2).isHashComputed();
        
        doReturn(Arrays.asList(new SweeperTargetImpl[] {target1, target2})).when(targetFolder1).getChildren();
        doReturn(Arrays.asList(new SweeperTargetImpl[] {target1})).when(targetFolder2).getChildren();
        doReturn(true).when(targetFolder1).isExpanded();
        doReturn(true).when(targetFolder2).isExpanded();
        doReturn(true).when(targetFolder1).isSizeComputed();
        doReturn(true).when(targetFolder2).isSizeComputed();
        assertFalse(targetFolder1.isHashComputed());
        assertFalse(targetFolder2.isHashComputed());
        
        for (int i = 0; i < 2; i++) {
            targetFolder1.computeHash(listener);
            assertTrue(targetFolder1.isHashComputed());
            assertEquals(21L, targetFolder1.getModificationDate().getMillis());
            assertEquals("0--1-60518c1c11dc0452be71a7118a43ab68e3451b82", targetFolder1.getHash());
            
            targetFolder2.computeHash(listener);
            assertTrue(targetFolder2.isHashComputed());
            assertEquals("foo", targetFolder2.getHash());
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
        when(folder1.listFiles()).thenReturn(new File[] {file1, file2});
        assertFalse(targetFolder1.isExpanded());
        
        for (int i = 0; i < 2; i++) {
            targetFolder1.expand(listener);
            
            assertTrue(targetFolder1.isExpanded());
            assertNotNull(targetFolder1.getChildren());
            assertEquals(2, targetFolder1.getChildren().size());
            assertEquals(file2, targetFolder1.getChildren().get(0).getResource());
            assertEquals(file1, targetFolder1.getChildren().get(1).getResource());
        }
        verify(listener).updateTargetAction(targetFolder1, SweeperTargetAction.EXPAND);
    }
    
    @Test
    public void testExpandSecurityException() {
        when(folder1.listFiles()).thenThrow(new SecurityException());
        targetFolder1.expand(listener);
        verify(listener).updateTargetException(eq(targetFolder1), eq(SweeperTargetAction.EXPAND),
                argThat(new SweeperExceptionMatcher(SecurityException.class)));
    }
    
    @Test
    public void testExpandNullException() {
        when(folder1.listFiles()).thenReturn(null);
        targetFolder1.expand(listener);
        verify(listener).updateTargetException(eq(targetFolder1), eq(SweeperTargetAction.EXPAND),
                isA(SweeperException.class));
    }
    
    @Test
    public void testReadException() throws Exception {
        when(folder1.listFiles()).thenReturn(new File[] { file1 });
        when(file1.getCanonicalFile()).thenThrow(new IOException());
        
        targetFolder1.expand(listener);
        
        verify(listener).updateTargetAction(targetFolder1, SweeperTargetAction.EXPAND);
        verify(listener).updateTargetException(isA(SweeperTarget.class), eq(SweeperTargetAction.OPEN),
                argThat(new SweeperExceptionMatcher(IOException.class)));
        assertTrue(targetFolder1.isExpanded());
        assertNotNull(targetFolder1.getChildren());
        assertTrue(targetFolder1.getChildren().isEmpty());
    }
    
    @Test
    public void testToString() {
        assertNotNull(target1.toString());
        assertNotNull(targetFolder1.toString());
        assertNotNull(targetRoot.toString());
    }

}
