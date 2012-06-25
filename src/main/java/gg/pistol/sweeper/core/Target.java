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

import javax.annotation.Nullable;

import gg.pistol.sweeper.core.resource.Resource;

import org.joda.time.DateTime;

/**
 * A sweep candidate.
 *
 * @author Bogdan Pistol
 */
public interface Target extends Comparable<Target> {

    /**
     * Getter for the target's name.
     *
     * @return the name
     */
    String getName();

    /**
     * Getter for the target's type.
     *
     * @return the type
     */
    Type getType();

    /**
     * Getter for the target's size. If the target's type is {@link Type#ROOT} or the target's resource is a directory
     * then the size is the sum of all the children's sizes.
     *
     * @return the size in bytes
     */
    long getSize();

    /**
     * Getter for the target's modification date. If the target's type is {@link Type#ROOT} or the target's resource is
     * a directory then the modification date is the modification date of the latest modified child.
     *
     * @return the modification date or {@code null} in case the target is an empty directory
     */
    @Nullable
    DateTime getModificationDate();

    /**
     * Getter for the resource wrapped by this target.
     *
     * @return the wrapped resource or {@code null} in case of a {@link Type#ROOT} target
     */
    @Nullable
    Resource getResource();

    /**
     * The target types.
     */
    enum Type {
        FILE, DIRECTORY, ROOT
    }

}
