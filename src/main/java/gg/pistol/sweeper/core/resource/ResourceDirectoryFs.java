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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Preconditions;

/**
 * File-system resource directory implementation.
 *
 * <p>This implementation can only delete empty directories.
 *
 * @author Bogdan Pistol
 */
public class ResourceDirectoryFs extends AbstractResource implements ResourceDirectory {

    private final File resource;
    private final String name;

    public ResourceDirectoryFs(File file) throws IOException {
        Preconditions.checkNotNull(file);
        Preconditions.checkArgument(file.isDirectory(),
                "The provided File <" + file.getPath() + "> is not representing a directory");
        resource = file.getCanonicalFile();
        name = resource.getPath();
        Preconditions.checkArgument(name != null, "The canonical path of the provided file is null");
    }

    public String getName() {
        return name;
    }

    public ResourceDirectory.ResourceCollectionResponse getSubresources() {
        File[] files;
        try {
            files = getFiles(resource);
        } catch (Exception e) {
            return createResponse(Collections.<Resource>emptyList(), Collections.singleton(e));
        }

        Collection<Resource> resources = new ArrayList<Resource>();
        Collection<Exception> exceptions = null;

        String prefix = name;
        if (!prefix.endsWith(File.separator)) {
            prefix += File.separator;
        }
        for (File f : files) {
            try {
                Resource r = createResource(f);
                if (r.getName().startsWith(prefix)) { // defend against cycles created by symbolic links
                    resources.add(r);
                }
            } catch (Exception e) {
                if (exceptions == null) {
                    exceptions = new ArrayList<Exception>();
                }
                exceptions.add(e);
            }
        }

        if (exceptions == null) {
            exceptions = Collections.emptyList();
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

    private ResourceDirectory.ResourceCollectionResponse createResponse(final Collection<? extends Resource> resources,
                                                                        final Collection<? extends Exception> exceptions) {
        return new ResourceDirectory.ResourceCollectionResponse() {

            public Collection<? extends Resource> getResources() {
                return resources;
            }

            public Collection<? extends Exception> getExceptions() {
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

    public boolean deleteOnlyEmpty() {
        return true;
    }

    public void delete() throws IOException {
        if (!resource.delete()) {
            throw new IOException("Could not delete the directory <" + name + ">");
        }
    }

}
