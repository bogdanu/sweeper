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

import java.util.Collection;

/**
 * A resource container that can have sub-resources.
 *
 * @author Bogdan Pistol
 */
public interface ResourceDirectory extends Resource {

    /**
     * Retrieve the sub-resources of this resource directory.
     *
     * <p>In case there are exceptions while resolving the sub-resources the response will contain a partial collection
     * of sub-resources and all the exceptions occurred.
     *
     * @return a response containing the collection of retrieved sub-resources and possibly a collection of exceptions
     *         encountered while retrieving the sub-resources
     */
    ResourceCollectionResponse getSubresources();

    /**
     * Determine if the implementation of this class supports deleting the directory with all of its contents or
     * alternatively only the empty directory (which will require the deletion of its content in advance).
     *
     * @return {@code true} if the {@link #delete} method only deletes an empty directory or {@code false} in case
     *         the method can delete the directory with all of its contents
     */
    boolean deleteOnlyEmpty();

    /**
     * A response that wraps a collection of resources and another collection of related exceptions.
     */
    interface ResourceCollectionResponse {

        /**
         * Getter for the collection of resources.
         *
         * @return the resources
         */
        Collection<? extends Resource> getResources();

        /**
         * Getter for the collection of exceptions.
         *
         * @return the exceptions
         */
        Collection<? extends Exception> getExceptions();

    }

}
