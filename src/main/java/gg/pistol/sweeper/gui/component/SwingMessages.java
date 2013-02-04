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
package gg.pistol.sweeper.gui.component;

import gg.pistol.sweeper.i18n.I18n;
import gg.pistol.sweeper.i18n.LocaleChangeListener;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.google.common.base.Preconditions;

/**
 * This class provides internationalization support for Swing out of the box components like {@link javax.swing.JFileChooser}.
 *
 * @author Bogdan Pistol
 */
public class SwingMessages implements LocaleChangeListener {

    private final I18n i18n;

    public SwingMessages(I18n i18n) {
        Preconditions.checkNotNull(i18n);
        this.i18n = i18n;
        onLocaleChange();
    }

    public void onLocaleChange() {
        /*
         * Ensure that the update() method is called only from the AWT event dispatching thread.
         */
        if (SwingUtilities.isEventDispatchThread()) {
            update();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    update();
                }
            });
        }
    }

    private void update() {
        updateFileChooser();
        UIManager.put("swing.boldMetal", Boolean.FALSE); // switch off MetalLookAndFeel default bold fonts
    }

    private void updateFileChooser() {
        UIManager.put("FileChooser.lookInLabelText", i18n.getString(I18n.FILE_CHOOSER_CURRENT_DIRECTORY_ID));
        UIManager.put("FileChooser.upFolderToolTipText", i18n.getString(I18n.FILE_CHOOSER_UP_ID));
        UIManager.put("FileChooser.homeFolderToolTipText", i18n.getString(I18n.FILE_CHOOSER_HOME_ID));
        UIManager.put("FileChooser.listViewActionLabelText", i18n.getString(I18n.FILE_CHOOSER_SHOW_LIST_ID));
        UIManager.put("FileChooser.listViewButtonToolTipText", i18n.getString(I18n.FILE_CHOOSER_SHOW_LIST_ID));
        UIManager.put("FileChooser.detailsViewActionLabelText", i18n.getString(I18n.FILE_CHOOSER_SHOW_DETAILS_ID));
        UIManager.put("FileChooser.detailsViewButtonToolTipText", i18n.getString(I18n.FILE_CHOOSER_SHOW_DETAILS_ID));
        UIManager.put("FileChooser.fileNameLabelText", i18n.getString(I18n.FILE_CHOOSER_NAME_ID));
        UIManager.put("FileChooser.filesOfTypeLabelText", i18n.getString(I18n.FILE_CHOOSER_FILTER_LABEL_ID));
        UIManager.put("FileChooser.acceptAllFileFilterText", i18n.getString(I18n.FILE_CHOOSER_FILTER_ALL_ID));
        UIManager.put("FileChooser.cancelButtonText", i18n.getString(I18n.BUTTON_CANCEL_ID));
        UIManager.put("FileChooser.cancelButtonToolTipText", i18n.getString(I18n.BUTTON_CANCEL_ID));
        UIManager.put("FileChooser.viewMenuLabelText", i18n.getString(I18n.FILE_CHOOSER_MENU_VIEW_ID));
        UIManager.put("FileChooser.refreshActionLabelText", i18n.getString(I18n.FILE_CHOOSER_MENU_REFRESH_ID));
        UIManager.put("FileChooser.fileNameHeaderText", i18n.getString(I18n.RESOURCE_NAME_ID));
        UIManager.put("FileChooser.fileSizeHeaderText", i18n.getString(I18n.RESOURCE_SIZE_ID));
        UIManager.put("FileChooser.fileDateHeaderText", i18n.getString(I18n.RESOURCE_MODIFIED_ID));
    }

}
