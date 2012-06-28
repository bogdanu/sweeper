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
package gg.pistol.sweeper.core;

import javax.annotation.Nullable;

/**
 * A wrapper around the possible exceptions that could happen while executing the {@link Sweeper} operations.
 *
 * @author Bogdan Pistol
 */
public class SweeperException extends Exception {

    public SweeperException(@Nullable String msg) {
        super(msg);
    }

    public SweeperException(@Nullable Exception cause) {
        super(cause);
    }

    public SweeperException(@Nullable String msg, @Nullable Exception cause) {
        super(msg, cause);
    }

}
