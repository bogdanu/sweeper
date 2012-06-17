/*
 * Sweeper - Duplicate file/folder cleaner
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

/**
 * Operation executed in the scope of {@link Sweeper#analyze} or {@link Sweeper#delete} method.
 *
 * <p><ul><li>The {@link Sweeper#analyze} method is doing the operations: {@link #RESOURCE_TRAVERSING},
 * {@link #SIZE_COMPUTATION}, {@link #SIZE_DEDUPLICATION}, {@link #HASH_COMPUTATION}, {@link #HASH_DEDUPLICATION},
 * {@link #COUNTING} and {@link #DUPLICATE_GROUPING}.</li>
 *
 * <li>The {@link Sweeper#delete} method is executing the {@link #RESOURCE_DELETION} operation.</li></ul>
 *
 * @author Bogdan Pistol
 */
public enum SweeperOperation {
    RESOURCE_TRAVERSING(29), SIZE_COMPUTATION(20), SIZE_DEDUPLICATION(1), HASH_COMPUTATION(47), HASH_DEDUPLICATION(1),
            COUNTING(1), DUPLICATE_GROUPING(1), RESOURCE_DELETION(100);

    private int percentQuota;

    private SweeperOperation(int percentQuota) {
        this.percentQuota = percentQuota;
    }

    /**
     * Getter for the percentage quota of this operation. The percentage quota is how much this operation contributes
     * to the global completion percentage. At completion the global percentage (the sum of all operation quotas) is 100%.
     *
     * @return the percent quota
     */
    // package private
    int getPercentQuota() {
        return percentQuota;
    }

}
