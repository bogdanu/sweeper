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

/**
 * An execution part of a {@link Sweeper} operation. Another way of saying it is that an operation is composed by
 * different phases.
 * <p>
 * <ul>
 * <li>The analysis operation (calling the method {@link Sweeper#analyze()}) is composed by the
 * {@link #FILESYSTEM_TRAVERSING}, {@link #SIZE_COMPUTATION}, {@link #SIZE_DEDUPLICATION}, {@link #HASH_COMPUTATION},
 * {@link #HASH_DEDUPLICATION}, {@link #COUNTING} and {@link #DUPLICATE_GROUPING} phases.</li>
 *
 * <li>The deletion operation (calling the method {@link Sweeper#delete()}) is composed by the
 * {@link #FILESYSTEM_DELETION} phase.</li>
 * </ul>
 *
 * @author Bogdan Pistol
 */
public enum SweeperOperationPhase {
    FILESYSTEM_TRAVERSING(29), SIZE_COMPUTATION(20), SIZE_DEDUPLICATION(1), HASH_COMPUTATION(47), HASH_DEDUPLICATION(1), COUNTING(1),
            DUPLICATE_GROUPING(1), FILESYSTEM_DELETION(100);

    private int percentProgress;

    private SweeperOperationPhase(int percentQuota) {
        this.percentProgress = percentQuota;
    }

    public int getPercentQuota() {
        return percentProgress;
    }

}
