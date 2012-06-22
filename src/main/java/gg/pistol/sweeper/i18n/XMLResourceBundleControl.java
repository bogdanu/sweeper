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
package gg.pistol.sweeper.i18n;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;

/**
 * {@link ResourceBundle.Control} implementation that creates {@link XMLResourceBundle}s.
 *
 * <p>This class is thread safe.
 *
 * @author Bogdan Pistol
 */
// package private
class XMLResourceBundleControl extends ResourceBundle.Control {

    @Override
    public Locale getFallbackLocale(String baseName, Locale locale) {
        // the I18n class has a default locale, there is no need for a fallback locale
        return null;
    }

    @Override
    public List<String> getFormats(String baseName) {
        Preconditions.checkNotNull(baseName);
        return Arrays.asList("xml");
    }

    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
            boolean reload) throws IllegalAccessException, InstantiationException, IOException {
        Preconditions.checkNotNull(baseName);
        Preconditions.checkNotNull(locale);
        Preconditions.checkNotNull(format);
        Preconditions.checkNotNull(loader);

        if (!format.equals("xml")) {
            return null;
        }

        String bundleName = toBundleName(baseName, locale);
        if (bundleName == null) {
            return null;
        }

        String resourceName = toResourceName(bundleName, format);
        if (resourceName == null) {
            return null;
        }

        InputStream stream = null;
        if (reload) {
            URL url = loader.getResource(resourceName);
            if (url != null) {
                URLConnection connection = url.openConnection();
                if (connection != null) {
                    connection.setUseCaches(false);
                    stream = connection.getInputStream();
                }
            }
        } else {
            stream = loader.getResourceAsStream(resourceName);
        }

        if (stream == null) {
            return null;
        }
        stream = new BufferedInputStream(stream);
        try {
            return new XMLResourceBundle(stream);
        } finally {
            Closeables.closeQuietly(stream);
        }
    }
}
