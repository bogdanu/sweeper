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
import gg.pistol.sweeper.core.Sweeper;
import gg.pistol.sweeper.core.SweeperCount;
import gg.pistol.sweeper.core.SweeperPoll;
import gg.pistol.sweeper.core.Target;
import gg.pistol.sweeper.gui.component.ConfirmationDialog;
import gg.pistol.sweeper.i18n.I18n;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

// package private
class PollPage extends WizardPage {

    private final WizardPage analysisPage;

    private final List<Target> targets;

    @Nullable private JLabel statDelete;
    @Nullable private AbstractTableModel tableModel;

    PollPage(WizardPage analysisPage, I18n i18n, WizardPageListener listener, Sweeper sweeper) {
        super(Preconditions.checkNotNull(i18n), Preconditions.checkNotNull(listener), Preconditions.checkNotNull(sweeper));
        Preconditions.checkNotNull(analysisPage);

        this.analysisPage = analysisPage;
        targets = new ArrayList<Target>(sweeper.getCurrentPoll().getTargets());
    }

    @Override
    protected void addComponents(JPanel contentPanel) {
        Preconditions.checkNotNull(contentPanel);
        super.addComponents(contentPanel);

        SweeperCount count = sweeper.getCount();

        contentPanel.add(alignLeft(createWordWrappingLabel(i18n.getString(I18n.PAGE_POLL_DESCRIPTION_ID,
                formatInt(count.getTotalTargets()), formatSize(count.getTotalSize()),
                formatInt(count.getDuplicateTargets()), formatSize(count.getDuplicateSize())))));
        contentPanel.add(createVerticalStrut(20));

        JPanel topTablePanel = createHorizontalPanel();
        JPanel topLinkPanel = createVerticalPanel();
        topLinkPanel.add(createLink(i18n.getString(I18n.PAGE_POLL_LINK_DECIDE_LATER_ALL_ID), markAllAction(SweeperPoll.Mark.DECIDE_LATER)));
        topLinkPanel.add(createVerticalStrut(3));
        topLinkPanel.add(createLink(i18n.getString(I18n.PAGE_POLL_LINK_RETAIN_ALL), markAllAction(SweeperPoll.Mark.RETAIN)));
        topLinkPanel.add(createVerticalStrut(3));
        topLinkPanel.add(createLink(i18n.getString(I18n.PAGE_POLL_LINK_DELETE_ALL_ID), markAllAction(SweeperPoll.Mark.DELETE)));
        topTablePanel.add(topLinkPanel);
        topTablePanel.add(Box.createHorizontalGlue());
        topTablePanel.add(new JLabel(i18n.getString(I18n.PAGE_POLL_NUMBER_ID, Integer.toString(sweeper.getCurrentPoll().getNumber()))));
        contentPanel.add(alignLeft(topTablePanel));
        contentPanel.add(createVerticalStrut(5));

        contentPanel.add(alignLeft(addScrollPane(createTable())));
        contentPanel.add(createVerticalStrut(5));

        statDelete = createWordWrappingLabel(i18n.getString(I18n.PAGE_POLL_STAT_DELETE_ID,
                formatInt(count.getToDeleteTargets()), formatSize(count.getToDeleteSize())));
        contentPanel.add(alignLeft(statDelete));
        contentPanel.add(createVerticalStrut(15));
    }

    private Runnable markAllAction(final SweeperPoll.Mark mark) {
        return new Runnable() {
            @Override
            public void run() {
                for (Target t : targets) {
                    sweeper.getCurrentPoll().mark(t, mark);
                }

                /*
                 * The statistics about targets are computed when advancing to the next poll, in this way the Sweeper
                 * implementation knows that the user made a decision for all the targets in the current poll.
                 * But we would like to provide real time statistics, whenever the user marks a target the statistics
                 * should change to reflect that and not wait until the user marked all the targets and advanced to
                 * the next poll. To simulate this we advance to the next poll and back and we have access to
                 * the statistics while the user's perception about the current poll is not changed.
                 */
                sweeper.nextPoll();
                sweeper.previousPoll();

                statDelete.setText(i18n.getString(I18n.PAGE_POLL_STAT_DELETE_ID,
                        formatInt(sweeper.getCount().getToDeleteTargets()), formatSize(sweeper.getCount().getToDeleteSize())));
                tableModel.fireTableDataChanged();
            }
        };
    }

