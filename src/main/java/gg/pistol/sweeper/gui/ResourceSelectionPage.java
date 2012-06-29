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

import gg.pistol.lumberjack.JackLogger;
import gg.pistol.lumberjack.JackLoggerFactory;
import gg.pistol.sweeper.core.Sweeper;
import gg.pistol.sweeper.core.resource.Resource;
import gg.pistol.sweeper.core.resource.ResourceDirectoryFs;
import gg.pistol.sweeper.core.resource.ResourceFileFs;
import gg.pistol.sweeper.gui.component.MessageDialog;
import gg.pistol.sweeper.gui.component.MessageDialog.MessageType;
import gg.pistol.sweeper.i18n.I18n;

import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

// package private
class ResourceSelectionPage extends WizardPage {

    private static final String BUTTON_GROUP = "buttons";

    private final JackLogger log;

    private final WizardPage previousPage;
    private final DefaultListModel resources;
    @Nullable private JList resourceList;
    @Nullable private File latestOpenedDirectory;

    ResourceSelectionPage(WizardPage previousPage, I18n i18n, WizardPageListener listener, Sweeper sweeper) {
        super(Preconditions.checkNotNull(i18n), Preconditions.checkNotNull(listener), Preconditions.checkNotNull(sweeper));
        Preconditions.checkNotNull(previousPage);

        log = JackLoggerFactory.getLogger(LoggerFactory.getLogger(ResourceSelectionPage.class));
        this.previousPage = previousPage;
        resources = new DefaultListModel();
    }

    @Override
    protected void addComponents(JPanel contentPanel) {
        Preconditions.checkNotNull(contentPanel);
        super.addComponents(contentPanel);

        contentPanel.add(alignLeft(new JLabel(i18n.getString(I18n.PAGE_RESOURCE_SELECTION_INTRODUCTION_ID))));
        contentPanel.add(createVerticalStrut(30));

        JPanel selectionPanel = createHorizontalPanel();
        contentPanel.add(alignLeft(selectionPanel));

        selectionPanel.add(addScrollPane(resourceList = new JList(resources)));
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

                opener.setDialogTitle(i18n.getString(I18n.PAGE_RESOURCE_SELECTION_FILE_CHOOSER_TITLE_ID));
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
        List<File> erroneousFiles = new ArrayList<File>();
        Resource latestResource = null;

        for (File file : files) {
            Resource resource = null;
            try {
                if (file.isDirectory()) {
                    resource = new ResourceDirectoryFs(file);
                } else if (file.isFile()) {
                    resource = new ResourceFileFs(file);
                } else {
                    log.warn("Ignoring <{}> it is not a file or directory.", file);
                }
            } catch (Exception e) {
                log.error("Exception while reading <" + file + ">.", e);
                erroneousFiles.add(file);
            }
            if (resource == null) {
                continue;
            }

            latestResource = resource;
            if (!resources.contains(resource)) {
                log.info("Adding <{}> to the selected resources.", resource);
                resources.addElement(resource);
            } else {
                log.warn("The resource <{}> is already contained in the selected resources, ignoring it.", resource);
            }
        }

        if (!erroneousFiles.isEmpty()) {
            String selectableMessage = Joiner.on(", ").join(erroneousFiles);
            new MessageDialog(listener.getWindow(), MessageType.ERROR, i18n, i18n.getString(I18n.LABEL_ERROR_ID),
                    i18n.getString(I18n.PAGE_RESOURCE_SELECTION_FILE_CHOOSER_RESOURCE_ERROR_ID), selectableMessage);
        }

        if (latestResource != null) {
            resourceList.setSelectedValue(latestResource, true);
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
        return i18n.getString(I18n.PAGE_RESOURCE_SELECTION_HEADER_ID);
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
