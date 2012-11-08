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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static gg.pistol.sweeper.test.ObjectVerifier.*;

import gg.pistol.sweeper.core.Target.Type;
import gg.pistol.sweeper.core.resource.Resource;
import gg.pistol.sweeper.core.resource.ResourceDirectory;
import gg.pistol.sweeper.core.resource.ResourceFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class TargetImplTest {

    private ResourceFile resource1;
    private ResourceFile resource1Copy;
    private ResourceFile resource2;
    private ResourceDirectory resourceDir;

    private TargetImpl mockedParent;

    private TargetImpl target1;
    private TargetImpl target1Copy;
    private TargetImpl target2;
    private TargetImpl targetDir;

    private OperationTrackingListener listener;
    private HashFunction hashFunction;

    @Before
    public void setUp() throws Exception {
        resource1 = mockResourceFile("bar");
        resource1Copy = mockResourceFile("bar");
        resource2 = mockResourceFile("foo");
        resourceDir = mockResourceDirectory("baz", resource1, resource2);

        mockedParent = mock(TargetImpl.class);

        target1 = new TargetImpl(resource1, mockedParent);
        target1Copy = new TargetImpl(resource1Copy, mockedParent);
        target2 = new TargetImpl(resource2, mockedParent);
        targetDir = new TargetImpl(resourceDir, mockedParent);

        listener = mock(OperationTrackingListener.class);
        hashFunction = new HashFunction();
    }

    private ResourceFile mockResourceFile(String name) {
        ResourceFile res = mock(ResourceFile.class);
        when(res.getName()).thenReturn(name);
        return res;
    }

    private ResourceDirectory mockResourceDirectory(String name, Resource... subresources) {
        ResourceDirectory res = mock(ResourceDirectory.class);
        when(res.getName()).thenReturn(name);
        ResourceDirectory.ResourceCollectionResponse subresourceCollection = mock(ResourceDirectory.ResourceCollectionResponse.class);
        when(res.getSubresources()).thenReturn(subresourceCollection);
        doReturn(ImmutableList.copyOf(subresources)).when(subresourceCollection).getResources();
        return res;
    }

    @Test
    public void testRootConstructor() throws Exception {
        TargetImpl root = new TargetImpl(ImmutableSet.of((Resource) resource1, resource2));

        assertEquals("", root.getName());
        assertEquals(Type.ROOT, root.getType());
        assertNull(root.getResource());
        assertNull(root.getParent());
        assertTrue(root.isPartiallyExpanded());
        assertTrue(root.isExpanded());

        Iterator<TargetImpl> children = root.getChildren().iterator();
        assertEquals(resource1, children.next().getResource());
        assertEquals(resource2, children.next().getResource());

        try {
            new TargetImpl(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            new TargetImpl(Collections.<Resource>emptySet());
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testConstructor() throws Exception {
        assertEquals("bar", target1.getName());
        assertEquals(resource1, target1.getResource());
        assertEquals(mockedParent, target1.getParent());

        assertEquals(Type.FILE, target1.getType());
        assertTrue(target1.isPartiallyExpanded());
        assertTrue(target1.isExpanded());
        assertTrue(target1.getChildren().isEmpty());

        assertEquals(Type.DIRECTORY, targetDir.getType());
        assertTrue(targetDir.getChildren().isEmpty());

        try {
            new TargetImpl(null, mockedParent);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            new TargetImpl(resource1, null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            new TargetImpl(mock(Resource.class), mockedParent);
            fail();
        } catch (IllegalArgumentException e) {
            // expected, resource is not a ResourceFile or a ResourceDirectory
        }
    }

    @Test
    public void testExpand() throws Exception {
        targetDir.expand(listener);

        assertTrue(targetDir.isPartiallyExpanded());
        assertTrue(targetDir.isExpanded());

        Iterator<TargetImpl> children = targetDir.getChildren().iterator();
        assertEquals(resource1, children.next().getResource());
        assertEquals(resource2, children.next().getResource());

        verify(listener).updateTargetAction(targetDir, TargetAction.EXPAND);
    }

    @Test
    public void testExpandException() throws Exception {
        try {
            targetDir.expand(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        ResourceDirectory.ResourceCollectionResponse response = mock(ResourceDirectory.ResourceCollectionResponse.class);
        doReturn(ImmutableList.of(new Exception())).when(response).getExceptions();
        when(resourceDir.getSubresources()).thenReturn(response);
        targetDir.expand(listener);

        assertTrue(targetDir.isPartiallyExpanded());
        assertFalse(targetDir.isExpanded());
        verify(listener).updateTargetAction(targetDir, TargetAction.EXPAND);
        verify(listener).updateTargetException(eq(targetDir), eq(TargetAction.EXPAND), any(SweeperException.class));
    }

    @Test
    public void testComputeFileSize() throws Exception {
        long size = 5L;
        when(resource1.getSize()).thenReturn(size);

        assertFalse(target1.isPartiallySized());
        assertFalse(target1.isSized());

        for (int i = 1; i <= 2; i++) {
            target1.computeSize(listener);

            assertTrue(target1.isPartiallyExpanded());
            assertTrue(target1.isSized());

            assertEquals(size, target1.getSize());
            assertEquals(1, target1.getTotalTargets());
            assertEquals(1, target1.getTotalTargetFiles());
        }
        verify(listener).updateTargetAction(target1, TargetAction.COMPUTE_SIZE);
    }

    @Test
    public void testComputeDirectorySize() throws Exception {
        computeDirectorySize(false);
        computeDirectorySize(true);
    }

    private void computeDirectorySize(boolean isFullExpanded) {
        long target1Size = 5L;
        int target1Subtargets = 1;
        int target1Files = 1;
        TargetImpl target1Spy = prepareChildToSize(target1, target1Size, target1Subtargets, target1Files);

        long target2Size = 6L;
        int target2Subtargets = 3;
        int target2Files = 2;
        TargetImpl target2Spy = prepareChildToSize(target2, target2Size, target2Subtargets, target2Files);
        TargetImpl targetDirSpy = prepareDirToSize(targetDir, isFullExpanded, target1Spy, target2Spy);

        assertFalse(targetDirSpy.isPartiallySized());
        assertFalse(targetDirSpy.isSized());

        for (int i = 1; i <= 2; i++) {
            targetDirSpy.computeSize(listener);

            assertTrue(targetDirSpy.isPartiallySized());
            assertEquals(isFullExpanded, targetDirSpy.isSized());

            assertEquals(target1Size + target2Size, targetDirSpy.getSize());
            assertEquals(1 + target1Subtargets + target2Subtargets, targetDirSpy.getTotalTargets());
            assertEquals(target1Files + target2Files, targetDirSpy.getTotalTargetFiles());
        }
        verify(listener).updateTargetAction(targetDirSpy, TargetAction.COMPUTE_SIZE);
    }

    private TargetImpl prepareChildToSize(TargetImpl target, long size, int totalTargets, int totalFiles) {
        target = spy(target);
        when(target.isPartiallySized()).thenReturn(true);
        when(target.isSized()).thenReturn(true);
        when(target.getSize()).thenReturn(size);
        when(target.getTotalTargets()).thenReturn(totalTargets);
        when(target.getTotalTargetFiles()).thenReturn(totalFiles);
        return target;
    }

    private TargetImpl prepareDirToSize(TargetImpl target, boolean isFullExpanded, TargetImpl... children) {
        target = spy(target);
        when(target.isPartiallyExpanded()).thenReturn(true);
        when(target.isExpanded()).thenReturn(isFullExpanded);
        when(target.getChildren()).thenReturn(ImmutableList.copyOf(children));
        return target;
    }

    @Test
    public void testComputeSizeException() throws Exception {
        try {
            target1.computeSize(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            targetDir.computeSize(listener);
            fail();
        } catch (IllegalStateException e) {
            // expected because not expanded
        }

        TargetImpl targetDirSpy = prepareDirToSize(targetDir, true, target1);
        try {
            targetDirSpy.computeSize(listener);
            fail();
        } catch (IllegalStateException e) {
            // expected because target1 is not partially sized
            assertFalse(targetDir.isPartiallySized());
            assertFalse(targetDir.isSized());
        }

        when(resource1.getSize()).thenThrow(new RuntimeException());
        target1.computeSize(listener);
        assertTrue(target1.isPartiallySized());
        assertFalse(target1.isSized());

        // exception because target1 is not sized
        verify(listener).updateTargetException(eq(target1), eq(TargetAction.COMPUTE_SIZE), any(SweeperException.class));
    }

    @Test
    public void testComputeFileHash() throws Exception {
        long modificationDate = 100L;
        long size = 20L;
        when(resource1.getModificationDate()).thenReturn(new DateTime(modificationDate));
        when(resource1.getInputStream()).thenReturn(new ByteArrayInputStream("foo".getBytes("UTF-8")));
        target1 = spy(target1);
        when(target1.isPartiallySized()).thenReturn(true);
        when(target1.isSized()).thenReturn(true);
        when(target1.getSize()).thenReturn(size);

        assertFalse(target1.isPartiallyHashed());
        assertFalse(target1.isHashed());

        for (int i = 1; i <= 2; i++) {
            target1.computeHash(hashFunction, listener, new AtomicBoolean());

            assertTrue(target1.isPartiallyHashed());
            assertTrue(target1.isHashed());

            assertEquals(modificationDate, target1.getModificationDate().getMillis());
            assertEquals(size + "0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33", target1.getHash());
        }

        verify(listener).updateTargetAction(target1, TargetAction.COMPUTE_HASH);
    }

    @Test
    public void testComputeDirectoryHash() throws Exception {
        long modificationDate1 = 100L;
        long size1 = 150L;
        target1 = prepareChildToHash(target1, modificationDate1, size1, "foo");

        long modificationDate2 = 200L;
        long size2 = 250L;
        target2 = prepareChildToHash(target2, modificationDate2, size2, "bar");

        verifyComputeHashDirectory(Math.max(modificationDate1, modificationDate2),
                (size1 + size2) + "889ed5b4ac3cb37eb2a39531fb640e7d4d74c2c0", size1 + size2, targetDir, target1, target2);

        when(target2.getSize()).thenReturn(0L);
        verifyComputeHashDirectory(modificationDate1, "foo", size1, new TargetImpl(resourceDir, mockedParent),
                target1, target2);
    }

    private TargetImpl prepareChildToHash(TargetImpl target, long lastModifiedMillis, long size, String hash) {
        target = spy(target);
        when(target.isPartiallyHashed()).thenReturn(true);
        when(target.isHashed()).thenReturn(true);
        when(target.getModificationDate()).thenReturn(new DateTime(lastModifiedMillis));
        when(target.getHash()).thenReturn(hash);
        doReturn(size).when(target).getSize();
        return target;
    }

    private TargetImpl prepareDirToHash(TargetImpl target, long size, TargetImpl... children) {
        target = spy(target);
        when(target.isPartiallySized()).thenReturn(true);
        when(target.isSized()).thenReturn(true);
        when(target.getSize()).thenReturn(size);
        when(target.getChildren()).thenReturn(ImmutableList.copyOf(children));
        return target;
    }

    private void verifyComputeHashDirectory(long expectedLastModified, String expectedHash, long dirSize,
            TargetImpl target, TargetImpl... children) throws Exception {
        target = prepareDirToHash(target, dirSize, children);

        assertFalse(target.isPartiallyHashed());
        assertFalse(target.isHashed());

        target.computeHash(hashFunction, listener, new AtomicBoolean());

        assertTrue(target.isPartiallyHashed());
        assertTrue(target.isHashed());
        assertEquals(expectedLastModified, target.getModificationDate().getMillis());
        assertEquals(expectedHash, target.getHash());

        verify(listener).updateTargetAction(target, TargetAction.COMPUTE_HASH);
    }

    @Test
    public void testComputeHashException() throws Exception {
        try {
            target1.computeHash(null, listener, new AtomicBoolean());
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            target1.computeHash(hashFunction, null, new AtomicBoolean());
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            target1.computeHash(hashFunction, listener, null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            target1.computeHash(hashFunction, listener, new AtomicBoolean());
            fail();
        } catch (IllegalStateException e) {
            // expected, not sized
        }

        targetDir = prepareDirToHash(targetDir, 0L, target1);
        try {
            targetDir.computeHash(hashFunction, listener, new AtomicBoolean());
            fail();
        } catch (IllegalStateException e) {
            // expected, target1 is not partially hashed
            assertFalse(targetDir.isPartiallyHashed());
            assertFalse(targetDir.isHashed());
        }

        target1 = prepareChildToHash(target1, 0L, 0L, "");
        when(target1.isHashed()).thenReturn(false);
        targetDir = prepareDirToHash(new TargetImpl(resourceDir, mockedParent), 0L, target1);
        targetDir.computeHash(hashFunction, listener, new AtomicBoolean());
        assertTrue(target1.isPartiallyHashed());
        assertFalse(target1.isHashed());

        // exception because target1 is not hashed
        verify(listener).updateTargetException(eq(targetDir), eq(TargetAction.COMPUTE_HASH), any(SweeperException.class));
    }

    @Test
    public void testDelete() throws Exception {
        target1.delete(listener);
        verify(listener).updateTargetAction(target1, TargetAction.DELETE);
        verify(resource1).delete();

        try {
            target1.delete(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            new TargetImpl(ImmutableSet.of(resource1)).delete(listener);
            fail();
        } catch (IllegalStateException e) {
            // expected because of trying to delete a ROOT target
        }

        when(resourceDir.deleteOnlyEmpty()).thenReturn(true);
        try {
            targetDir.delete(listener);
            fail();
        } catch (IllegalStateException e) {
            // expected because "targetDir" is not expanded
        }

        when(targetDir.isExpanded()).thenReturn(true);
        try {
            targetDir.delete(listener);
            fail();
        } catch (IllegalStateException e) {
            // expected because only empty directories can be deleted
        }

        doThrow(new IOException()).when(resource1).delete();
        target1.delete(listener);
        verify(listener).updateTargetException(eq(target1), eq(TargetAction.DELETE), any(SweeperException.class));
    }

    @Test
    public void testHashCode() {
        verifyHashCode(target1, target1Copy);
    }

    @Test
    public void testEquals() {
        verifyEquals(target1, target1Copy, target2);
    }

    @Test
    public void testToString() {
        verifyToString(target1);
    }

    @Test
    public void testCompareTo() throws IOException {
        verifyCompareTo(target1, target1Copy, target2);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSizeException() {
        target1.getSize();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetTotalTargetsException() {
        target1.getTotalTargets();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetTotalTargetFilesException() {
        target1.getTotalTargetFiles();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetHashException() {
        target1.getHash();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetModificationDateException() throws Exception {
        target1.getModificationDate();
    }

}