    private JTable createTable() {
        tableModel = createTableModel();
        JTable table = new JTable(tableModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                String tooltip = null;
                Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                int realColumnIndex = convertColumnIndexToModel(colIndex);

                if (realColumnIndex == 3) {
                    tooltip = getValueAt(rowIndex, colIndex).toString();
                }
                return tooltip;
            }
        };
        table.getTableHeader().setReorderingAllowed(false);

        final TableCellRenderer booleanRenderer = table.getDefaultRenderer(Boolean.class);
        final TableCellRenderer stringRenderer = table.getDefaultRenderer(String.class);
        table.setDefaultRenderer(Boolean.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return booleanRenderer.getTableCellRendererComponent(table, value, false, false, row, column);
            }
        });
        table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return stringRenderer.getTableCellRendererComponent(table, value, false, false, row, column);
            }
        });

        for (int column = 0; column < table.getColumnCount(); column++) {
            packColumn(table, column, column == 3);
        }
        table.setComponentOrientation(ComponentOrientation.getOrientation(i18n.getLocale()));
        return table;
    }

    private void packColumn(JTable table, int column, boolean autoResize) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);

        TableCellRenderer renderer = table.getTableHeader().getDefaultRenderer();
        Component rendererComponent = renderer.getTableCellRendererComponent(
                table, tableColumn.getHeaderValue(), false, false, -1, column);
        int width = rendererComponent.getPreferredSize().width;

        for (int row = 0; row < table.getRowCount(); row++) {
            renderer = table.getCellRenderer(row, column);
            rendererComponent = renderer.getTableCellRendererComponent(
                    table, table.getValueAt(row, column), false, false, row, column);
            width = Math.max(width, rendererComponent.getPreferredSize().width);
        }

        width += 6;
        table.getColumnModel().getColumn(column).setPreferredWidth(width);
        if (!autoResize) {
            table.getColumnModel().getColumn(column).setMinWidth(width);
            table.getColumnModel().getColumn(column).setMaxWidth(width);
        }
    }

    private AbstractTableModel createTableModel() {
        return new AbstractTableModel() {

            @Override
            public int getRowCount() {
                return targets.size();
            }

            @Override
            public int getColumnCount() {
                return 7;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                switch (columnIndex)
                {
                    case 0:
                        return sweeper.getCurrentPoll().getMark(targets.get(rowIndex)) == SweeperPoll.Mark.DECIDE_LATER;
                    case 1:
                        return sweeper.getCurrentPoll().getMark(targets.get(rowIndex)) == SweeperPoll.Mark.RETAIN;
                    case 2:
                        return sweeper.getCurrentPoll().getMark(targets.get(rowIndex)) == SweeperPoll.Mark.DELETE;
                    case 3:
                        return targets.get(rowIndex).getName();
                    case 4:
                        if (targets.get(rowIndex).getType() == Target.Type.FILE) {
                            return i18n.getString(I18n.RESOURCE_TYPE_FILE_ID);
                        } else {
                            return i18n.getString(I18n.RESOURCE_TYPE_DIRECTORY_ID);
                        }
                    case 5:
                        return formatSize(targets.get(rowIndex).getSize());
                    case 6:
                        DateTime date = targets.get(rowIndex).getModificationDate();
                        return date == null ? i18n.getString(I18n.PAGE_POLL_TABLE_COLUMN_DATE_UNKNOWN_ID) :
                                date.toString(DateTimeFormat.patternForStyle("MM", i18n.getLocale()));
                }
                return null;
            }

            @Override
            public String getColumnName(int column) {
                switch (column)
                {
                    case 0:
                        return i18n.getString(I18n.PAGE_POLL_TABLE_COLUMN_DECIDE_LATER_ID);
                    case 1:
                        return i18n.getString(I18n.PAGE_POLL_TABLE_COLUMN_RETAIN_ID);
                    case 2:
                        return i18n.getString(I18n.PAGE_POLL_TABLE_COLUMN_DELETE_ID);
                    case 3:
                        return i18n.getString(I18n.RESOURCE_NAME_ID);
                    case 4:
                        return i18n.getString(I18n.PAGE_POLL_TABLE_COLUMN_TYPE_ID);
                    case 5:
                        return i18n.getString(I18n.RESOURCE_SIZE_ID);
                    case 6:
                        return i18n.getString(I18n.RESOURCE_MODIFIED_ID);
                }
                return null;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex < 3 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex < 3;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                SweeperPoll.Mark mark = null;
                switch (columnIndex) {
                    case 0:
                        mark = SweeperPoll.Mark.DECIDE_LATER;
                        break;
                    case 1:
                        mark = SweeperPoll.Mark.RETAIN;
                        break;
                    case 2:
                        mark = SweeperPoll.Mark.DELETE;
                        break;
                }
                sweeper.getCurrentPoll().mark(targets.get(rowIndex), mark);
                sweeper.nextPoll();
                sweeper.previousPoll();

                statDelete.setText(i18n.getString(I18n.PAGE_POLL_STAT_DELETE_ID,
                        formatInt(sweeper.getCount().getToDeleteTargets()), formatSize(sweeper.getCount().getToDeleteSize())));
                fireTableRowsUpdated(rowIndex, rowIndex);
            }
        };
    }

    private String formatSize(long size) {
        int gb = (int) (size >> 30);
        int mb = (int) (size >> 20 & 0x3FF);
        int kb = (int) (size >> 10 & 0x3FF);
        int bytes = (int) (size & 0x3FF);

        if (gb > 0) {
            return i18n.getString(I18n.SIZE_DESCRIPTION_GB_ID, formatInt(gb), formatInt(mb));
        }
        if (mb > 0) {
            return i18n.getString(I18n.SIZE_DESCRIPTION_MB_ID, formatInt(mb));
        }
        if (kb > 0) {
            return i18n.getString(I18n.SIZE_DESCRIPTION_KB_ID, formatInt(kb));
        }
        return i18n.getString(I18n.SIZE_DESCRIPTION_BYTE_ID, formatInt(bytes));
    }

    private String formatInt(int val) {
        return String.format(i18n.getLocale(), "%1$,d", val);
    }

    @Override
    protected String getPageHeader() {
        return i18n.getString(I18n.PAGE_POLL_HEADER_ID);
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
        return true;
    }

    @Override
    boolean isFinishButtonEnabled() {
        return true;
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
    @Nullable
    WizardPage back() {
        if (sweeper.previousPoll() == null) {
            if (new ConfirmationDialog(getParentWindow(), i18n, i18n.getString(I18n.LABEL_CONFIRMATION_ID),
                    i18n.getString(I18n.PAGE_POLL_BACK_CONFIRMATION_MESSAGE_ID)).isConfirmed()) {
                setParentWindow(null);
                return analysisPage;
            } else {
                return null;
            }
        }

        PollPage page = new PollPage(analysisPage, i18n, listener, sweeper);
        page.setParentWindow(getParentWindow());
        setParentWindow(null);
        return page;
    }

    @Override
    WizardPage next() {
        WizardPage page;
        if (sweeper.nextPoll() != null) {
            page = new PollPage(analysisPage, i18n, listener, sweeper);
            page.setParentWindow(getParentWindow());
            setParentWindow(null);
        } else {
            page = null;
//            setParentWindow(null);
        }
        return page;
    }

    @Override
    WizardPage finish() {
        return null;
    }

}
