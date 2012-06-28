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

import gg.pistol.sweeper.core.resource.Resource;
import gg.pistol.sweeper.core.resource.ResourceDirectory;
import gg.pistol.sweeper.core.resource.ResourceFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
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
        listener = mock(SweeperOperationListener.class);
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
        doReturn(ImmutableList.copyOf(children)).when(subresResponse).getResources();
        doReturn(Collections.<Exception>emptyList()).when(subresResponse).getExceptions();
        return res;
    }

    private boolean areTargetsFromResources(Collection<? extends Target> targets, Resource... resources) {
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

    private TargetImpl getTargetFromResource(Collection<? extends Target> targets, Resource resource) {
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
    public void testAnalyzeDuplicateDir() throws Exception {
        long file1Size = 1L;
        String file1Content = "file1Content";
        ResourceFile file1 = mockFile("file1", file1Size, 10L, file1Content);

        long file2Size = 2L;
        String file2Content = "file2Content";
        ResourceFile file2 = mockFile("file2", file2Size, 11L, file2Content);
        ResourceDirectory dir = mockDirectory("dir", file1, file2);

        ResourceFile file1Copy = mockFile("file1Copy", file1Size, 20L, file1Content);
        ResourceFile file2Copy = mockFile("file2Copy", file2Size, 21L, file2Content);
        ResourceDirectory dirCopy = mockDirectory("dirCopy", file1Copy, file2Copy);

        ResourceFile someFile = mockFile("someFile", 5L, 30L, "someFileContent");
        ResourceDirectory upperDir = mockDirectory("upperDir", dir, someFile);

        Set<Resource> set = ImmutableSet.of((Resource) upperDir, dirCopy);
        NavigableSet<DuplicateGroup> dups = analyzer.analyze(set, listener);

        assertEquals(3, dups.size());
        Iterator<DuplicateGroup> iterator = dups.iterator();
        assertEquals((file1Size + file2Size) + "e20496eb93b914eeef887e311bcc6c56b739f4e0", iterator.next().getHash());
        assertEquals(file2Size + "6c46db5318dbb05719a85de973e4f1894149ce2d", iterator.next().getHash());
        assertEquals(file1Size + "5e24f8e3368074888321372b53d3e1b14b3f2858", iterator.next().getHash());

        iterator = dups.iterator();
        assertTrue(areTargetsFromResources(iterator.next().getTargets(), dir, dirCopy));
        assertTrue(areTargetsFromResources(iterator.next().getTargets(), file2, file2Copy));
        assertTrue(areTargetsFromResources(iterator.next().getTargets(), file1, file1Copy));

        TargetImpl dirCopyTarget = getTargetFromResource(dups.first().getTargets(), dirCopy);
        TargetImpl root = dirCopyTarget.getParent();

        assertEquals(root, analyzer.getRootTarget());
        assertTrue(dirCopyTarget.isHashed());
        assertTrue(root.isSized());
        assertFalse(root.isHashed());

        verifyAnalyzeListener();

        SweeperCountImpl count = analyzer.getCount();
        assertEquals(8, count.getTotalTargets());
        assertEquals(5, count.getTotalTargetFiles());
        assertEquals(3, count.getTotalTargetDirectories());
        assertEquals(11L, count.getTotalSize());

        assertEquals(3, count.getDuplicateTargets());
        assertEquals(2, count.getDuplicateTargetFiles());
        assertEquals(1, count.getDuplicateTargetDirectories());
        assertEquals(file1Size + file2Size, count.getDuplicateSize());
    }

    private void verifyAnalyzeListener() {
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
     *      --emptyDir
     *     /
     * root---emptyFile1
     *     \
     *      --dir---emptyFile2
     *           \
     *            --emptyFile3
     *
     * The following are duplicates: emptyFile1 = emptyFile2 = emptyFile3 = dir. The root is not considered because it
     * is not a real file or directory). Notice that files can be equal with directories, this is on purpose to have
     * more flexibility when cleaning duplicate content.
     */
    @Test
    public void testAnalyzeEmpty() throws Exception {
        ResourceDirectory emptyDir = mockDirectory("emptyDir");
        ResourceFile emptyFile1 = mockFile("emptyFile1", 0L, 10L, "");
        ResourceFile emptyFile2 = mockFile("emptyFile2", 0L, 11L, "");
        ResourceFile emptyFile3 = mockFile("emptyFile3", 0L, 12L, "");
        ResourceDirectory dir = mockDirectory("dir", emptyFile2, emptyFile3);

        Set<Resource> set = ImmutableSet.of(emptyDir, emptyFile1, dir);
        NavigableSet<DuplicateGroup> dups = analyzer.analyze(set, listener);

        assertEquals(1, dups.size());
        assertEquals(0L + "da39a3ee5e6b4b0d3255bfef95601890afd80709", dups.first().getHash());

        assertTrue(areTargetsFromResources(dups.first().getTargets(), emptyDir, emptyFile1, emptyFile2, emptyFile3, dir));

        verifyAnalyzeListener();

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
    public void testAnalyzeSingleFile() throws Exception {
        long fileSize = 1L;
        String fileContent = "fileContent";
        ResourceFile file = mockFile("file", fileSize, 10L, fileContent);
        ResourceFile fileCopy = mockFile("fileCopy", fileSize, 11L, fileContent);
        ResourceDirectory dir = mockDirectory("dir", file);

        Set<Resource> set = ImmutableSet.of(fileCopy, dir);
        NavigableSet<DuplicateGroup> dups = analyzer.analyze(set, listener);

        assertEquals(1, dups.size());
        assertEquals(fileSize + "5d6829b05a40a708a8661a63359145c3f4376883", dups.first().getHash());

        assertTrue(areTargetsFromResources(dups.first().getTargets(), dir, fileCopy));

        verifyAnalyzeListener();

        SweeperCountImpl count = analyzer.getCount();
        assertEquals(3, count.getTotalTargets());
        assertEquals(2, count.getTotalTargetFiles());
        assertEquals(1, count.getTotalTargetDirectories());
        assertEquals(fileSize * 2, count.getTotalSize());

        // The duplicate count depends on the targets that need to be removed, if "dir" and "file" are considered to be
        // duplicates then there are 2 duplicate targets, otherwise if "fileCopy" is considered to be duplicate then
        // there is one duplicate target.
        assertTrue(ImmutableSet.of(1, 2).contains(count.getDuplicateTargets()));
        assertEquals(1, count.getDuplicateTargetFiles());
        assertTrue(ImmutableSet.of(0, 1).contains(count.getDuplicateTargetDirectories()));
        assertEquals(fileSize, count.getDuplicateSize());
    }

    /*
     * Test the analyze() method on more than INITIAL_EXPAND_LIMIT targets.
     */
    @Test
    public void testAnalyzeLimit() throws Exception {
        int targets = 103;
        Set<Resource> set = new HashSet<Resource>();
        for (int i = 0; i < targets; i++) {
            set.add(mockFile("file" + i, i, i, "file" + i));
        }

        NavigableSet<DuplicateGroup> dups = analyzer.analyze(set, listener);

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
     * root---dir---file2 (file1 copy that throws hash exception)
     *     \     \
     *      \     --file1Copy
     *       --file3 (throwing size exception)
     */
    @Test
    public void testAnalyzeChildrenException() throws Exception {
        long file1Size = 1L;
        String file1Content = "file1Content";
        ResourceFile file1 = mockFile("file1", file1Size, 10L, file1Content);
        ResourceFile file1Copy = mockFile("file1Copy", file1Size, 11L, file1Content);
        ResourceFile file2 = mockFile("file2", file1Size, 20L, "file2Content");
        ResourceFile file3 = mockFile("file3", 0L, 30L, "file3Content");

        when(file2.getInputStream()).thenThrow(new IOException());
        when(file3.getSize()).thenThrow(new RuntimeException());

        ResourceDirectory dir = mockDirectory("dir", file2, file1Copy);

        NavigableSet<DuplicateGroup> dups = analyzer.analyze(ImmutableSet.of(file1, dir, file3), listener);

        assertEquals(1, dups.size());
        assertEquals(file1Size + "5e24f8e3368074888321372b53d3e1b14b3f2858", dups.first().getHash());

        assertTrue(areTargetsFromResources(dups.first().getTargets(), file1, file1Copy));

        TargetImpl root = analyzer.getRootTarget();
        assertFalse(root.isSized());
        assertFalse(root.isHashed());
        TargetImpl dirTarget = getTargetFromResource(dups.first().getTargets(), file1Copy).getParent();
        assertTrue(dirTarget.isSized());
        assertFalse(dirTarget.isHashed());

        verifyAnalyzeListener();

        SweeperCountImpl count = analyzer.getCount();
        assertEquals(5, count.getTotalTargets());
        assertEquals(4, count.getTotalTargetFiles());
        assertEquals(1, count.getTotalTargetDirectories());
        assertEquals(3L, count.getTotalSize());

        assertEquals(1, count.getDuplicateTargets());
        assertEquals(1, count.getDuplicateTargetFiles());
        assertEquals(0, count.getDuplicateTargetDirectories());
        assertEquals(file1Size, count.getDuplicateSize());
    }

    /*
     * Test fixing the multiple target parent situations.
     *
     * root---res1---dir---res2
     *     \
     *      --res2
     *
     * In this case res2 has two parents: root and dir, to prevent this from happening the "root---res2" child should
     * be removed.
     */
    @Test
    public void testAnalyzeMultipleParent() throws Exception {
        ResourceFile res2 = mockFile("res2", 0L, 0L, "");
        ResourceDirectory dir = mockDirectory("dir", res2);
        ResourceDirectory res1 = mockDirectory("res1", dir);

        analyzer.analyze(ImmutableSet.of(res1, res2), listener);

        assertEquals(1, analyzer.getRootTarget().getChildren().size());
    }

    @Test
    public void testAnalyzeException() throws Exception {
        try {
            analyzer.analyze(null, listener);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            analyzer.analyze(Collections.<Resource>emptySet(), null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            analyzer.analyze(Collections.<Resource>emptySet(), listener);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /*
     * Test deleting recursively the following hierarchy:
     *
     *             --file1
     *            /
     * root---dir1---file2
     *     \
     *      --dir2---file3
     *            \
     *             --file4
     */
    @Test
    public void testRecursiveDelete() throws Exception {
        ResourceFile file1 = mockFile("file1", 0, 0, "");
        ResourceFile file2 = mockFile("file2", 0, 0, "");
        ResourceFile file3 = mockFile("file3", 0, 0, "");
        ResourceFile file4 = mockFile("file4", 0, 0, "");
        ResourceDirectory dir1 = mockDirectory("dir1", file1, file2);
        ResourceDirectory dir2 = mockDirectory("dir2", file3, file4);
        when(dir1.deleteOnlyEmpty()).thenReturn(true);
        when(dir2.deleteOnlyEmpty()).thenReturn(true);

        analyzer.analyze(ImmutableSet.of(dir1, dir2), listener);
        reset(listener);
        analyzer.delete(ImmutableSet.of(analyzer.getRootTarget()), listener);

        verify(listener).updateOperation(SweeperOperation.RESOURCE_DELETION);
        verify(listener, times(6)).updateTargetAction(any(Target.class), eq(TargetAction.DELETE));
        verify(listener).updateOperationProgress(anyLong(), anyLong(), eq(100));
        verify(file1).delete();
        verify(file2).delete();
        verify(file3).delete();
        verify(file4).delete();
        verify(dir1).delete();
        verify(dir2).delete();
    }

    /*
     * Test deleting non-recursively the following hierarchy:
     *
     * root---dir---file1
     *           \
     *            --file2
     */
    @Test
    public void testNonRecursiveDelete() throws Exception {
        ResourceFile file1 = mockFile("file1", 0, 0, "");
        ResourceFile file2 = mockFile("file2", 0, 0, "");
        ResourceDirectory dir = mockDirectory("dir", file1, file2);
        when(dir.deleteOnlyEmpty()).thenReturn(false);

        analyzer.analyze(ImmutableSet.of(dir), listener);
        reset(listener);

        Collection<Target> toDelete = new ArrayList<Target>();
        toDelete.add(analyzer.getRootTarget());
        toDelete.addAll(analyzer.getRootTarget().getChildren()); // test also multiple instance of the same target
        analyzer.delete(toDelete, listener);

        verify(listener).updateOperation(SweeperOperation.RESOURCE_DELETION);
        verify(listener, times(1)).updateTargetAction(any(Target.class), eq(TargetAction.DELETE));
        verify(listener).updateOperationProgress(anyLong(), anyLong(), eq(100));
        verify(file1, never()).delete();
        verify(file2, never()).delete();
        verify(dir).delete();
    }

    @Test
    public void testDeleteException() throws Exception {
        try {
            analyzer.delete(null, listener);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            analyzer.delete(Collections.<Target>emptySet(), null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            analyzer.delete(Collections.<Target>emptySet(), listener);
            fail();
        } catch (IllegalArgumentException e) {
            // expected because of the empty collection
        }
    }

    @Test
    public void testAbort() throws Exception {
        ResourceDirectory dir = mockDirectory("dir");
        Resource root = mockDirectory("root", dir);
        analyzer = PowerMockito.spy(analyzer);
        PowerMockito.when(analyzer, "checkAbortFlag").thenThrow(new SweeperAbortException());

        try {
            analyzer.analyze(ImmutableSet.of(root), listener);
            fail();
        } catch (SweeperAbortException e) {
            // expected
            assertNull(analyzer.getCount());
        }
    }

}
