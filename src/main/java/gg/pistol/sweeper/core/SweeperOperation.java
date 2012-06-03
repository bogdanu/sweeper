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
 * An operation executed by a {@link Sweeper} instance.
 * <p>
 * <ul>
 * <li>The {@link Sweeper#analyze()} method is doing the operations:
 * {@link #RESOURCE_TRAVERSING}, {@link #SIZE_COMPUTATION}, {@link #SIZE_DEDUPLICATION}, {@link #HASH_COMPUTATION},
 * {@link #HASH_DEDUPLICATION}, {@link #COUNTING} and {@link #DUPLICATE_GROUPING}.</li>
 *
 * <li>The {@link Sweeper#delete()} method is doing the {@link #FILESYSTEM_DELETION} operation.</li>
 * </ul>
 *
 * @author Bogdan Pistol
 */
public enum SweeperOperation {
    RESOURCE_TRAVERSING(29), SIZE_COMPUTATION(20), SIZE_DEDUPLICATION(1), HASH_COMPUTATION(47), HASH_DEDUPLICATION(1), COUNTING(1),
            DUPLICATE_GROUPING(1), FILESYSTEM_DELETION(100);

    private int percentProgress;

    private SweeperOperation(int percentQuota) {
        this.percentProgress = percentQuota;
    }

    public int getPercentQuota() {
        return percentProgress;
    }

}
