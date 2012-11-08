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

import java.io.IOException;
import java.io.InputStream;

import org.joda.time.DateTime;

/**
 * An individual resource which contains byte data.
 *
 * @author Bogdan Pistol
 */
public interface ResourceFile extends Resource {

    /**
     * Open an {@link InputStream} that reads the resource content.
     *
     * @return the resource input stream
     * @throws IOException
     *         if the underlying implementation experiences I/O exceptions while opening the input stream
     */
    InputStream getInputStream() throws IOException;

    /**
     * Retrieve the size of the resource content.
     *
     * @return the size of the content in bytes
     * @throws IOException
     *         if the underlying implementation experiences I/O exceptions while getting the size
     */
    long getSize() throws IOException;

    /**
     * Retrieve the latest modification date of the resource.
     *
     * @return the latest modification date
     * @throws IOException
     *         if the underlying implementation experiences I/O exceptions while getting the modification date
     */
    DateTime getModificationDate() throws IOException;

}
