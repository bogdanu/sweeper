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


import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * Wrapper for {@link SweeperOperationListener} that provides tracking of operation progress.
 *
 * @author Bogdan Pistol
 */
// package private
class OperationTrackingListener implements SweeperOperationListener {

    /**
     * No operation listener, useful for cases when listening the operation progress is not wanted.
     */
    static final OperationTrackingListener NOOP_LISTENER = new OperationTrackingListener(new SweeperOperationListener() {
        public void updateOperation(SweeperOperation operation) { /* ignore */ }

        public void updateOperationProgress(long progress, long maxProgress, int percentGlobal) { /* ignore */ }

        public void updateTargetAction(Target target, TargetAction action) { /* ignore */ }

        public void updateTargetException(Target target, TargetAction action, SweeperException e) { /* ignore */ }
    });

    // The wrapped listener.
    private final SweeperOperationListener listener;

    @Nullable private SweeperOperation operation;

    private long progress;
    private long maxProgress = 1; // the smallest operation will be completed in one step
    private int percentGlobal;


    OperationTrackingListener(SweeperOperationListener listener) {
        Preconditions.checkNotNull(listener);
        this.listener = listener;
    }

    private void checkOperation() {
        // Check that there is an operation in progress.
        Preconditions.checkState(operation != null);
    }

    private void checkProgressArgument(long progress) {
        Preconditions.checkArgument(progress >= 0 && progress <= maxProgress);
    }

    public void updateOperation(SweeperOperation operation) {
        Preconditions.checkNotNull(operation);

        // Start a new operation only after the previous one completed.
        Preconditions.checkState(this.operation == null);

        this.operation = operation;
        listener.updateOperation(operation);
    }

    public void updateOperationProgress(long progress, long maxProgress, int percentGlobal) {
        // Update only when an operation is started.
        checkOperation();

        // Ensure that maxProgress remains the same during operation's progress.
        Preconditions.checkArgument(maxProgress == this.maxProgress);

        checkProgressArgument(progress);
        Preconditions.checkArgument(percentGlobal >= 0 && percentGlobal <= 100);

        listener.updateOperationProgress(progress, maxProgress, percentGlobal);
    }

    public void updateTargetAction(Target target, TargetAction action) {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(action);
        checkOperation();

        listener.updateTargetAction(target, action);
    }

    public void updateTargetException(Target target, TargetAction action, SweeperException e) {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(action);
        Preconditions.checkNotNull(e);
        checkOperation();

        listener.updateTargetException(target, action, e);
    }

    /**
     * Configure the maximum progress that the current operation will reach when completed. This is required before
     * updating the progress for the operation.
     */
    void setOperationMaxProgress(long maxProgress) {
        checkOperation();
        Preconditions.checkArgument(maxProgress >= 0);

        if (maxProgress == 0) {
            // The max progress value should be at least 1 (even when the real max progress is zero from
            // the perspective of the caller of this method) because the smallest operation will be completed
            // in one step.
            maxProgress = 1;
        }

        this.maxProgress = maxProgress;
    }

    /**
     * Notification that the operation progressed further.
     *
     * <p>The progress of the operation is incremented in absolute values, for example (considering a max progress value
     * of 130):
     *
     * <ol><li>incrementOperationProgress(40)</li>
     * <li>incrementOperationProgress(110)</li>
     * <li>incrementOperationProgress(130)</li></ol>
     *
     * @param progress
     *         an absolute value representing the progress
     */
    void incrementOperationProgress(long progress) {
        checkOperation();
        checkProgressArgument(progress);

        if (progress > this.progress) {
            this.progress = progress;
            int percent = getPercentage(operation, this.progress, maxProgress);
            updateOperationProgress(this.progress, maxProgress, percentGlobal + percent);
        }
    }

    /**
     * Notification that a target action progressed further.
     *
     * <p>The progress of the action is incremented in relative values, for example (considering a maximum of 130):
     *
     * <ol><li>incrementTargetActionProgress(40)</li>
     * <li>incrementTargetActionProgress(70)</li>
     * <li>incrementTargetActionProgress(20)</li></ol>
     *
     * At the end the progress will be 130 (the sum of all the relative action progress increments).
     *
     * @param actionProgress
     *         a relative value representing the action progress increment
     */
    void incrementTargetActionProgress(long actionProgress) {
        checkOperation();
        long operationProgress = progress + actionProgress;
        checkProgressArgument(operationProgress);

        incrementOperationProgress(operationProgress);
    }

    /**
     * Mark that the operation is done and update the listener with max progress if not already updated.
     */
    void operationCompleted() {
        checkOperation();

        percentGlobal += operation.getPercentQuota();
        if (progress < maxProgress) {
            updateOperationProgress(maxProgress, maxProgress, percentGlobal);
        }
        operation = null;
        progress = 0;
        maxProgress = 1;
    }

    private int getPercentage(SweeperOperation operation, long progress, long maxProgress) {
        return (int) (operation.getPercentQuota() * progress / maxProgress);
    }

}
