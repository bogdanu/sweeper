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
import gg.pistol.sweeper.gui.component.WebBrowserLauncher;
import gg.pistol.sweeper.gui.i18n.I18n;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

/**
 * The main application dialog that will guide the user into cleaning the files/directories.
 *
 * @author Bogdan Pistol
 */
public class Wizard {

    private static final String HELP_URL = "https://github.com/bogdanu/sweeper";

    private final I18n i18n;
    private final WebBrowserLauncher webBrowserLauncher;

    private final BasicDialog window;
    private final About aboutDialog;

    private Wizard() {
        i18n = new I18n();
        webBrowserLauncher = new WebBrowserLauncher(i18n);

        DecoratedPanel panel = new DecoratedPanel(i18n, false, null) {
            @Override
            protected void addComponents(JPanel contentPanel) {
                contentPanel.setLayout(new BorderLayout());
                setTitle(i18n.getString(I18n.WIZARD_TITLE_ID, I18n.APPLICATION_NAME));

                JPanel mainPanel = new JPanel();
                contentPanel.add(mainPanel, BorderLayout.CENTER);

                JPanel southPanel = new JPanel(new BorderLayout());
                contentPanel.add(southPanel, BorderLayout.SOUTH);
                southPanel.add(new JSeparator(), BorderLayout.NORTH);

                JPanel buttons = createHorizontalPanel();
                southPanel.add(buttons, BorderLayout.SOUTH);
                buttons.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));

                buttons.add(createButton(i18n.getString(I18n.WIZARD_BUTTON_HELP_ID), helpAction(), "buttons"));
                buttons.add(Box.createHorizontalStrut(5));
                buttons.add(createButton(i18n.getString(I18n.WIZARD_BUTTON_ABOUT_ID), aboutAction(), "buttons"));
                buttons.add(Box.createHorizontalGlue());
                buttons.add(Box.createHorizontalStrut(40));

                buttons.add(createButton(i18n.getString(I18n.WIZARD_BUTTON_CANCEL_ID), aboutAction(), "buttons"));
                buttons.add(Box.createHorizontalStrut(10));
                buttons.add(createButton(i18n.getString(I18n.WIZARD_BUTTON_BACK_ID), aboutAction(), "buttons"));
                buttons.add(Box.createHorizontalStrut(5));
                buttons.add(createButton(i18n.getString(I18n.WIZARD_BUTTON_NEXT_ID), aboutAction(), "buttons"));
                buttons.add(Box.createHorizontalStrut(10));
                buttons.add(createButton(i18n.getString(I18n.WIZARD_BUTTON_FINISH_ID), aboutAction(), "buttons"));
            }
        };
        window = new BasicDialog(null, panel, false);
        aboutDialog = new About(window, i18n, webBrowserLauncher);

        window.setVisible(true);
    }

    private ActionListener helpAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                webBrowserLauncher.openWebBrowser(HELP_URL, window, I18n.WIZARD_BUTTON_HELP_ID);
            }
        };
    }

    private ActionListener aboutAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                aboutDialog.show();
            }
        };
    }

    public static void open() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Wizard();
            }
        });
    }

}
