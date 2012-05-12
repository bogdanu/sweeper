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

import java.io.File;

import org.joda.time.DateTime;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

/**
 * Simple implementation that only provides the name of the resource.
 * 
 * @author Bogdan Pistol
 */
public class MockTarget implements SweeperTarget {
    
    private final String name;

    public MockTarget(File file) {
        name = file.getPath();
    }

    public int compareTo(SweeperTarget other) {
        Preconditions.checkNotNull(other);
        return ComparisonChain.start().compare(name, other.getName()).result();
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return null;
    }

    public long getSize() {
        return 0;
    }

    public DateTime getModificationDate() {
        return null;
    }

    public File getResource() {
        return null;
    }

    public Mark getMark() {
        return null;
    }

    public String getHash() {
        return null;
    }

}
