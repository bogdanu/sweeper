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

import gg.pistol.sweeper.core.Sweeper;
import gg.pistol.sweeper.core.resource.Resource;
import gg.pistol.sweeper.core.resource.ResourceDirectoryFs;
import gg.pistol.sweeper.core.resource.ResourceFileFs;
import gg.pistol.sweeper.i18n.I18n;

import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.google.common.base.Preconditions;

// package private
class TargetSelectionPage extends WizardPage {

    private static final String BUTTON_GROUP = "buttons";

    private final WizardPage previousPage;
    private final DefaultListModel resources;
    @Nullable private File latestOpenedDirectory;

    TargetSelectionPage(WizardPage previousPage, I18n i18n, WizardPageListener listener, Sweeper sweeper) {
        super(Preconditions.checkNotNull(i18n), Preconditions.checkNotNull(listener), Preconditions.checkNotNull(sweeper));
        Preconditions.checkNotNull(previousPage);
        this.previousPage = previousPage;
        resources = new DefaultListModel();
    }

    @Override
    protected void addComponents(JPanel contentPanel) {
        Preconditions.checkNotNull(contentPanel);
        super.addComponents(contentPanel);

        contentPanel.add(alignLeft(new JLabel(i18n.getString(I18n.PAGE_TARGET_SELECTION_INTRODUCTION_ID))));
        contentPanel.add(createVerticalStrut(30));

        JPanel selectionPanel = createHorizontalPanel();
        contentPanel.add(alignLeft(selectionPanel));

        selectionPanel.add(addScrollPane(new JList(resources)));
        selectionPanel.add(createHorizontalStrut(10));

        JPanel buttons = createVerticalPanel();
        selectionPanel.add(buttons);
        buttons.add(createButton(i18n.getString(I18n.BUTTON_ADD_ID), addAction(), BUTTON_GROUP));
        buttons.add(createVerticalStrut(5));
        buttons.add(createButton(i18n.getString(I18n.BUTTON_REMOVE_ID), removeAction(), BUTTON_GROUP));
        buttons.add(Box.createVerticalGlue());

        contentPanel.add(createVerticalStrut(15));
    }

    private ActionListener addAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (latestOpenedDirectory == null) {
                    File[] roots = File.listRoots();
                    if (roots != null && roots.length > 0) {
                        latestOpenedDirectory = roots[0];
                    }
                }
                UIManager.put("FileChooser.readOnly", Boolean.TRUE);

                JFileChooser opener = latestOpenedDirectory == null ? new JFileChooser() :
                    new JFileChooser(latestOpenedDirectory);

                opener.setComponentOrientation(ComponentOrientation.getOrientation(i18n.getLocale()));
                opener.setFileHidingEnabled(false);
                opener.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                opener.setMultiSelectionEnabled(true);

                opener.setDialogTitle(i18n.getString(I18n.PAGE_TARGET_SELECTION_FILE_CHOOSER_TITLE_ID));
                opener.setApproveButtonText(i18n.getString(I18n.BUTTON_ADD_ID));
                opener.setApproveButtonToolTipText(i18n.getString(I18n.BUTTON_ADD_ID));

                int ret = opener.showOpenDialog(listener.getWindow());
                if (ret != JFileChooser.APPROVE_OPTION) {
                    return;
                }
                latestOpenedDirectory = opener.getCurrentDirectory();
                File[] files = opener.getSelectedFiles();
                if (files != null) {
                    addResources(files);
                }
            }
        };
    }

    private void addResources(File[] files) {
        for (File file : files) {
            Resource resource = null;
            if (file.isDirectory()) {
                try {
                    resource = new ResourceDirectoryFs(file);
                } catch (IOException e) {
                }
            } else if (file.isFile()) {
                try {
                    resource = new ResourceFileFs(file);
                } catch (IOException e) {
                }
            }
            if (resource != null) {
                resources.addElement(resource);
            }
        }
    }

    private ActionListener removeAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        };
    }

    @Override
    protected String getPageHeader() {
        return i18n.getString(I18n.PAGE_TARGET_SELECTION_HEADER_ID);
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
    WizardPage finish() {
        return null;
    }

}
