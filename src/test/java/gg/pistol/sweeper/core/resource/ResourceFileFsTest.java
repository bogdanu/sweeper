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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gg.pistol.sweeper.core.resource.ResourceDirectoryFs;
import gg.pistol.sweeper.core.resource.ResourceFileFs;

import java.io.File;

import org.joda.time.DateTime;
import org.junit.Test;

public class ResourceFileFsTest {

    @Test
    public void testConstructor() throws Exception {
        File file = mockFile("foo");
        File canonicalFile = mockFile("bar");
        when(file.getCanonicalFile()).thenReturn(canonicalFile);

        ResourceFileFs res = new ResourceFileFs(file);
        assertEquals("bar", res.getName());

        when(file.isFile()).thenReturn(false);
        try {
            new ResourceDirectoryFs(file);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    private File mockFile(String name) throws Exception {
        File file = mock(File.class);
        when(file.isFile()).thenReturn(true);
        when(file.getPath()).thenReturn(name);
        when(file.getCanonicalFile()).thenReturn(file);
        return file;
    }

    @Test
    public void testGetSize() throws Exception {
        File file = mockFile("foo");
        when(file.length()).thenReturn(10L);
        ResourceFileFs res = new ResourceFileFs(file);

        assertEquals(10L, res.getSize());
    }

    @Test
    public void testGetModificationDate() throws Exception {
        File file = mockFile("foo");
        DateTime time = new DateTime();
        when(file.lastModified()).thenReturn(time.getMillis());
        ResourceFileFs res = new ResourceFileFs(file);

        assertEquals(time, res.getModificationDate());
    }

}
