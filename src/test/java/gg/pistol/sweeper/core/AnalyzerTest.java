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

import gg.pistol.sweeper.core.resource.Resource;
import gg.pistol.sweeper.core.resource.ResourceDirectory;
import gg.pistol.sweeper.core.resource.ResourceFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Analyzer.class)
public class AnalyzerTest {

    private Analyzer analyzer;

    private SweeperOperationListener listener;

    @Before
    public void setUp() throws Exception {
        analyzer = new Analyzer();
        listener = mock(OperationTrackingListener.class);
    }

    private ResourceFile mockFile(String name, long size, long lastModifiedMillis, String content) throws Exception {
        ResourceFile res = mock(ResourceFile.class);
        when(res.getName()).thenReturn(name);
        when(res.getSize()).thenReturn(size);
        when(res.getModificationDate()).thenReturn(new DateTime(lastModifiedMillis));
        when(res.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes("UTF-8")));
        return res;
    }

    private ResourceDirectory mockDirectory(String name, Resource... children) {
        ResourceDirectory res = mock(ResourceDirectory.class);
        when(res.getName()).thenReturn(name);
        ResourceDirectory.ResourceCollectionResponse subresResponse = mock(ResourceDirectory.ResourceCollectionResponse.class);
        when(res.getSubresources()).thenReturn(subresResponse);
        if (children == null) {
            children = new Resource[] {};
        }
        when(subresResponse.getResources()).thenReturn(ImmutableList.copyOf(children));
        when(subresResponse.getExceptions()).thenReturn(Collections.<Exception>emptyList());
        return res;
    }

    private boolean areTargetsFromResources(Collection<Target> targets, Resource... resources) {
        if (targets.size() != resources.length) {
            return false;
        }
        for (Resource res : resources) {
            boolean contained = false;
            for (Target target : targets) {
                if (target.getResource() == res) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                return false;
            }
        }
        return true;
    }

    private TargetImpl getTargetFromResource(Collection<Target> targets, Resource resource) {
        for (Target target : targets) {
            if (target.getResource() == resource) {
                return (TargetImpl) target;
            }
        }
        return null;
    }

    /*
     * Test detecting duplicates in the following setup:
     *
     *                  --someFile
     *                 /
     * root----upperDir---dir---file1
     *     \                 \
     *      \                 --file2
     *       --dirCopy---file1Copy
     *                \
     *                 --file2Copy
     *
     * The following are duplicates: dir = dirCopy, file1 = file1Copy, file2 = file2Copy.
     */
    @Test
    public void testWithDuplicateDir() throws Exception {
        ResourceFile file1 = mockFile("file1", 1L, 10L, "file1");
        ResourceFile file2 = mockFile("file2", 2L, 11L, "file2");
        ResourceDirectory dir = mockDirectory("dir", file1, file2);

        ResourceFile file1Copy = mockFile("file1Copy", 1L, 20L, "file1");
        ResourceFile file2Copy = mockFile("file2Copy", 2L, 21L, "file2");
        ResourceDirectory dirCopy = mockDirectory("dirCopy", file1Copy, file2Copy);

        ResourceFile someFile = mockFile("someFile", 5L, 30L, "someFile");
        ResourceDirectory upperDir = mockDirectory("upperDir", dir, someFile);

        Set<Resource> set = ImmutableSet.of((Resource) upperDir, dirCopy);
        List<DuplicateGroup> dups = analyzer.compute(set, listener);

        assertEquals(3, dups.size());
        assertEquals(3L + "24c6f5ec6943e59c19b08dd8f11c73ec8dd3b764", dups.get(0).getHash());
        assertEquals(2L + "cb99b709a1978bd205ab9dfd4c5aaa1fc91c7523", dups.get(1).getHash());
        assertEquals(1L + "60b27f004e454aca81b0480209cce5081ec52390", dups.get(2).getHash());

        assertTrue(areTargetsFromResources(dups.get(0).getTargets(), dir, dirCopy));
        assertTrue(areTargetsFromResources(dups.get(1).getTargets(), file2, file2Copy));
        assertTrue(areTargetsFromResources(dups.get(2).getTargets(), file1, file1Copy));

        TargetImpl dirCopyTarget = getTargetFromResource(dups.get(0).getTargets(), dirCopy);
        TargetImpl root = dirCopyTarget.getParent();
        assertTrue(dirCopyTarget.isHashed());
        assertTrue(root.isSized());
        assertFalse(root.isHashed());

        verifyListener();

        SweeperCountImpl count = analyzer.getCount();
        assertEquals(8, count.getTotalTargets());
        assertEquals(5, count.getTotalTargetFiles());
        assertEquals(3, count.getTotalTargetDirectories());
        assertEquals(11L, count.getTotalSize());

        assertEquals(3, count.getDuplicateTargets());
        assertEquals(2, count.getDuplicateTargetFiles());
        assertEquals(1, count.getDuplicateTargetDirectories());
        assertEquals(3L, count.getDuplicateSize());
    }

    private void verifyListener() {
        verify(listener).updateOperation(SweeperOperation.RESOURCE_TRAVERSING);
        verify(listener, atLeastOnce()).updateTargetAction(any(TargetImpl.class), eq(TargetAction.EXPAND));

        verify(listener).updateOperation(SweeperOperation.SIZE_COMPUTATION);
        verify(listener, atLeastOnce()).updateTargetAction(any(TargetImpl.class), eq(TargetAction.COMPUTE_SIZE));

        verify(listener).updateOperation(SweeperOperation.SIZE_DEDUPLICATION);

        verify(listener).updateOperation(SweeperOperation.HASH_COMPUTATION);
        verify(listener, atLeastOnce()).updateTargetAction(any(TargetImpl.class), eq(TargetAction.COMPUTE_HASH));

        verify(listener).updateOperation(SweeperOperation.HASH_DEDUPLICATION);
        verify(listener).updateOperation(SweeperOperation.COUNTING);
        verify(listener).updateOperation(SweeperOperation.DUPLICATE_GROUPING);
        verify(listener).updateOperationProgress(anyLong(), anyLong(), eq(100));
    }

    /*
     * Test detecting empty file/directory duplicates in the following setup:
     *
     *      --emtyDir
     *     /
     * root---emtyFile1
     *     \
     *      --dir---emtyFile2
     *           \
     *            --emtyFile3
     *
     * The following are duplicates: emtyFile1 = emtyFile2 = emtyFile3 = dir.
     */
    @Test
    public void testWithEmpty() throws Exception {
        ResourceDirectory emptyDir = mockDirectory("emptyDir");
        ResourceFile emptyFile1 = mockFile("emptyFile1", 0L, 10L, "");
        ResourceFile emptyFile2 = mockFile("emptyFile2", 0L, 11L, "");
        ResourceFile emptyFile3 = mockFile("emptyFile3", 0L, 12L, "");
        ResourceDirectory dir = mockDirectory("dir", emptyFile2, emptyFile3);

        Set<Resource> set = ImmutableSet.of(emptyDir, emptyFile1, dir);
        List<DuplicateGroup> dups = analyzer.compute(set, listener);

        assertEquals(1, dups.size());
        assertEquals(0L + "da39a3ee5e6b4b0d3255bfef95601890afd80709", dups.get(0).getHash());

        assertTrue(areTargetsFromResources(dups.get(0).getTargets(), emptyDir, emptyFile1, emptyFile2, emptyFile3, dir));

        verifyListener();

        SweeperCountImpl count = analyzer.getCount();
        assertEquals(5, count.getTotalTargets());
        assertEquals(3, count.getTotalTargetFiles());
        assertEquals(2, count.getTotalTargetDirectories());
        assertEquals(0L, count.getTotalSize());

        // The duplicate count depends on the targets that need to be removed.
        assertTrue(ImmutableSet.of(2, 3, 4).contains(count.getDuplicateTargets()));
        assertTrue(ImmutableSet.of(1, 2, 3).contains(count.getDuplicateTargetFiles()));
        assertTrue(ImmutableSet.of(1, 2).contains(count.getDuplicateTargetDirectories()));
        assertEquals(0L, count.getDuplicateSize());
    }

    /*
     * Test detecting duplicates having a folder containing only one file:
     *
     * root---dir---file
     *     \
     *      --fileCopy
     *
     * The following are duplicates: dir = fileCopy ("dir" and "file" are not considered duplicates, the directory
     * is considered synonymous with the file it only contains).
     */
    @Test
    public void testWithSingleFile() throws Exception {
        ResourceFile file = mockFile("file", 1L, 10L, "file");
        ResourceFile fileCopy = mockFile("fileCopy", 1L, 11L, "file");
        ResourceDirectory dir = mockDirectory("dir", file);

        Set<Resource> set = ImmutableSet.of(fileCopy, dir);
        List<DuplicateGroup> dups = analyzer.compute(set, listener);

        assertEquals(1, dups.size());
        assertEquals(1L + "971c419dd609331343dee105fffd0f4608dc0bf2", dups.get(0).getHash());

        assertTrue(areTargetsFromResources(dups.get(0).getTargets(), dir, fileCopy));

        verifyListener();

        SweeperCountImpl count = analyzer.getCount();
        assertEquals(3, count.getTotalTargets());
        assertEquals(2, count.getTotalTargetFiles());
        assertEquals(1, count.getTotalTargetDirectories());
        assertEquals(2L, count.getTotalSize());

        // The duplicate count depends on the targets that need to be removed, if "dir" and "file" are considered to be
        // duplicates then there are 2 duplicate targets, otherwise if "fileCopy" is considered to be duplicate then
        // there is one duplicate target.
        assertTrue(ImmutableSet.of(1, 2).contains(count.getDuplicateTargets()));
        assertEquals(1, count.getDuplicateTargetFiles());
        assertTrue(ImmutableSet.of(0, 1).contains(count.getDuplicateTargetDirectories()));
        assertEquals(1L, count.getDuplicateSize());
    }

    /*
     * Test the compute() method on more than INITIAL_EXPAND_LIMIT targets.
     */
    @Test
    public void testWithExpandLimit() throws Exception {
        int targets = 103;
        Set<Resource> set = new HashSet<Resource>();
        for (int i = 0; i < targets; i++) {
            set.add(mockFile("file" + i, i, i, "file" + i));
        }

        List<DuplicateGroup> dups = analyzer.compute(set, listener);

        assertTrue(dups.isEmpty());

        verify(listener).updateOperation(SweeperOperation.RESOURCE_TRAVERSING);
        verify(listener, never()).updateTargetAction(any(TargetImpl.class), eq(TargetAction.EXPAND));

        verify(listener).updateOperation(SweeperOperation.SIZE_COMPUTATION);
        verify(listener, atLeastOnce()).updateTargetAction(any(TargetImpl.class), eq(TargetAction.COMPUTE_SIZE));

        verify(listener).updateOperation(SweeperOperation.SIZE_DEDUPLICATION);

        verify(listener).updateOperation(SweeperOperation.HASH_COMPUTATION);
        verify(listener, never()).updateTargetAction(any(TargetImpl.class), eq(TargetAction.COMPUTE_HASH));

        verify(listener).updateOperation(SweeperOperation.HASH_DEDUPLICATION);
        verify(listener).updateOperation(SweeperOperation.COUNTING);
        verify(listener).updateOperation(SweeperOperation.DUPLICATE_GROUPING);
        verify(listener).updateOperationProgress(anyLong(), anyLong(), eq(100));

        SweeperCountImpl count = analyzer.getCount();
        assertEquals(targets, count.getTotalTargets());
        assertEquals(targets, count.getTotalTargetFiles());
        assertEquals(0, count.getTotalTargetDirectories());
        assertEquals((targets - 1) * targets / 2, count.getTotalSize());

        assertEquals(0, count.getDuplicateTargets());
        assertEquals(0, count.getDuplicateTargetFiles());
        assertEquals(0, count.getDuplicateTargetDirectories());
        assertEquals(0L, count.getDuplicateSize());
    }

    /*
     * Test detecting duplicates while having exceptions computing the size/hash.
     *
     *      --file1
     *     /
     * root---dir---file2 (throwing hash exception)
     *     \     \
     *      \     --file1Copy
     *       --file3 (throwing size exception)
     */
    @Test
    public void testWithChildrenException() throws Exception {
        ResourceFile file1 = mockFile("file1", 1L, 10L, "file1");
        ResourceFile file1Copy = mockFile("file1Copy", 1L, 11L, "file1");
        ResourceFile file2 = mockFile("file2", 2L, 20L, "file2");
        ResourceFile file3 = mockFile("file3", 0L, 30L, "file3");

        when(file2.getInputStream()).thenThrow(new IOException());
        when(file3.getSize()).thenThrow(new RuntimeException());

        ResourceDirectory dir = mockDirectory("dir", file2, file1Copy);

        List<DuplicateGroup> dups = analyzer.compute(ImmutableSet.of(file1, dir, file3), listener);

        assertEquals(1, dups.size());
        assertEquals(1L + "60b27f004e454aca81b0480209cce5081ec52390", dups.get(0).getHash());

        assertTrue(areTargetsFromResources(dups.get(0).getTargets(), file1, file1Copy));

        TargetImpl root = getTargetFromResource(dups.get(0).getTargets(), file1).getParent();
        assertFalse(root.isSized());
        assertFalse(root.isHashed());
        TargetImpl dirTarget = getTargetFromResource(dups.get(0).getTargets(), file1Copy).getParent();
        assertTrue(dirTarget.isSized());
        assertFalse(dirTarget.isHashed());

        verifyListener();

        SweeperCountImpl count = analyzer.getCount();
        assertEquals(5, count.getTotalTargets());
        assertEquals(4, count.getTotalTargetFiles());
        assertEquals(1, count.getTotalTargetDirectories());
        assertEquals(4L, count.getTotalSize());

        assertEquals(1, count.getDuplicateTargets());
        assertEquals(1, count.getDuplicateTargetFiles());
        assertEquals(0, count.getDuplicateTargetDirectories());
        assertEquals(1L, count.getDuplicateSize());
    }

    @Test
    public void testComputeException() throws Exception {
        try {
            analyzer.compute(null, listener);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            analyzer.compute(Collections.<Resource>emptySet(), null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            analyzer.compute(Collections.<Resource>emptySet(), listener);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testAbort() throws Exception {
        ResourceDirectory dir = mockDirectory("dir");
        Resource root = mockDirectory("root", dir);
        analyzer = PowerMockito.spy(analyzer);
        PowerMockito.when(analyzer, "checkAbortFlag").thenThrow(new SweeperAbortException());

        try {
            analyzer.compute(ImmutableSet.of(root), listener);
            fail();
        } catch (SweeperAbortException e) {
            // expected
            assertNull(analyzer.getCount());
        }
    }

}
