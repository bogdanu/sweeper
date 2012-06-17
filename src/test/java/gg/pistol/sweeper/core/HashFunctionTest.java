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
import static org.mockito.Mockito.*;

import gg.pistol.sweeper.core.HashFunction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;

public class HashFunctionTest {

    private HashFunction hash;
    private InputStream inputStream;
    private OperationTrackingListener listener;
    private AtomicBoolean abortFlag;

    @Before
    public void setUp() throws Exception {
        hash = new HashFunction();
        inputStream = new ByteArrayInputStream("foo".getBytes("UTF-8"));
        listener = mock(OperationTrackingListener.class);
        abortFlag = new AtomicBoolean();
    }

    @Test
    public void testCompute() throws Exception {
        assertEquals("0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33", hash.compute(inputStream, listener, abortFlag));

        inputStream = new ByteArrayInputStream("".getBytes("UTF-8"));
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hash.compute(inputStream, listener, abortFlag));

        inputStream = new ByteArrayInputStream(new byte[6 * (1 << 20)]); // 6 MB
        hash.compute(inputStream, listener, abortFlag);
        verify(listener).incrementTargetActionProgress(anyLong());
    }

    @Test
    public void testComputeException() throws Exception {
        try {
            hash.compute(null, listener, abortFlag);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            hash.compute(inputStream, null, abortFlag);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            hash.compute(inputStream, listener, null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            hash.compute(inputStream, listener, new AtomicBoolean(true));
            fail();
        } catch (SweeperAbortException e) {
            // expected
        }
    }

}
