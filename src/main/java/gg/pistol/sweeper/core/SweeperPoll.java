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

import gg.pistol.sweeper.core.SweeperTarget.Mark;

import java.util.Collection;

/**
 * Resolution of duplicate content. The collection of duplicates can be retrieved with {@link #getTargets()} and then
 * every target can be marked with {@link Mark} values.
 *
 * @author Bogdan Pistol
 */
public interface SweeperPoll {

    Collection<SweeperTarget> getTargets();

}
