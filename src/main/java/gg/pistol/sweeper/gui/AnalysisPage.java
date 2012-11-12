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
package gg.pistol.sweeper.gui;

import com.google.common.base.Preconditions;
import gg.pistol.lumberjack.JackLogger;
import gg.pistol.lumberjack.JackLoggerFactory;
import gg.pistol.sweeper.core.Sweeper;
import gg.pistol.sweeper.core.SweeperAbortException;
import gg.pistol.sweeper.core.SweeperException;
import gg.pistol.sweeper.core.SweeperOperation;
import gg.pistol.sweeper.core.SweeperOperationListener;
import gg.pistol.sweeper.core.Target;
import gg.pistol.sweeper.core.TargetAction;
import gg.pistol.sweeper.core.resource.Resource;
import gg.pistol.sweeper.i18n.I18n;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

// package private
class AnalysisPage extends WizardPage {

    public static final int PROGRESS_UPDATE_FREQUENCY = 40; // millis

    private final JackLogger log;
    private final WizardPage previousPage;

    private final Collection<? extends Resource> resources;
    private final SweeperOperationListener operationListener;

    private final long startTime;
    private volatile int totalProgressPercent;
    private SweeperOperation operation;
    private volatile long operationProgress;
    private volatile long operationMaxProgress;
    private volatile Target currentTarget;
    private AtomicLong errors = new AtomicLong();

    private ExecutorService executor;
    private boolean analysisStarted;
    private volatile boolean analysisDone;

    @Nullable private JProgressBar totalProgressBar;
    @Nullable private JLabel totalTime;
    @Nullable private JLabel totalRemainingTime;
    @Nullable private JLabel operationDescription;
    @Nullable private JProgressBar operationProgressBar;
    @Nullable private JLabel operationTargetLabel;
    @Nullable private JLabel operationTarget;
    @Nullable private JLabel errorCounter;

    AnalysisPage(WizardPage previousPage, I18n i18n, WizardPageListener listener, Sweeper sweeper, Collection<? extends Resource> resources) {
        super(Preconditions.checkNotNull(i18n), Preconditions.checkNotNull(listener), Preconditions.checkNotNull(sweeper));
        Preconditions.checkNotNull(previousPage);

        log = JackLoggerFactory.getLogger(LoggerFactory.getLogger(ResourceSelectionPage.class));
        this.previousPage = previousPage;
        this.resources = resources;
        operationListener = getOperationListener();

        startTime = System.currentTimeMillis();

        executor = Executors.newFixedThreadPool(2);
        operation = SweeperOperation.RESOURCE_TRAVERSING;
    }

    @Override
    protected void addComponents(JPanel contentPanel) {
        Preconditions.checkNotNull(contentPanel);
        super.addComponents(contentPanel);
        contentPanel.add(alignLeft(new JLabel(i18n.getString(I18n.PAGE_ANALYSIS_INTRODUCTION_ID))));
        contentPanel.add(createVerticalStrut(15));

        JPanel grid = new JPanel();
        grid.setComponentOrientation(ComponentOrientation.getOrientation(i18n.getLocale()));
        grid.setLayout(new GridBagLayout());
        contentPanel.add(alignLeft(grid));

        addGridComponent(grid, new JLabel(i18n.getString(I18n.PAGE_ANALYSIS_TOTAL_PROGRESS_ID)), false, true);
        totalProgressBar = createProgressBar();
        addGridComponent(grid, totalProgressBar, true, true);

        addGridComponent(grid, new JLabel(i18n.getString(I18n.PAGE_ANALYSIS_TOTAL_ELAPSED_TIME_ID)), false, false);
        totalTime = new JLabel(formatTime(getElapsedTime()));
        addGridComponent(grid, totalTime, true, false);

        addGridComponent(grid, new JLabel(i18n.getString(I18n.PAGE_ANALYSIS_TOTAL_REMAINING_TIME_ID)), false, false);
        totalRemainingTime = new JLabel();
        addGridComponent(grid, totalRemainingTime, true, false);

        addGridComponent(grid, new JLabel(i18n.getString(I18n.PAGE_ANALYSIS_OPERATION_LABEL_ID)), false, true);
        operationDescription = new JLabel(getOperationDescription(operation));
        addGridComponent(grid, operationDescription, true, true);

        addGridComponent(grid, new JLabel(i18n.getString(I18n.PAGE_ANALYSIS_OPERATION_PROGRESS_ID)), false, false);
        operationProgressBar = createProgressBar();
        addGridComponent(grid, operationProgressBar, true, false);

        operationTargetLabel = new JLabel(i18n.getString(I18n.PAGE_ANALYSIS_OPERATION_TARGET_LABEL_ID));
        addGridComponent(grid, operationTargetLabel, false, true);
        operationTarget = new JLabel();
        addGridComponent(grid, operationTarget, true, true);
        setOperationTargetVisibility(false);

        addGridComponent(grid, new JLabel(i18n.getString(I18n.PAGE_ANALYSIS_ERROR_COUNTER_ID)), false, true);
        errorCounter = new JLabel(Long.toString(errors.get()));
        addGridComponent(grid, errorCounter, true, true);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weighty = 1;
        grid.add(Box.createVerticalGlue(), constraints);

        if (!analysisStarted) {
            analysisStarted = true;
            executor.submit(getRunnableAnalysis());
            executor.submit(getRunnableProgress());
        }
    }

