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

    @Nullable
    private SweeperOperation operation;

    private long progress;
    private long maxProgress;
    private int percentGlobal;


    OperationTrackingListener(SweeperOperationListener listener) {
        Preconditions.checkNotNull(listener);
        this.listener = listener;
    }

    public void updateOperation(SweeperOperation operation) {
        Preconditions.checkNotNull(operation);

        // Start a new operation only after the previous one completed.
        Preconditions.checkState(this.operation == null);

        this.operation = operation;
        listener.updateOperation(operation);
    }

    public void updateOperationProgress(long progress, long maxProgress, int percentGlobal) {
        // Update only when an operation is started and the max progress for that operation is defined.
        Preconditions.checkState(operation != null && this.maxProgress > 0);

        // Ensure that maxProgress remains the same during operation's progress.
        Preconditions.checkArgument(maxProgress == this.maxProgress);

        Preconditions.checkArgument(progress >= 0 && progress <= maxProgress);
        Preconditions.checkArgument(percentGlobal >= 0 && percentGlobal <= 100);

        listener.updateOperationProgress(progress, maxProgress, percentGlobal);
    }

    public void updateTargetAction(Target target, TargetAction action) {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(action);
        Preconditions.checkState(operation != null && maxProgress > 0);

        listener.updateTargetAction(target, action);
    }

    public void updateTargetException(Target target, TargetAction action, SweeperException e) {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(action);
        Preconditions.checkNotNull(e);
        Preconditions.checkState(operation != null && maxProgress > 0);

        listener.updateTargetException(target, action, e);
    }

    /**
     * Configure the maximum progress that the current operation will reach when completed. This is required before
     * updating the progress for the operation.
     */
    void setOperationMaxProgress(long maxProgress) {
        Preconditions.checkState(operation != null);
        Preconditions.checkArgument(maxProgress > 0);

        this.maxProgress = maxProgress;
    }

    /**
     * Notification that the operation progressed further.
     * <p>
     * The progress of the operation is incremented in absolute values, for example (considering a max progress value
     * of 130):
     * <ol>
     * <li>incrementOperationProgress(40)</li>
     * <li>incrementOperationProgress(110)</li>
     * <li>incrementOperationProgress(130)</li>
     * </ol>
     *
     * @param progress
     *            an absolute value representing the progress
     */
    void incrementOperationProgress(long progress) {
        Preconditions.checkState(operation != null && maxProgress > 0);
        Preconditions.checkArgument(progress >= 0 && progress <= maxProgress);

        if (progress > this.progress) {
            this.progress = progress;
            int percent = getPercentage(operation, this.progress, maxProgress);
            updateOperationProgress(this.progress, maxProgress, percentGlobal + percent);
        }
    }

    /**
     * Notification that a target action progressed further.
     * <p>
     * The progress of the action is incremented in relative values, for example (considering a maximum of 130):
     * <ol>
     * <li>incrementTargetActionProgress(40)</li>
     * <li>incrementTargetActionProgress(70)</li>
     * <li>incrementTargetActionProgress(20)</li>
     * </ol>
     * At the end the progress will be 130 (the sum of all the relative action progress increments).
     *
     * @param actionProgress
     *            a relative value representing the action progress increment
     */
    void incrementTargetActionProgress(long actionProgress) {
        Preconditions.checkState(operation != null && maxProgress > 0);
        long operationProgress = progress + actionProgress;
        Preconditions.checkArgument(operationProgress >= 0 && operationProgress <= maxProgress);

        incrementOperationProgress(operationProgress);
    }

    /**
     * Mark that the operation is done and update the listener with max progress if not already updated.
     */
    void operationCompleted() {
        Preconditions.checkState(operation != null && maxProgress > 0);

        percentGlobal += operation.getPercentQuota();
        if (progress < maxProgress) {
            updateOperationProgress(maxProgress, maxProgress, percentGlobal);
        }
        operation = null;
        progress = 0;
        maxProgress = 0;
    }

    private int getPercentage(SweeperOperation operation, long progress, long maxProgress) {
        return maxProgress == 0 ? 0 : (int) (operation.getPercentQuota() * progress / maxProgress);
    }

}
