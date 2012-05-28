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

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

public class OperationTrackingListenerTest {

    private SweeperOperationListener listener;

    private OperationTrackingListener wrapper;

    @Before
    public void setUp() throws Exception {
        listener = mock(SweeperOperationListener.class);
        wrapper = new OperationTrackingListener(listener);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor() {
        new OperationTrackingListener(null);
    }

    @Test
    public void testUpdateOperationProgress() {
        wrapper.updateOperationProgress(10);
        verify(listener).updateOperationProgress(10);
    }

    @Test
    public void testUpdateOperationPhase() {
        wrapper.updateOperationPhase(SweeperOperationPhase.SIZE_COMPUTATION);
        verify(listener).updateOperationPhase(SweeperOperationPhase.SIZE_COMPUTATION);
    }

    @Test
    public void testUpdateTargetAction() {
        SweeperTarget target = mock(SweeperTarget.class);
        wrapper.updateTargetAction(target, SweeperTargetAction.EXPAND);
        verify(listener).updateTargetAction(target, SweeperTargetAction.EXPAND);
    }

    @Test
    public void testUpdateTargetException() {
        SweeperTarget target = mock(SweeperTarget.class);
        SweeperException e = new SweeperException("");
        wrapper.updateTargetException(target, SweeperTargetAction.EXPAND, e);
        verify(listener).updateTargetException(target, SweeperTargetAction.EXPAND, e);
    }

    @Test
    public void testOperationFinished() {
        wrapper.operationFinished();
        verify(listener).operationFinished();
    }

    @Test
    public void testOperationAborted() {
        wrapper.operationAborted();
        verify(listener).operationAborted();
    }

    @Test
    public void testIncrementPhase() {
        wrapper.incrementOperation(SweeperOperationPhase.SIZE_COMPUTATION);

        long maxProgress = 50L;

        wrapper.incrementPhase(SweeperOperationPhase.SIZE_DEDUPLICATION, 10L, maxProgress);
        wrapper.incrementPhase(SweeperOperationPhase.SIZE_DEDUPLICATION, 40L, maxProgress);
        wrapper.incrementPhase(SweeperOperationPhase.SIZE_DEDUPLICATION, 40L, maxProgress);
        wrapper.incrementPhase(SweeperOperationPhase.SIZE_DEDUPLICATION, 0, 0);

        int expectedPercent1 = (int) (SweeperOperationPhase.SIZE_COMPUTATION.getPercentQuota() + SweeperOperationPhase.SIZE_DEDUPLICATION.getPercentQuota() * 10L / maxProgress);
        int expectedPercent2 = (int) (SweeperOperationPhase.SIZE_COMPUTATION.getPercentQuota() + SweeperOperationPhase.SIZE_DEDUPLICATION.getPercentQuota() * 40L / maxProgress);

        verify(listener).updateOperationProgress(expectedPercent1);
        verify(listener).updateOperationProgress(expectedPercent2);
    }

    @Test
    public void testIncrementOperation() {
        wrapper.incrementOperation(SweeperOperationPhase.SIZE_COMPUTATION);
        verify(listener).updateOperationProgress(SweeperOperationPhase.SIZE_COMPUTATION.getPercentQuota());

        wrapper.incrementOperation(SweeperOperationPhase.SIZE_DEDUPLICATION);
        verify(listener).updateOperationProgress(SweeperOperationPhase.SIZE_COMPUTATION.getPercentQuota() + SweeperOperationPhase.SIZE_DEDUPLICATION.getPercentQuota());

        wrapper.incrementPhase(SweeperOperationPhase.HASH_COMPUTATION, SweeperOperationPhase.HASH_COMPUTATION.getPercentQuota(), SweeperOperationPhase.HASH_COMPUTATION.getPercentQuota());
        wrapper.incrementOperation(SweeperOperationPhase.HASH_COMPUTATION);
        verify(listener, times(1)).updateOperationProgress(SweeperOperationPhase.SIZE_COMPUTATION.getPercentQuota() + SweeperOperationPhase.SIZE_DEDUPLICATION.getPercentQuota() + SweeperOperationPhase.HASH_COMPUTATION.getPercentQuota());
    }

}
