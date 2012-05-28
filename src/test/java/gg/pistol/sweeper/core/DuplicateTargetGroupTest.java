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
import static gg.pistol.sweeper.test.ObjectVerifier.*;

import java.util.Collections;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class DuplicateTargetGroupTest {

    private DuplicateTargetGroup group1;
    private DuplicateTargetGroup group1Copy;
    private DuplicateTargetGroup group2;

    @Before
    public void setUp() throws Exception {
        group1 = createGroup(20L, "hash1");
        group1Copy = createGroup(20L, "hash1");
        group2 = createGroup(10L, "hash2");
    }

    private DuplicateTargetGroup createGroup(long size, String hash) {
        SweeperTargetImpl target1 = mockTarget("target-" + Math.random(), size, true, hash);
        SweeperTargetImpl target2 = mockTarget("target-" + Math.random(), size, true, hash);
        return new DuplicateTargetGroup(ImmutableList.of(target1, target2));
    }

    private SweeperTargetImpl mockTarget(String name, long size, boolean isHashed, String hash) {
        SweeperTargetImpl target = mock(SweeperTargetImpl.class);
        when(target.getHash()).thenReturn(name);
        when(target.getSize()).thenReturn(size);
        when(target.isHashed()).thenReturn(isHashed);
        when(target.getHash()).thenReturn(hash);
        return target;
    }

    @Test
    public void testConstructor() throws Exception {
        SweeperTargetImpl target1 = mockTarget("target1", 1L, true, "hash");
        SweeperTargetImpl target2 = mockTarget("target2", 1L, true, "hash");
        DuplicateTargetGroup group = new DuplicateTargetGroup(ImmutableList.of(target1, target2));

        Iterator<SweeperTarget> iterator = group.getTargets().iterator();
        assertEquals(target1, iterator.next());
        assertEquals(target2, iterator.next());

        verify(target1).setDuplicateTargetGroup(group);
        verify(target2).setDuplicateTargetGroup(group);

        assertEquals("hash", group.getHash());
        assertEquals(1L, group.getSize());

        try {
            new DuplicateTargetGroup(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            new DuplicateTargetGroup(Collections.<SweeperTargetImpl>emptyList());
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            new DuplicateTargetGroup(Lists.newArrayList(mockTarget("target", 0L, false, null)));
            fail();
        } catch (IllegalArgumentException e) {
            // expected because not hashed
        }

        try {
            new DuplicateTargetGroup(Lists.newArrayList(mockTarget("a", 0L, true, "foo"), mockTarget("b", 0L, true, "bar")));
            fail();
        } catch (IllegalArgumentException e) {
            // expected because hashes are different
        }

        try {
            new DuplicateTargetGroup(Lists.newArrayList(mockTarget("a", 1L, true, "hash"), mockTarget("b", 2L, true, "hash")));
            fail();
        } catch (IllegalArgumentException e) {
            // expected because sizes are different
        }
    }

    @Test
    public void testSetPolled() {
        assertFalse(group1.isPolled());
        group1.setPolled(true);
        assertTrue(group1.isPolled());
    }

    @Test
    public void testSetTargetMarked() {
        assertFalse(group1.isTargetMarked());
        group1.setTargetMarked(true);
        assertTrue(group1.isTargetMarked());
    }

    @Test
    public void testCompareTo() {
        verifyCompareTo(group1, group1Copy, group2);
    }

    @Test
    public void testHashCode() {
        verifyHashCode(group1, group1Copy);
    }

    @Test
    public void testEquals() {
        verifyEquals(group1, group1Copy, group2);
    }

    @Test
    public void testToString() {
        verifyToString(group1);
    }

}