    private SweeperOperationListener getOperationListener() {
        return new SweeperOperationListener() {
            @Override
            public void updateOperation(final SweeperOperation currentOperation) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateTimes();
                        operation = currentOperation;
                        operationDescription.setText(getOperationDescription(operation));
                    }
                });
            }

            @Override
            public void updateOperationProgress(long progress, long maxProgress, int percentGlobal) {
                operationProgress = progress;
                operationMaxProgress = maxProgress;
                totalProgressPercent = percentGlobal;
                if (progress == 0 || progress == maxProgress) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateProgress();
                        }
                    });
                }
            }

            @Override
            public void updateTargetAction(Target target, TargetAction action) {
                currentTarget = target;
            }

            @Override
            public void updateTargetException(Target target, TargetAction action, SweeperException e) {
                errors.incrementAndGet();
            }
        };
    }

    private void updateProgress() {
        updateTimes();
        totalProgressBar.setValue(totalProgressPercent);
        if (operationMaxProgress > 0) {
            operationProgressBar.setValue((int) (100 * operationProgress / operationMaxProgress));
        }
        errorCounter.setText(Long.toString(errors.get()));
        if ((operation == SweeperOperation.RESOURCE_TRAVERSING || operation == SweeperOperation.SIZE_COMPUTATION ||
                operation == SweeperOperation.HASH_COMPUTATION || operation == SweeperOperation.RESOURCE_DELETION) && currentTarget != null) {
            setOperationTargetVisibility(true);
            operationTarget.setText(currentTarget.getName());
        }
    }

    private void updateTimes() {
        totalTime.setText(formatTime(getElapsedTime()));
        if (totalProgressPercent > 0) {
            totalRemainingTime.setText(formatTime(getRemainingTime()));
        }
    }

    private Runnable getRunnableProgress() {
        return new Runnable() {
            @Override
            public void run() {
                while (!analysisDone) {
                    try {
                        Thread.sleep(PROGRESS_UPDATE_FREQUENCY);
                    } catch (InterruptedException e) {
                        break;
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateProgress();
                        }
                    });
                }
                executor.shutdown();
            }
        };
    }

    private Runnable getRunnableAnalysis() {
        return new Runnable() {
            @Override
            public void run() {
                WindowListener windowListener = new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        sweeper.abortAnalysis();
                    }
                };
                getParentWindow().addWindowListener(windowListener);

                try {
                    sweeper.analyze(resources, operationListener);
                } catch (SweeperAbortException e) {
                    // ignore
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    analysisDone = true;
                    getParentWindow().removeWindowListener(windowListener);
                }
            }
        };
    }

    private long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    private long getRemainingTime() {
        return (100 - totalProgressPercent) * getElapsedTime() / totalProgressPercent;
    }

    private String getOperationDescription(SweeperOperation operation) {
        switch (operation) {
            case RESOURCE_TRAVERSING:
                return i18n.getString(I18n.PAGE_ANALYSIS_OPERATION_RESOURCE_TRAVERSAL_ID);
            case SIZE_COMPUTATION:
                return i18n.getString(I18n.PAGE_ANALYSIS_OPERATION_SIZE_COMPUTATION_ID);
            case SIZE_DEDUPLICATION:
                return i18n.getString(I18n.PAGE_ANALYSIS_OPERATION_SIZE_DEDUPLICATION_ID);
            case HASH_COMPUTATION:
                return i18n.getString(I18n.PAGE_ANALYSIS_OPERATION_HASH_COMPUTATION_ID);
            case HASH_DEDUPLICATION:
                return i18n.getString(I18n.PAGE_ANALYSIS_OPERATION_HASH_DEDUPLICATION_ID);
            case COUNTING:
                return i18n.getString(I18n.PAGE_ANALYSIS_OPERATION_COUNTING_ID);
            case DUPLICATE_GROUPING:
                return i18n.getString(I18n.PAGE_ANALYSIS_OPERATION_DUPLICATE_GROUPING_ID);
            case RESOURCE_DELETION:
                return i18n.getString(I18n.PAGE_ANALYSIS_OPERATION_RESOURCE_DELETION_ID);
        }
        return null;
    }

    private String formatTime(long time) {
        int hours = (int) (time / 3600000);
        time %= 3600000;
        int minutes = (int) (time / 60000);
        time %= 60000;
        int seconds = (int) (time / 1000);
        if (hours > 0) {
            return i18n.getString(I18n.TIME_DESCRIPTION_HOURS_ID, Integer.toString(hours), Integer.toString(minutes), Integer.toString(seconds));
        } else if (minutes > 0) {
            return i18n.getString(I18n.TIME_DESCRIPTION_MINUTES_ID, Integer.toString(minutes), Integer.toString(seconds));
        } else {
            return i18n.getString(I18n.TIME_DESCRIPTION_SECONDS_ID, Integer.toString(seconds));
        }
    }

    private void setOperationTargetVisibility(boolean visible) {
        operationTargetLabel.setVisible(visible);
        operationTarget.setVisible(visible);
    }

    private void addGridComponent(JPanel panel, JComponent component, boolean lastRowCell, boolean newGridSection) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.LINE_START;
        int topInset = newGridSection ? 15 : 7;
        int horizontalInset = 3;
        if (lastRowCell) {
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.insets = new Insets(topInset, horizontalInset, 0, 0);
        } else {
            constraints.gridwidth = GridBagConstraints.RELATIVE;
            constraints.insets = new Insets(topInset, 0, 0, horizontalInset);
        }
        panel.add(alignLeft(component), constraints);
    }

    private JProgressBar createProgressBar() {
        JProgressBar bar = new JProgressBar();
        bar.setComponentOrientation(ComponentOrientation.getOrientation(i18n.getLocale()));
        bar.setStringPainted(true);
        return bar;
    }

    @Override
    protected String getPageHeader() {
        return i18n.getString(I18n.PAGE_ANALYSIS_HEADER_ID);
    }

    @Override
    boolean isCancelButtonVisible() {
        return false;
    }

    @Override
    boolean isCancelButtonEnabled() {
        return false;
    }

    @Override
    boolean isBackButtonEnabled() {
        return true;
    }

    @Override
    boolean isNextButtonEnabled() {
        return false;
    }

    @Override
    boolean isFinishButtonEnabled() {
        return false;
    }

    @Override
    boolean isLastPage() {
        return false;
    }

    @Override
    boolean isLanguageSelectorVisible() {
        return true;
    }

    @Override
    void cancel() {
    }

    @Override
    WizardPage back() {
        return previousPage;
    }

    @Override
    WizardPage next() {
        return null;
    }

    @Override
    @Nullable
    WizardPage finish() {
        return null;
    }

}
