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

import java.util.List;

/**
 * Resolution of duplicate content, the list of duplicates can be retrieved with {@link #getTargets()} and then
 * every target can be marked with {@link Mark} values. When finished marking the {@link #endPoll()} method must be called
 * to notify the {@link Sweeper} that the poll is over.
 * 
 * @author Bogdan Pistol
 */
public interface SweeperPoll {
    
    List<SweeperTarget> getTargets();
    
    void mark(SweeperTarget target, Mark mark);
    
    void endPoll();

}
