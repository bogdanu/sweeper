/*
 * Sweeper - Duplicate file cleaner
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

public class OperationTrackingListenerTest {

    private SweeperOperationListener wrappedListener;
    private OperationTrackingListener trackingListener;

    @Before
    public void setUp() throws Exception {
        wrappedListener = mock(SweeperOperationListener.class);
        trackingListener = new OperationTrackingListener(wrappedListener);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor() {
        new OperationTrackingListener(null);
    }

    @Test
    public void testUpdateOperation() {
        try {
            trackingListener.updateOperation(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        trackingListener.updateOperation(SweeperOperation.SIZE_COMPUTATION);
        verify(wrappedListener).updateOperation(SweeperOperation.SIZE_COMPUTATION);

        try {
            trackingListener.updateOperation(SweeperOperation.SIZE_COMPUTATION);
            fail();
        } catch (IllegalStateException e) {
            // Expected because there is an operation in progress, the current operation needs to be completed before
            // starting a new one.
        }
    }

    @Test
    public void testUpdateOperationProgress() {
        try {
            trackingListener.updateOperationProgress(0, 0, 0);
            fail();
        } catch (IllegalStateException e) {
            // expected because no operation is started
        }

        trackingListener.updateOperation(SweeperOperation.SIZE_COMPUTATION);
        trackingListener.setOperationMaxProgress(1);

        try {
            trackingListener.updateOperationProgress(0, 0, 0);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected because the max progress is not consistent with the previously configured max progress.
        }

        try {
            trackingListener.updateOperationProgress(2, 1, 0);
            fail();
        } catch (IllegalArgumentException e) {
            // expected because progress > maxProgress
        }

        try {
            trackingListener.updateOperationProgress(-1, 1, 0);
            fail();
        } catch (IllegalArgumentException e) {
            // expected because progress < 0
        }

        try {
            trackingListener.updateOperationProgress(1, 1, 101);
            fail();
        } catch (IllegalArgumentException e) {
            // expected because percentGlobal > 100
        }

        try {
            trackingListener.updateOperationProgress(1, 1, -1);
            fail();
        } catch (IllegalArgumentException e) {
            // expected because percentGlobal < 0
        }

        trackingListener.updateOperationProgress(1, 1, 100);
        verify(wrappedListener).updateOperationProgress(1, 1, 100);
    }

    @Test
    public void testUpdateTarget() {
        Target target = mock(Target.class);

        try {
            trackingListener.updateTarget(null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        trackingListener.updateOperation(SweeperOperation.RESOURCE_TRAVERSING);
        trackingListener.setOperationMaxProgress(1);
        trackingListener.updateTarget(target);
        verify(wrappedListener).updateTarget(target);
    }

    @Test
    public void testUpdateException() {
        Target target = mock(Target.class);
        SweeperException exception = new SweeperException("");

        try {
            trackingListener.updateException(null, exception);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        try {
            trackingListener.updateException(target, null);
            fail();
        } catch (NullPointerException e) {
            // expected
        }

        trackingListener.updateOperation(SweeperOperation.RESOURCE_TRAVERSING);
        trackingListener.setOperationMaxProgress(1);
        trackingListener.updateException(target, exception);
        verify(wrappedListener).updateException(target, exception);
    }

    @Test
    public void testSetOperationMaxProgress() {
        try {
            trackingListener.setOperationMaxProgress(0);
            fail();
        } catch (IllegalStateException e) {
            // expected because no operation is started
        }

        trackingListener.updateOperation(SweeperOperation.RESOURCE_TRAVERSING);

        try {
            trackingListener.setOperationMaxProgress(-1);
            fail();
        } catch (IllegalArgumentException e) {
            // expected because the condition maxProgress >= 0 is not true
        }

        trackingListener.setOperationMaxProgress(1);
    }

    @Test
    public void testIncrementOperationProgress() {
        trackingListener.updateOperation(SweeperOperation.RESOURCE_TRAVERSING);
        trackingListener.setOperationMaxProgress(100);

        try {
            trackingListener.incrementOperationProgress(110);
            fail();
        } catch (IllegalArgumentException e) {
            // expected because the progress is greater than maxProgress
        }

        trackingListener.incrementOperationProgress(50);
        trackingListener.incrementOperationProgress(50);
        verify(wrappedListener).updateOperationProgress(eq(50L), eq(100L), anyInt());
    }

    @Test
    public void testIncrementTargetProgress() {
        trackingListener.updateOperation(SweeperOperation.RESOURCE_TRAVERSING);
        trackingListener.setOperationMaxProgress(100);
        trackingListener.incrementOperationProgress(50);

        try {
            trackingListener.incrementTargetProgress(51);
            fail();
        } catch (IllegalArgumentException e) {
            // expected because the progress is greater than maxProgress
        }

        trackingListener.incrementTargetProgress(30);
        trackingListener.incrementTargetProgress(20);
        verify(wrappedListener).updateOperationProgress(eq(100L), eq(100L), anyInt());
    }

    @Test
    public void testOperationCompleted() {
        trackingListener.updateOperation(SweeperOperation.RESOURCE_TRAVERSING);
        trackingListener.setOperationMaxProgress(100);

        trackingListener.operationCompleted();
        verify(wrappedListener).updateOperationProgress(eq(100L), eq(100L), anyInt());
    }

}
