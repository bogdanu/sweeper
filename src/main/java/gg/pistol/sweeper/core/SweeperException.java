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
package gg.pistol.sweeper.core;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * A wrapper around the possible exceptions that could happen while executing the {@link Sweeper} operations.
 *
 * @author Bogdan Pistol
 */
public class SweeperException extends Exception {
    private static final long serialVersionUID = 1L;

    public SweeperException(String msg) {
        super(Preconditions.checkNotNull(msg));
    }

    public SweeperException(@Nullable Exception cause) {
        super(cause);
    }

    public SweeperException(String msg, @Nullable Exception cause) {
        super(Preconditions.checkNotNull(msg), cause);
    }

}
