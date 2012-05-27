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
package gg.pistol.sweeper.core.resource;

import static org.mockito.Mockito.*;
import static gg.pistol.sweeper.test.ObjectVerifier.*;

import gg.pistol.sweeper.core.resource.AbstractResource;
import gg.pistol.sweeper.core.resource.ResourceFileFs;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class AbstractResourceTest {
    
    private AbstractResource resource1;
    private AbstractResource resource1Copy;
    private AbstractResource resource2;

    @Before
    public void setUp() throws Exception {
        resource1 = createResource("bar");
        resource1Copy = createResource("bar");
        resource2 = createResource("foo");
    }
    
    private AbstractResource createResource(String name) throws Exception {
        File file = mock(File.class);
        when(file.isFile()).thenReturn(true);
        when(file.getCanonicalFile()).thenReturn(file);
        when(file.getPath()).thenReturn(name);
        return new ResourceFileFs(file);
    }

    @Test
    public void testCompareTo() {
        verifyCompareTo(resource1, resource1Copy, resource2);
    }
    
    @Test
    public void testHashCode() {
        verifyHashCode(resource1, resource1Copy);
    }

    @Test
    public void testEqualsObject() {
        verifyEquals(resource1, resource1Copy, resource2);
    }

    @Test
    public void testToString() {
        verifyToString(resource1);
    }

}
