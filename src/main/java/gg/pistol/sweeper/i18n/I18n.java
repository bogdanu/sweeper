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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.common.base.Preconditions;

/**
 * Internationalization support for managing locales and resource bundles.
 *
 * <p>This class is thread safe.
 *
 * @author Bogdan Pistol
 */
public class I18n {

    public static final String APPLICATION_NAME = "Sweeper";

    public static final String COPYRIGHT = "Copyright (C) 2012 Bogdan Ciprian Pistol";

    public static final String LICENSE = "Sweeper - Duplicate file/folder cleaner\n"
            + COPYRIGHT + "\n\n"
            + "This program is free software: you can redistribute it and/or modify\n"
            + "it under the terms of the GNU General Public License as published by\n"
            + "the Free Software Foundation, either version 3 of the License, or\n"
            + "(at your option) any later version.\n\n"
            + "This program is distributed in the hope that it will be useful,\n"
            + "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
            + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
            + "GNU General Public License for more details.\n\n"
            + "You should have received a copy of the GNU General Public License\n"
            + "along with this program.  If not, see <http://www.gnu.org/licenses/>.\n";

    public static final String LANGUAGE_NAME_ID = "language.name";
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

    private static final String MESSAGES_BASENAME = "messages";
    private static final String[] SUPPORTED_LANGUAGES = new String[] {"en", "de", "fr", "es", "pt", "ro", "ru", "ar", "ja", "hi", "zh_CN", "zh_TW"};

    private final ResourceBundle.Control resourceBundleControl;
    private final Collection<LocaleChangeListener> listeners;
    private final Map<Locale, String> supportedLocales;
    private ResourceBundle resourceBundle;
    private Locale locale;

    /**
     * Initialize the internationalization using the default locale.
     */
    public I18n() {
        resourceBundleControl = new XMLResourceBundleControl();
        listeners = new LinkedHashSet<LocaleChangeListener>();
        supportedLocales = new LinkedHashMap<Locale, String>();
        populateSupportedLocales();
        setLocale(getDefaultLocale());
    }

    /**
     * The returned default locale is the locale {@link Locale#getDefault()} matched against the supported languages.
     *
     * @return the matched default locale or if there is no match then the locale for the English language is returned
     */
    private Locale getDefaultLocale() {
        Locale defaultLocale = Locale.getDefault();

        // 0 = no match; 1 = language match; 2 = language and country match; 3 = language, country and variant match
        int matchLevel = 0;
        Locale matchedLocale = Locale.ENGLISH;

        for (Locale loc : supportedLocales.keySet()) {
            int currentMatchLevel = 0;
            if (loc.getLanguage().equals(defaultLocale.getLanguage())) {
                currentMatchLevel++;
            }
            if (loc.getCountry().equals(defaultLocale.getCountry())) {
                currentMatchLevel++;
            }
            if (loc.getVariant().equals(defaultLocale.getVariant())) {
                currentMatchLevel++;
            }
            if (currentMatchLevel > matchLevel) {
                matchLevel = currentMatchLevel;
                matchedLocale = loc;
            }
        }
        return matchedLocale;
    }

    private void populateSupportedLocales() {
        for (String lang : SUPPORTED_LANGUAGES) {
            Locale locale;
            int i = lang.indexOf('_');
            if (i != -1) {
                locale = new Locale(lang.substring(0, i), lang.substring(i + 1));
            } else {
                locale = new Locale(lang);
            }
            String langName = ResourceBundle.getBundle(MESSAGES_BASENAME, locale, resourceBundleControl).getString(LANGUAGE_NAME_ID);
            supportedLocales.put(locale, langName);
        }
        Preconditions.checkState(supportedLocales.containsKey(Locale.ENGLISH));
    }

    /**
     * Change the current locale, the registered {@link LocaleChangeListener}s will be notified.
     * If the provided {@code locale} is not supported it will fall back to the English locale.
     *
     * @param locale
     *            the new locale
     */
    public void setLocale(Locale locale) {
        Preconditions.checkNotNull(locale);
        if (!supportedLocales.containsKey(locale)) {
            locale = Locale.ENGLISH;
        }

        synchronized (this) {
            this.locale = locale;
            resourceBundle = ResourceBundle.getBundle(MESSAGES_BASENAME, locale, resourceBundleControl);
        }
        synchronized (listeners) {
            for (LocaleChangeListener listener: listeners) {
                listener.onLocaleChange();
            }
        }
    }

    /**
     * @return the current locale
     */
    synchronized public Locale getLocale() {
        return locale;
    }

    /**
     * Retrieve the localized string message identified by the provided {@code id}.
     *
     * @param id
     *            the unique identifier of the string
     * @param args
     *            optional arguments to be injected in the localized string
     * @return the localized string message
     */
    synchronized public String getString(String id, String... args) {
        Preconditions.checkNotNull(id);
        String msg = resourceBundle.getString(id);

        if (args != null && args.length > 0) {
            msg = MessageFormat.format(msg, (Object[]) args);
        }
        return msg;
    }

    /**
     * Register to receive locale change notifications.
     *
     * @param listener
     *            the locale change listener
     */
    public void registerListener(LocaleChangeListener listener) {
        Preconditions.checkNotNull(listener);
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister from locale change notifications.
     *
     * @param listener
     *            the locale change listener
     */
    public void unregisterListener(LocaleChangeListener listener) {
        Preconditions.checkNotNull(listener);
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Retrieve the supported locales.
     *
     * @return the map of supported locales and for every locale its localized language name
     */
    public Map<Locale, String> getSupportedLocales() {
        return new LinkedHashMap<Locale, String>(supportedLocales);
    }

}
