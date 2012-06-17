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

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.google.common.base.Preconditions;

class About {

    private static final String WEBSITE_URL = "https://github.com/bogdanu/sweeper";

    private final Window owner;
    private final BorderedDialog window;
    private final LicenseDialog licenseDialog;

    private final I18n i18n;
    private final WebBrowserLauncher webBrowserLauncher;

    About(@Nullable Window owner, I18n i18n, WebBrowserLauncher webBrowserLauncher) {
        Preconditions.checkNotNull(i18n);
        Preconditions.checkNotNull(webBrowserLauncher);
        this.owner = owner;
        this.i18n = i18n;
        this.webBrowserLauncher = webBrowserLauncher;
        licenseDialog = new LicenseDialog();

        window = new BorderedDialog(owner, i18n);
        window.setTitle(i18n.getString(I18n.WIZARD_BUTTON_ABOUT_ID));
        window.setModal(true);
        initComponents();
    }

    private void initComponents() {
        JPanel contentPanel = window.getContentPanel();
        window.setBoxLayout(contentPanel, BoxLayout.PAGE_AXIS);

        window.addAlignedComponent(contentPanel, new JLabel(I18n.APPLICATION_NAME));
        contentPanel.add(Box.createVerticalStrut(3));

        window.addAlignedComponent(contentPanel, new JLabel("Copyright (C) 2012 Bogdan Pistol"));
        contentPanel.add(Box.createVerticalStrut(30));

        JPanel site = new JPanel();
        window.setBoxLayout(site, BoxLayout.LINE_AXIS);
        site.add(new JLabel(i18n.getString(I18n.ABOUT_LABEL_WEBSITE_ID)));
        site.add(new JLabel(": "));
        JLabel url = new JLabel("<html><a href=''>" + WEBSITE_URL + "</a></html>");
        url.addMouseListener(websiteAction());
        url.setCursor(new Cursor(Cursor.HAND_CURSOR));
        url.setHorizontalAlignment(site.getComponentOrientation().isLeftToRight() ? SwingConstants.LEFT : SwingConstants.RIGHT);
        site.add(url);

        window.addAlignedComponent(contentPanel, site);
        contentPanel.add(Box.createVerticalStrut(50));
        contentPanel.add(Box.createVerticalGlue());

        JPanel buttons = new JPanel();
        window.setBoxLayout(buttons, BoxLayout.LINE_AXIS);
        window.addAlignedComponent(contentPanel, buttons);
        JButton license = new JButton(i18n.getString(I18n.ABOUT_BUTTON_LICENSE_ID));
        JButton close = new JButton(i18n.getString(I18n.BUTTON_CLOSE_ID));
        buttons.add(license);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(close);
        license.addActionListener(licenseAction());
        close.addActionListener(closeAction());
    }

    private MouseListener websiteAction() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    webBrowserLauncher.openWebBrowser(new URI(WEBSITE_URL), window,
                            i18n.getString(I18n.ABOUT_LABEL_WEBSITE_ID));
                } catch (URISyntaxException ex) {
                    // should never happen
                    new RuntimeException(ex);
                }
            }
        };
    }

    private ActionListener licenseAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                licenseDialog.pack();
                licenseDialog.setLocationRelativeTo(window);
                licenseDialog.setVisible(true);
            }
        };
    }

    private ActionListener closeAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                window.setVisible(false);
            }
        };
    }

    void show() {
        window.pack();
        window.setLocationRelativeTo(owner);
        window.setVisible(true);
    }

    private class LicenseDialog extends BorderedDialog {
        private LicenseDialog() {
            super(window, i18n);
            setTitle(i18n.getString(I18n.ABOUT_BUTTON_LICENSE_ID));
            setModal(true);

            addCloseButton();

            Icon warningIcon = UIManager.getIcon("OptionPane.informationIcon");
            if (warningIcon != null) {
                addSideImage(warningIcon);
            }

            JTextArea text = new JTextArea(I18n.LICENSE);
            text.setEditable(false);
            text.setCursor(new Cursor(Cursor.TEXT_CURSOR));
            text.setOpaque(false);

            JScrollPane scroll = new JScrollPane(text);
            scroll.setBorder(null);

            getContentPanel().setLayout(new BorderLayout());
            getContentPanel().add(scroll, BorderLayout.CENTER);
        }
    }

}
