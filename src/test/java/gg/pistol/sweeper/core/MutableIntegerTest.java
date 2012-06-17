/*
 * Sweeper - Duplicate file/folder cleaner
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
import static gg.pistol.sweeper.test.ObjectVerifier.*;

import org.junit.Before;
import org.junit.Test;

public class MutableIntegerTest {

    private MutableInteger integer;

    @Before
    public void setUp() throws Exception {
        integer = new MutableInteger(0);
    }

    @Test
    public void testHashCode() {
        verifyHashCode(new MutableInteger(1), new MutableInteger(1));
    }

    @Test
    public void testIntValue() {
        assertEquals(0, integer.intValue());
    }

    @Test
    public void testLongValue() {
        assertEquals(0L, integer.longValue());
    }

    @Test
    public void testFloatValue() {
        assertEquals(0F, integer.floatValue(), 0F);
    }

    @Test
    public void testDoubleValue() {
        assertEquals(0F, integer.doubleValue(), 0F);
    }

    @Test
    public void testSetValue() {
        integer.setValue(1);
        assertEquals(1, integer.intValue());
    }

    @Test
    public void testIncrement() {
        integer.increment();
        assertEquals(1, integer.intValue());
    }

    @Test
    public void testDecrement() {
        integer.decrement();
        assertEquals(-1, integer.intValue());
    }

    @Test
    public void testAdd() {
        integer.add(2);
        assertEquals(2, integer.intValue());
    }

    @Test
    public void testRemove() {
        integer.remove(2);
        assertEquals(-2, integer.intValue());
    }

    @Test
    public void testCompareTo() {
        verifyCompareTo(new MutableInteger(-1), new MutableInteger(-1), integer);
    }

    @Test
    public void testEqualsObject() {
        verifyEquals(new MutableInteger(1), new MutableInteger(1), integer);
    }

    @Test
    public void testToString() {
        assertEquals("0", integer.toString());
    }

}
