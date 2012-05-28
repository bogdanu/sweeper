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

/**
 * Extends SweeperOperationListener to track the operation and phase progress.
 * 
 * @author Bogdan Pistol
 */
class OperationTrackingListener implements SweeperOperationListener {
    
    static final OperationTrackingListener IGNORE_OPERATION_LISTENER = new OperationTrackingListener(new SweeperOperationListener() {
        public void updateOperationProgress(int percent) { }
        public void updateOperationPhase(SweeperOperationPhase phase) { }
        public void updateTargetAction(SweeperTarget target, SweeperTargetAction action) { }
        public void updateTargetException(SweeperTarget target, SweeperTargetAction action, SweeperException e) { }
        public void operationFinished() { }
        public void operationAborted() { }
        });

    private final SweeperOperationListener listener;

    private int phaseProgress;

    private int operationProgress;

    OperationTrackingListener(SweeperOperationListener listener) {
        Preconditions.checkNotNull(listener);
        this.listener = listener;
    }

    public void updateOperationProgress(int percent) {
        listener.updateOperationProgress(percent);
    }

    public void updateOperationPhase(SweeperOperationPhase phase) {
        listener.updateOperationPhase(phase);
    }

    public void updateTargetAction(SweeperTarget target, SweeperTargetAction action) {
        listener.updateTargetAction(target, action);
    }

    public void updateTargetException(SweeperTarget target, SweeperTargetAction action, SweeperException e) {
        listener.updateTargetException(target, action, e);
    }

    public void operationFinished() {
        listener.operationFinished();
    }

    public void operationAborted() {
        listener.operationAborted();
    }

    void incrementPhase(SweeperOperationPhase phase, long progress, long maxProgress) {
        int p = normalizeProgress(phase, progress, maxProgress);
        if (p > phaseProgress) {
            phaseProgress = p;
            updateOperationProgress(operationProgress + phaseProgress);
        }
    }

    void incrementOperation(SweeperOperationPhase phase) {
        operationProgress += phase.getPercentQuota();
        if (phaseProgress < phase.getPercentQuota()) {
            updateOperationProgress(operationProgress);
        }
        phaseProgress = 0;
    }

    private int normalizeProgress(SweeperOperationPhase phase, long progress, long maxProgress) {
        return maxProgress == 0 ? 0 : (int) (phase.getPercentQuota() * progress / maxProgress);
    }

}
