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

import org.mockito.ArgumentMatcher;

public class SweeperExceptionMatcher extends ArgumentMatcher<SweeperException> {

    private Class<? extends Exception> expectedCause;

    public SweeperExceptionMatcher(Class<? extends Exception> expectedCause) {
        this.expectedCause = expectedCause;
    }

    @Override
    public boolean matches(Object exception) {
        return exception.getClass() == SweeperException.class
                && ((SweeperException) exception).getCause().getClass() == expectedCause;
    }

}
