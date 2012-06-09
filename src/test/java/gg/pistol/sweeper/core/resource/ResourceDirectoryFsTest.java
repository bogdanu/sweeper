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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Iterator;

import org.junit.Test;

public class ResourceDirectoryFsTest {

    @Test
    public void testConstructor() throws Exception {
        File dir = mockFile("foo", true);
        File canonicalDir = mockFile("bar", true);
        when(dir.getCanonicalFile()).thenReturn(canonicalDir);

        ResourceDirectoryFs res = new ResourceDirectoryFs(dir);
        assertEquals("bar", res.getName());

        when(dir.isDirectory()).thenReturn(false);
        try {
            new ResourceDirectoryFs(dir);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    private File mockFile(String name, boolean isDirectory) throws Exception {
        File file = mock(File.class);
        if (isDirectory) {
            when(file.isDirectory()).thenReturn(true);
        } else {
            when(file.isFile()).thenReturn(true);
        }
        when(file.getPath()).thenReturn(name);
        when(file.getCanonicalFile()).thenReturn(file);
        return file;
    }

    @Test
    public void testGetSubresources() throws Exception {
        File dir = mockFile("foo", true);

        File child1 = mockFile("child1", true);
        File child2 = mockFile("child2", false);
        when(dir.listFiles()).thenReturn(new File[] {child1, child2});

        ResourceDirectoryFs res = new ResourceDirectoryFs(dir);
        Iterator<? extends Resource> iterator = res.getSubresources().getResources().iterator();

        assertEquals("child1", ((ResourceDirectoryFs) iterator.next()).getName());
        assertEquals("child2", ((ResourceFileFs) iterator.next()).getName());
        assertTrue(res.getSubresources().getExceptions().isEmpty());

        when(child1.isDirectory()).thenReturn(false);
        assertEquals(1, res.getSubresources().getResources().size());
        assertEquals(1, res.getSubresources().getExceptions().size());

        when(dir.listFiles()).thenReturn(null);
        assertTrue(res.getSubresources().getResources().isEmpty());
        assertEquals(1, res.getSubresources().getExceptions().size());
    }

}
