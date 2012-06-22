/*
 * Sweeper - Duplicate file/folder cleaner
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

import gg.pistol.sweeper.gui.component.BasicDialog;
import gg.pistol.sweeper.gui.component.DecoratedPanel;
import gg.pistol.sweeper.gui.component.DynamicPanel;
import gg.pistol.sweeper.gui.component.WebBrowserLauncher;
import gg.pistol.sweeper.i18n.I18n;

import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.google.common.base.Preconditions;

// package private
class About {

    private static final String WEBSITE_URL = "https://github.com/bogdanu/sweeper";

    private static final String BUTTON_GROUP = "buttons";

    About(@Nullable Window owner, I18n i18n, WebBrowserLauncher webBrowserLauncher) {
        Preconditions.checkNotNull(i18n);
        Preconditions.checkNotNull(webBrowserLauncher);

        BasicDialog dialog = new BasicDialog(owner, createAboutPanel(i18n, webBrowserLauncher));
        dialog.setVisible(true);
    }

    private DynamicPanel createAboutPanel(I18n i18n, final WebBrowserLauncher webBrowserLauncher) {
        return new DecoratedPanel(i18n, false, null) {
            @Override
            protected void addComponents(JPanel contentPanel) {
                setTitle(i18n.getString(I18n.WIZARD_BUTTON_ABOUT_ID));
                contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

                contentPanel.add(alignVertically(new JLabel(I18n.APPLICATION_NAME)));
                contentPanel.add(Box.createVerticalStrut(3));
                contentPanel.add(alignVertically(new JLabel(I18n.COPYRIGHT)));
                contentPanel.add(Box.createVerticalStrut(30));

                JPanel linkPanel = createHorizontalPanel();
                linkPanel.add(new JLabel(i18n.getString(I18n.ABOUT_LABEL_WEBSITE_ID) + ": "));
                linkPanel.add(createLink(WEBSITE_URL, websiteAction(webBrowserLauncher, i18n, parentWindow)));
                contentPanel.add(alignVertically(linkPanel));

                contentPanel.add(Box.createVerticalStrut(50));
                contentPanel.add(Box.createVerticalGlue());

                JPanel buttons = createHorizontalPanel();
                buttons.add(createButton(i18n.getString(I18n.ABOUT_BUTTON_LICENSE_ID), licenseAction(parentWindow, i18n), BUTTON_GROUP));
                buttons.add(Box.createHorizontalGlue());
                buttons.add(createButton(i18n.getString(I18n.BUTTON_CLOSE_ID), closeAction(), BUTTON_GROUP));
                contentPanel.add(alignVertically(buttons));
            }
        };
    }

    private Runnable websiteAction(final WebBrowserLauncher webBrowserLauncher, final I18n i18n, final Window parentWindow) {
        return new Runnable() {
            public void run() {
                webBrowserLauncher.openWebBrowser(WEBSITE_URL, parentWindow, i18n.getString(I18n.ABOUT_LABEL_WEBSITE_ID));
            }
        };
    }

    private ActionListener licenseAction(final Window parentWindow, final I18n i18n) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BasicDialog license = new BasicDialog(parentWindow, createLicensePanel(i18n));
                license.setVisible(true);
            }
        };
    }

    private DynamicPanel createLicensePanel(I18n i18n) {
        return new DecoratedPanel(i18n, true, null) {
            @Override
            protected void addComponents(JPanel contentPanel) {
                setTitle(i18n.getString(I18n.ABOUT_BUTTON_LICENSE_ID));

                JTextArea license = new JTextArea(I18n.LICENSE);
                license.setEditable(false);
                license.setCursor(new Cursor(Cursor.TEXT_CURSOR));
                license.setOpaque(false);

                JScrollPane scroll = new JScrollPane(license);
                scroll.setBorder(null);

                contentPanel.add(scroll);
            }
        };
    }

}
