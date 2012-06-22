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
package gg.pistol.sweeper.gui.component;

import gg.pistol.lumberjack.JackLogger;
import gg.pistol.lumberjack.JackLoggerFactory;
import gg.pistol.sweeper.i18n.I18n;

import java.awt.Desktop;
import java.awt.Window;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Utility class that provides functionality to access a URL address with the operating system's default web browser.
 *
 * @author Bogdan Pistol
 */
public class WebBrowserLauncher {

    private final JackLogger log = JackLoggerFactory.getLogger(LoggerFactory.getLogger(WebBrowserLauncher.class));
    private final I18n i18n;

    public WebBrowserLauncher(I18n i18n) {
        Preconditions.checkNotNull(i18n);
        this.i18n = i18n;
    }

    public void openWebBrowser(final String url, Window nobrowserDialogOwner, final String nobrowserTitleId) {
        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(nobrowserDialogOwner);
        Preconditions.checkNotNull(nobrowserTitleId);

        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        boolean nobrowser = !Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
        if (!nobrowser) {
            try {
                log.info("Opening the operating system default web browser with the URL <{}>.", url);
                Desktop.getDesktop().browse(uri);
            } catch (Exception e) {
                nobrowser = true;
            }
        }
        if (nobrowser) {
            log.warn("Could not open the operating system default web browser.");
            DecoratedPanel panel = new DecoratedPanel(i18n, true, UIManager.getIcon("OptionPane.warningIcon")) {
                @Override
                protected void addComponents(JPanel contentPanel) {
                    setTitle(i18n.getString(nobrowserTitleId));
                    contentPanel.add(new JLabel(i18n.getString(I18n.WEBBROWSER_NOBROWSER_ID) + " "));
                    contentPanel.add(createTextLabel(url));
                }
            };
            BasicDialog dialog = new BasicDialog(nobrowserDialogOwner, panel);
            dialog.setVisible(true);
        }
    }

}
