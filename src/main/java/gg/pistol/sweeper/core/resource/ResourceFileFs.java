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

    public DateTime getModificationDate() {
        return new DateTime(resource.lastModified());
    }

}
