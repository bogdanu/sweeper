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
package gg.pistol.sweeper.test;

import static org.junit.Assert.*;

public class ObjectVerifier {

    public static <T extends Comparable> void verifyCompareTo(T first, T firstCopy, T second) {
        assertTrue(first.compareTo(first) == 0);
        assertTrue(first.compareTo(firstCopy) == 0);
        assertTrue(firstCopy.compareTo(first) == 0);

        assertTrue(first.compareTo(second) < 0);
        assertTrue(second.compareTo(first) > 0);

        try {
            first.compareTo(null);
            fail("Should throw NullPointerException on null comparison");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public static void verifyHashCode(Object obj, Object objCopy) {
        assertTrue(obj.equals(objCopy));
        assertTrue(obj.hashCode() == objCopy.hashCode());
    }

    public static void verifyEquals(Object first, Object firstCopy, Object second) {
        assertTrue(first.equals(first));
        assertTrue(first.equals(firstCopy));
        assertTrue(firstCopy.equals(first));

        assertFalse(first.equals(second));
        assertFalse(first.equals(null));
        assertFalse(first.equals(new Object()));
    }

    public static void verifyToString(Object obj) {
        assertNotNull(obj.toString());
        assertTrue(obj.toString().length() > 0);
    }
    
}
