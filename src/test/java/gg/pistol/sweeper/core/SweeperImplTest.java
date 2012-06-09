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

import gg.pistol.sweeper.core.SweeperPoll.Mark;
import gg.pistol.sweeper.core.resource.Resource;

import java.util.Collections;
import java.util.NavigableSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

public class SweeperImplTest {

    private SweeperImpl sweeper;
    private Analyzer analyzer;
    private SweeperCountImpl count;

    private NavigableSet<Resource> resources;
    private SweeperOperationListener listener;

    @Before
    public void setUp() throws Exception {
        analyzer = mock(Analyzer.class);
        count = mock(SweeperCountImpl.class);
        when(analyzer.getCount()).thenReturn(count);

        sweeper = new SweeperImpl(analyzer);

        resources = Sets.newTreeSet(Collections.singleton(mock(Resource.class)));
        listener = mock(SweeperOperationListener.class);
    }

    private void analyzerReturns(DuplicateGroup... dups) throws SweeperAbortException {
        if (dups == null) {
            dups = new DuplicateGroup[] {};
        }
        NavigableSet<DuplicateGroup> set = Sets.newTreeSet(ImmutableSet.copyOf(dups));
        when(analyzer.compute(resources, listener)).thenReturn(set);
    }

    @Test
    public void testAnalyze() throws Exception {
        analyzerReturns(mock(DuplicateGroup.class), mock(DuplicateGroup.class));
        sweeper.analyze(resources, listener);

        verify(analyzer).compute(resources, listener);
        assertEquals(count, sweeper.getCount());
        assertTrue(sweeper.getToDeleteTargets().isEmpty());

        try {
            sweeper.analyze(null, listener);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            sweeper.analyze(resources, null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            sweeper.analyze(Collections.<Resource>emptySet(), listener);
            fail();
        } catch (IllegalArgumentException e) {
            // expected because the resource collection is empty
        }
    }

    private TargetImpl mockTarget(TargetImpl parent, int totalTargets) {
        TargetImpl target = mock(TargetImpl.class);
        if (parent != null) {
            when(target.getParent()).thenReturn(parent);
        }
        when(target.getTotalTargets()).thenReturn(totalTargets);
        return target;
    }

    private DuplicateGroup mockDuplicate(long size, TargetImpl... targets) {
        DuplicateGroup dup = mock(DuplicateGroup.class);
        doReturn(ImmutableSet.copyOf(targets)).when(dup).getTargets();
        when(dup.getSize()).thenReturn(size);
        when(dup.compareTo(any(DuplicateGroup.class))).thenAnswer(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                long s1 = ((DuplicateGroup) invocation.getMock()).getSize();
                long s2 = ((DuplicateGroup) invocation.getArguments()[0]).getSize();
                return ComparisonChain.start().compare(s1, s2, Ordering.natural().reverse()).result();
            }
        });
        return dup;
    }

    @Test
    public void testNextPrevious() throws Exception {
        TargetImpl file1 = mockTarget(null, 0);
        TargetImpl file2 = mockTarget(null, 0);
        TargetImpl file1Copy = mockTarget(null, 0);
        TargetImpl file2Copy = mockTarget(null, 0);

        DuplicateGroup dup1 = mockDuplicate(2, file1, file1Copy);
        DuplicateGroup dup2 = mockDuplicate(1, file2, file2Copy);

        analyzerReturns(dup1, dup2);
        sweeper.analyze(resources, listener);

        assertNull(sweeper.previousPoll());

        // forward
        SweeperPoll poll = sweeper.nextPoll();
        assertEquals(ImmutableSet.of(file1, file1Copy), poll.getTargets());
        assertEquals(poll, sweeper.getCurrentPoll());

        poll = sweeper.nextPoll();
        assertEquals(ImmutableSet.of(file2, file2Copy), poll.getTargets());
        assertEquals(poll, sweeper.getCurrentPoll());

        // backward
        poll = sweeper.previousPoll();
        assertEquals(ImmutableSet.of(file1, file1Copy), poll.getTargets());
        assertEquals(poll, sweeper.getCurrentPoll());

        assertNull(sweeper.previousPoll());
        assertEquals(poll, sweeper.getCurrentPoll());

        // forward again
        for (int i = 0; i < 3; i++) {
            poll = sweeper.nextPoll();
            assertEquals(ImmutableSet.of(file2, file2Copy), poll.getTargets());
            assertEquals(poll, sweeper.getCurrentPoll());

            poll = sweeper.nextPoll();
            assertEquals(ImmutableSet.of(file1, file1Copy), poll.getTargets());
            assertEquals(poll, sweeper.getCurrentPoll());
        }
    }

    @Test
    public void testNextDefaultMark() throws Exception {
        TargetImpl dir = mockTarget(null, 3);
        TargetImpl file1 = mockTarget(dir, 1);
        TargetImpl file2 = mockTarget(dir, 1);

        TargetImpl dirCopy = mockTarget(null, 3);
        TargetImpl file1Copy = mockTarget(dirCopy, 1);
        TargetImpl file2Copy = mockTarget(dirCopy, 1);

        DuplicateGroup dup1 = mockDuplicate(3, dir, dirCopy);
        DuplicateGroup dup2 = mockDuplicate(2, file1, file1Copy);
        DuplicateGroup dup3 = mockDuplicate(1, file2, file2Copy);

        analyzerReturns(dup1, dup2, dup3);
        sweeper.analyze(resources, listener);

        SweeperPoll poll = sweeper.nextPoll();
        poll.mark(dirCopy, Mark.DELETE);

        assertNull(sweeper.nextPoll());
        verify(count).setToDeleteTargets(3);
        assertEquals(1, sweeper.getToDeleteTargets().size());

        sweeper.getCurrentPoll().mark(dirCopy, Mark.RETAIN);
        poll = sweeper.nextPoll();

        assertEquals(Mark.RETAIN, poll.getMark(file1Copy));
        assertEquals(Mark.DELETE, poll.getMark(file1));
    }

    @Test
    public void testDelete() {
    }

}
