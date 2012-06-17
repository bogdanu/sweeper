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

import java.awt.ComponentOrientation;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;

class I18n {

    public static final String APPLICATION_NAME = "Sweeper";

    public static final String LICENSE = "Sweeper\n"
            + "Copyright (C) 2012 Bogdan Pistol\n\n"
            + "This program is free software: you can redistribute it and/or modify\n"
            + "it under the terms of the GNU General Public License version 3\n"
            + "as published by the Free Software Foundation.\n\n"
            + "This program is distributed in the hope that it will be useful,\n"
            + "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
            + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
            + "GNU General Public License for more details.\n"
            + "You should have received a copy of the GNU General Public License\n"
            + "along with this program.  If not, see <http://www.gnu.org/licenses/>.\n";

    public static final String WIZARD_TITLE_ID = "wizard.title";
    public static final String WEBBROWSER_NOBROWSER_ID = "webbrowser.nobrowser";

    public static final String WIZARD_BUTTON_HELP_ID = "wizard.button.help";
    public static final String WIZARD_BUTTON_ABOUT_ID = "wizard.button.about";
    public static final String WIZARD_BUTTON_CANCEL_ID = "wizard.button.cancel";
    public static final String WIZARD_BUTTON_BACK_ID = "wizard.button.back";
    public static final String WIZARD_BUTTON_NEXT_ID = "wizard.button.next";
    public static final String WIZARD_BUTTON_FINISH_ID = "wizard.button.finish";

    public static final String BUTTON_CLOSE_ID = "button.close";

    public static final String ABOUT_LABEL_WEBSITE_ID = "about.label.website";
    public static final String ABOUT_BUTTON_LICENSE_ID = "about.button.license";

    private ResourceBundle resourceBundle;
    private Locale locale;

    I18n(Locale locale) {
        setLocale(locale);
    }

    void setLocale(Locale locale) {
        Preconditions.checkNotNull(locale);
        this.locale = locale;
        resourceBundle = ResourceBundle.getBundle("messages", locale, new Control());
    }

    Locale getLocale() {
        return locale;
    }

    ComponentOrientation getComponentOrientation() {
        return ComponentOrientation.getOrientation(locale);
    }

    String getString(String id, String... args) {
        Preconditions.checkNotNull(id);
        String msg = resourceBundle.getString(id);

        if (args != null && args.length > 0) {
            msg = MessageFormat.format(msg, (Object[]) args);
        }
        return msg;
    }

    private static class Control extends ResourceBundle.Control {

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

    private static class XMLResourceBundle extends ResourceBundle {
        private Properties properties;
        private Collection<String> keys;

        XMLResourceBundle(InputStream stream) throws InvalidPropertiesFormatException, IOException {
            properties = new Properties();
            properties.loadFromXML(stream);
        }

        protected Object handleGetObject(String key) {
            Preconditions.checkNotNull(key);
            return properties.getProperty(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            if (keys == null) {
                Set<String> set = new LinkedHashSet<String>();
                for (Object o : properties.keySet()) {
                    set.add((String) o);
                }
                Enumeration<String> e = parent.getKeys();
                while (e.hasMoreElements()) {
                    set.add(e.nextElement());
                }
                keys = set;
            }
            return Collections.enumeration(keys);
        }
    }

}
