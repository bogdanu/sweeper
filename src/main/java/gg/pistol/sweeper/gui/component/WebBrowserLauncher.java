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

import gg.pistol.lumberjack.JackLogger;
import gg.pistol.lumberjack.JackLoggerFactory;
import gg.pistol.sweeper.gui.component.MessageDialog.MessageType;
import gg.pistol.sweeper.i18n.I18n;

import java.awt.Desktop;
import java.awt.Window;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nullable;
import javax.swing.SwingUtilities;

import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Utility class that provides functionality to access a URL address with the operating system's default web browser.
 *
 * <p>Use this class only from the AWT event dispatching thread (see {@link SwingUtilities#invokeLater}).
 *
 * @author Bogdan Pistol
 */
public class WebBrowserLauncher {

    private final JackLogger log;
    private final I18n i18n;

    public WebBrowserLauncher(I18n i18n) {
        Preconditions.checkNotNull(i18n);
        this.i18n = i18n;
        log = JackLoggerFactory.getLogger(LoggerFactory.getLogger(WebBrowserLauncher.class));
    }

    /**
     * Open the operating system's default web browser and access the provided {@code url}.
     *
     * @param url
     *         the web address to access
     * @param parentWindow
     *         in case of error this parameter specifies which window will be the parent of the error dialog box,
     *         this parameter can be {@code null} for no parent
     */
    public void openWebBrowser(final String url, @Nullable Window parentWindow) {
        Preconditions.checkNotNull(url);

        URI uri;
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
            new MessageDialog(parentWindow, MessageType.ERROR, i18n, i18n.getString(I18n.LABEL_ERROR_ID),
                    i18n.getString(I18n.WEB_BROWSER_LAUNCHER_ERROR_ID), url);
        }
    }

}
