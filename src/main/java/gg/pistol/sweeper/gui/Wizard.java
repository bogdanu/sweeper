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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

public class Wizard {

    private static final String HELP_URL = "https://github.com/bogdanu/sweeper";

    private final I18n i18n;
    private final WebBrowserLauncher webBrowserLauncher;

    private final BorderedDialog window;
    private final About aboutDialog;

    private Wizard(Locale locale) {
        i18n = new I18n(locale);
        webBrowserLauncher = new WebBrowserLauncher(i18n);

        window = new BorderedDialog(null, i18n);
        window.setTitle(i18n.getString(I18n.WIZARD_TITLE_ID, I18n.APPLICATION_NAME));
        aboutDialog = new About(window, i18n, webBrowserLauncher);

        initComponents();

        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel();
        JPanel southPanel = new JPanel(new BorderLayout());
        window.getContentPanel().setLayout(new BorderLayout());
        window.getContentPanel().add(mainPanel, BorderLayout.CENTER);
        window.getContentPanel().add(southPanel, BorderLayout.SOUTH);

        JPanel buttons = new JPanel();
        southPanel.add(new JSeparator(), BorderLayout.NORTH);
        southPanel.add(buttons, BorderLayout.SOUTH);

        window.setBoxLayout(buttons, BoxLayout.LINE_AXIS);
        buttons.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));

        JButton help = new JButton(i18n.getString(I18n.WIZARD_BUTTON_HELP_ID));
        JButton about = new JButton(i18n.getString(I18n.WIZARD_BUTTON_ABOUT_ID));
        help.addActionListener(helpAction());
        about.addActionListener(aboutAction());
        buttons.add(help);
        buttons.add(Box.createHorizontalStrut(5));
        buttons.add(about);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(Box.createHorizontalStrut(40));

        JButton cancel = new JButton(i18n.getString(I18n.WIZARD_BUTTON_CANCEL_ID));
        JButton back = new JButton(i18n.getString(I18n.WIZARD_BUTTON_BACK_ID));
        JButton next = new JButton(i18n.getString(I18n.WIZARD_BUTTON_NEXT_ID));
        JButton finish = new JButton(i18n.getString(I18n.WIZARD_BUTTON_FINISH_ID));
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(back);
        buttons.add(Box.createHorizontalStrut(5));
        buttons.add(next);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(finish);
    }

    private ActionListener helpAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    webBrowserLauncher.openWebBrowser(new URI(HELP_URL), window,
                            i18n.getString(I18n.WIZARD_BUTTON_HELP_ID));
                } catch (URISyntaxException ex) {
                    // should never happen
                    new RuntimeException(ex);
                }
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
//                new Wizard(new Locale("zh", "CN"));
//                new Wizard(new Locale("ar", "SA"));
                new Wizard(new Locale("ro", "RO"));
//                new Wizard(Locale.getDefault());
            }
        });
    }

}
