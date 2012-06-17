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
package gg.pistol.sweeper.gui;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Window;
import java.net.URI;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import com.google.common.base.Preconditions;

class WebBrowserLauncher {

    private final I18n i18n;

    WebBrowserLauncher(I18n i18n) {
        Preconditions.checkNotNull(i18n);
        this.i18n = i18n;
    }

    void openWebBrowser(URI uri, Window nobrowserDialogOwner, String nobrowserTitle) {
        Preconditions.checkNotNull(uri);
        Preconditions.checkNotNull(nobrowserDialogOwner);
        Preconditions.checkNotNull(nobrowserTitle);

        boolean nobrowser = !Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
        if (!nobrowser) {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (Exception e) {
                nobrowser = true;
            }
        }
        if (nobrowser) {
            JDialog dialog = new NobrowserDialog(nobrowserDialogOwner, nobrowserTitle,
                    i18n.getString(I18n.WEBBROWSER_NOBROWSER_ID), uri.toString());
            dialog.setVisible(true);
        }
    }

    private class NobrowserDialog extends BorderedDialog {
        private static final long serialVersionUID = 1L;

        private NobrowserDialog(Window owner, String title, String content, String url) {
            super(owner, i18n);
            setTitle(title);
            setModal(true);

            addCloseButton();

            Icon warningIcon = UIManager.getIcon("OptionPane.warningIcon");
            if (warningIcon != null) {
                addSideImage(warningIcon);
            }

            getContentPanel().add(new JLabel(content));
            getContentPanel().add(new JLabel(" "));

            JTextField urlField = new JTextField(url);
            urlField.setEditable(false);
            urlField.setBorder(null);
            urlField.setOpaque(false);
            urlField.setCursor(new Cursor(Cursor.TEXT_CURSOR));
            getContentPanel().add(urlField);

            pack();
            setLocationRelativeTo(owner);
        }
    }

}
