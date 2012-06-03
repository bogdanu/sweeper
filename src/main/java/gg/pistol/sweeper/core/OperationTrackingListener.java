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
 * Extends SweeperOperationListener to track operation progress.
 *
 * @author Bogdan Pistol
 */
// package private
class OperationTrackingListener implements SweeperOperationListener {

    static final OperationTrackingListener NOOP_LISTENER = new OperationTrackingListener(new SweeperOperationListener() {
        public void updateOperation(SweeperOperation operation) { }
        public void updateOperationProgress(long operationProgress, long operationSize, int percentGlobalProgress) { }
        public void updateTargetAction(Target target, TargetAction action) { }
        public void updateTargetException(Target target, TargetAction action, SweeperException e) { }
        });

    private final SweeperOperationListener listener;

    @Nullable
    private SweeperOperation operation;

    private long operationProgress = -1;
    private long operationMaxProgress = 0;
    private int percentGlobalProgress;

    OperationTrackingListener(SweeperOperationListener listener) {
        Preconditions.checkNotNull(listener);
        this.listener = listener;
    }

    public void updateOperation(SweeperOperation operation) {
        Preconditions.checkState(this.operation == null);

        this.operation = operation;
        listener.updateOperation(operation);
    }

    public void updateOperationProgress(long operationProgress, long operationMaxProgress, int percentGlobalProgress) {
        Preconditions.checkState(operation != null);
        Preconditions.checkArgument(operationMaxProgress == this.operationMaxProgress);
        Preconditions.checkArgument(operationProgress >= 0 && operationProgress <= operationMaxProgress);
        Preconditions.checkArgument(percentGlobalProgress >= 0 && percentGlobalProgress <= 100);

        listener.updateOperationProgress(operationProgress, operationMaxProgress, percentGlobalProgress);
    }

    public void updateTargetAction(Target target, TargetAction action) {
        Preconditions.checkState(operation != null);
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(action);

        listener.updateTargetAction(target, action);
    }

    public void updateTargetException(Target target, TargetAction action, SweeperException e) {
        Preconditions.checkState(operation != null);
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(action);
        Preconditions.checkNotNull(e);

        listener.updateTargetException(target, action, e);
    }

    void setOperationMaxProgress(long operationMaxProgress) {
        Preconditions.checkState(operation != null);
        Preconditions.checkArgument(operationMaxProgress >= 0);

        this.operationMaxProgress = operationMaxProgress;
    }

    void incrementProgress(long progress) {
        Preconditions.checkState(operation != null);
        Preconditions.checkState(operationMaxProgress >= 0);
        Preconditions.checkArgument(progress >= 0 && progress <= operationMaxProgress);

        if (progress > operationProgress) {
            operationProgress = progress;
            int percent = normalizeProgress(operation, operationProgress, operationMaxProgress);
            updateOperationProgress(operationProgress, operationMaxProgress, percentGlobalProgress + percent);
        }
    }

    void incrementMicroProgress(long microProgress) {
        Preconditions.checkState(operation != null);
        Preconditions.checkState(operationMaxProgress >= 0);
        microProgress += operationProgress;
        Preconditions.checkArgument(microProgress >= 0 && microProgress <= operationMaxProgress);

        incrementProgress(microProgress);
    }

    void operationCompleted() {
        Preconditions.checkState(operation != null);
        Preconditions.checkState(operationMaxProgress >= 0);

        percentGlobalProgress += operation.getPercentQuota();
        if (operationProgress < operationMaxProgress) {
            updateOperationProgress(operationMaxProgress, operationMaxProgress, percentGlobalProgress);
        }
        operationProgress = -1;
        operationMaxProgress = 0;
        operation = null;
    }

    private int normalizeProgress(SweeperOperation operation, long progress, long maxProgress) {
        return maxProgress == 0 ? 0 : (int) (operation.getPercentQuota() * progress / maxProgress);
    }

}
