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
//        wrapper.updateOperationProgress(10);
//        verify(listener).updateOperationProgress(10);
    }

    @Test
    public void testUpdateOperationPhase() {
        wrapper.updateOperation(SweeperOperation.SIZE_COMPUTATION);
        verify(listener).updateOperation(SweeperOperation.SIZE_COMPUTATION);
    }

    @Test
    public void testUpdateTargetAction() {
        Target target = mock(Target.class);
        wrapper.updateTargetAction(target, TargetAction.EXPAND);
        verify(listener).updateTargetAction(target, TargetAction.EXPAND);
    }

    @Test
    public void testUpdateTargetException() {
        Target target = mock(Target.class);
        SweeperException e = new SweeperException("");
        wrapper.updateTargetException(target, TargetAction.EXPAND, e);
        verify(listener).updateTargetException(target, TargetAction.EXPAND, e);
    }

    @Test
    public void testUpdateHashProgress() {
//        wrapper.updateHashProgress(1L, 2L);
//        verify(listener).updateHashProgress(1L, 2L);
    }

    @Test
    public void testOperationFinished() {
//        wrapper.operationFinished();
//        verify(listener).operationFinished();
    }

    @Test
    public void testOperationAborted() {
//        wrapper.operationAborted();
//        verify(listener).operationAborted();
    }

    @Test
    public void testIncrementPhase() {
//        wrapper.operationCompleted(SweeperOperation.SIZE_COMPUTATION);
//
//        long maxProgress = 50L;
//
//        wrapper.incrementPhase(SweeperOperation.SIZE_DEDUPLICATION, 10L, maxProgress);
//        wrapper.incrementPhase(SweeperOperation.SIZE_DEDUPLICATION, 40L, maxProgress);
//        wrapper.incrementPhase(SweeperOperation.SIZE_DEDUPLICATION, 40L, maxProgress);
//        wrapper.incrementPhase(SweeperOperation.SIZE_DEDUPLICATION, 0, 0);
//
//        int expectedPercent1 = (int) (SweeperOperation.SIZE_COMPUTATION.getPercentQuota() + SweeperOperation.SIZE_DEDUPLICATION.getPercentQuota() * 10L / maxProgress);
//        int expectedPercent2 = (int) (SweeperOperation.SIZE_COMPUTATION.getPercentQuota() + SweeperOperation.SIZE_DEDUPLICATION.getPercentQuota() * 40L / maxProgress);
//
//        verify(listener).updateOperationProgress(expectedPercent1);
//        verify(listener).updateOperationProgress(expectedPercent2);
    }

    @Test
    public void testIncrementOperation() {
//        wrapper.operationCompleted(SweeperOperation.SIZE_COMPUTATION);
//        verify(listener).updateOperationProgress(SweeperOperation.SIZE_COMPUTATION.getPercentQuota());
//
//        wrapper.operationCompleted(SweeperOperation.SIZE_DEDUPLICATION);
//        verify(listener).updateOperationProgress(SweeperOperation.SIZE_COMPUTATION.getPercentQuota() + SweeperOperation.SIZE_DEDUPLICATION.getPercentQuota());
//
//        wrapper.incrementPhase(SweeperOperation.HASH_COMPUTATION, SweeperOperation.HASH_COMPUTATION.getPercentQuota(), SweeperOperation.HASH_COMPUTATION.getPercentQuota());
//        wrapper.operationCompleted(SweeperOperation.HASH_COMPUTATION);
//        verify(listener, times(1)).updateOperationProgress(SweeperOperation.SIZE_COMPUTATION.getPercentQuota() + SweeperOperation.SIZE_DEDUPLICATION.getPercentQuota() + SweeperOperation.HASH_COMPUTATION.getPercentQuota());
    }

}
