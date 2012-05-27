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

import java.io.IOException;
import java.io.InputStream;

import org.joda.time.DateTime;

/**
 * A resource item which contains byte data.
 * 
 * @author Bogdan Pistol
 */
public interface ResourceFile extends Resource {

    InputStream getInputStream() throws IOException;

    long getSize();

    DateTime getModificationDate();

}
