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

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

/**
 * Mutable version of the {@link Integer} class.
 *
 * @author Bogdan Pistol
 */
public class MutableInteger extends Number implements Comparable<MutableInteger> {

    private static final long serialVersionUID = 1L;

    private int value;

    public MutableInteger(int value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void increment() {
        value++;
    }

    public void decrement() {
        value--;
    }

    public void add(int surplus) {
        value += surplus;
    }

    public void remove(int excess) {
        value -= excess;
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    public int compareTo(MutableInteger other) {
        Preconditions.checkNotNull(other);
        return Ints.compare(value, other.value);
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MutableInteger other = (MutableInteger) obj;
        return value == other.value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

}
