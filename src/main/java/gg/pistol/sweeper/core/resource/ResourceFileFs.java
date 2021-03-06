/*
 * Sweeper - Duplicate file cleaner
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
package gg.pistol.sweeper.core.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.joda.time.DateTime;

import com.google.common.base.Preconditions;

/**
 * File-system resource file implementation.
 *
 * @author Bogdan Pistol
 */
public class ResourceFileFs extends AbstractResource implements ResourceFile {

    private final File resource;
    private final String name;

    public ResourceFileFs(File file) throws IOException {
        Preconditions.checkNotNull(file);
        Preconditions.checkArgument(file.isFile(),
                "The provided File <" + file.getPath() + "> is not representing a normal file");
        resource = file.getCanonicalFile();
        name = resource.getPath();
        Preconditions.checkArgument(name != null, "The canonical path of the provided file is null");
    }

    public String getName() {
        return name;
    }

    public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(resource);
    }

    public long getSize() {
        return resource.length();
    }

    public DateTime getModificationDate() throws IOException {
        long time = resource.lastModified();
        if (time == 0L) {
            throw new IOException("Could not retrieve the modification date for the file <" + name + ">");
        }
        return new DateTime(time);
    }

    public void delete() throws IOException {
        if (!resource.delete()) {
            throw new IOException("Could not delete the file <" + name + ">");
        }
    }

}
