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
package gg.pistol.sweeper.core.hash;

import static org.junit.Assert.*;

import gg.pistol.sweeper.core.hash.Sha1Sum;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class Sha1SumTest {

    @Test
    public void testCompute() throws Exception {
        Sha1Sum sha1 = new Sha1Sum();
        ByteArrayInputStream stream = new ByteArrayInputStream("foo".getBytes("UTF-8"));
        assertEquals("0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33", sha1.compute(stream));
        
        stream = new ByteArrayInputStream("bar".getBytes("UTF-8"));
        assertEquals("62cdb7020ff920e5aa642c3d4066950dd1f01f4d", sha1.compute(stream));
        
        stream = new ByteArrayInputStream("".getBytes("UTF-8"));
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", sha1.compute(stream));
    }

}
