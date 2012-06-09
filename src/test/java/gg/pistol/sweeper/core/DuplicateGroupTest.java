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

public class DuplicateGroupTest {

    private DuplicateGroup group1;
    private DuplicateGroup group1Copy;
    private DuplicateGroup group2;

    @Before
    public void setUp() throws Exception {
        group1 = createGroup(20L, "hash1");
        group1Copy = createGroup(20L, "hash1");
        group2 = createGroup(10L, "hash2");
    }

    private DuplicateGroup createGroup(long size, String hash) {
        TargetImpl target1 = mockTarget("target-" + Math.random(), size, true, hash);
        TargetImpl target2 = mockTarget("target-" + Math.random(), size, true, hash);
        return new DuplicateGroup(ImmutableList.of(target1, target2));
    }

    private TargetImpl mockTarget(String name, long size, boolean isHashed, String hash) {
        TargetImpl target = mock(TargetImpl.class);
        when(target.getHash()).thenReturn(name);
        when(target.getSize()).thenReturn(size);
        when(target.isHashed()).thenReturn(isHashed);
        when(target.getHash()).thenReturn(hash);
        return target;
    }

    @Test
    public void testConstructor() throws Exception {
        TargetImpl target1 = mockTarget("target1", 1L, true, "hash");
        TargetImpl target2 = mockTarget("target2", 1L, true, "hash");
        DuplicateGroup group = new DuplicateGroup(ImmutableList.of(target1, target2));

        Iterator<Target> iterator = group.getTargets().iterator();
        assertEquals(target1, iterator.next());
        assertEquals(target2, iterator.next());

        assertEquals("hash", group.getHash());
        assertEquals(1L, group.getSize());
    }

    @Test
    public void testConstructorException() {
        try {
            new DuplicateGroup(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            new DuplicateGroup(Collections.<TargetImpl>emptyList());
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            new DuplicateGroup(Lists.newArrayList(mockTarget("target", 0L, false, null)));
            fail();
        } catch (IllegalArgumentException e) {
            // expected because it is not hashed
        }

        try {
            new DuplicateGroup(Lists.newArrayList(mockTarget("a", 0L, true, "foo"), mockTarget("b", 0L, true, "bar")));
            fail();
        } catch (IllegalArgumentException e) {
            // expected because hashes are different
        }

        try {
            new DuplicateGroup(Lists.newArrayList(mockTarget("a", 1L, true, "hash"), mockTarget("b", 2L, true, "hash")));
            fail();
        } catch (IllegalArgumentException e) {
            // expected because sizes are different
        }
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
