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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import com.google.common.base.Preconditions;

/**
 * File-system directory
 *
 * @author Bogdan Pistol
 */
public class ResourceDirectoryFs extends AbstractResource implements ResourceDirectory {

    private final File resource;

    private final String name;

    public ResourceDirectoryFs(File file) throws IOException {
        Preconditions.checkNotNull(file);
        Preconditions.checkArgument(file.isDirectory(), "The provided File <" + file.getPath() + "> is not representing a directory");
        resource = file.getCanonicalFile();
        name = resource.getPath();
    }

    public String getName() {
        return name;
    }

    public ResourceDirectory.ResourceCollectionResponse getSubresources() {
        Collection<Resource> resources = new ArrayList<Resource>();
        Collection<Exception> exceptions = new ArrayList<Exception>();

        File[] files = null;
        try {
            files = getFiles(resource);
        } catch (Exception e) {
            exceptions.add(e);
            return createResponse(resources, exceptions);
        }

        for (File f : files) {
            try {
                Resource r = createResource(f);
                resources.add(r);
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        return createResponse(resources, exceptions);
    }

    private File[] getFiles(File directory) throws IOException {
        File[] ret = directory.listFiles();
        if (ret == null) {
            throw new IOException("Cannot read the directory <" + directory.getPath() + ">");
        }
        return ret;
    }

    private ResourceDirectory.ResourceCollectionResponse createResponse(final Collection<Resource> resources,
            final Collection<Exception> exceptions) {
        return new ResourceDirectory.ResourceCollectionResponse() {

            public Collection<Resource> getResources() {
                return resources;
            }

            public Collection<Exception> getExceptions() {
                return exceptions;
            }
        };
    }

    private Resource createResource(File file) throws IOException {
        if (file.isFile()) {
            return new ResourceFileFs(file);
        } else if (file.isDirectory()) {
            return new ResourceDirectoryFs(file);
        } else {
            throw new IOException("Cannot create a resource from <" + file.getPath()
                    + ">, it is not a file or directory");
        }
    }

}
