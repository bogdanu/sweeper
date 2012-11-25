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
package gg.pistol.sweeper.i18n;

import gg.pistol.lumberjack.JackLogger;
import gg.pistol.lumberjack.JackLoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;

import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
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

    public static final String LICENSE = "Sweeper - Duplicate file cleaner\n"
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

    public static final String LABEL_ERROR_ID = "label.error";

    public static final String BUTTON_OK_ID = "button.ok";
    public static final String BUTTON_CANCEL_ID = "button.cancel";
    public static final String BUTTON_ADD_ID = "button.add";
    public static final String BUTTON_REMOVE_ID = "button.remove";
    public static final String BUTTON_HELP_ID = "button.help";
    public static final String BUTTON_ABOUT_ID = "button.about";
    public static final String BUTTON_FINISH_ID = "button.finish";
    public static final String BUTTON_CLOSE_ID = "button.close";

    public static final String WIZARD_TITLE_ID = "wizard.title";
    public static final String WIZARD_CHANGE_LANGUAGE_ID = "wizard.languageSelector";
    public static final String WIZARD_ERROR_OPEN_ID = "wizard.error.open";
    public static final String WIZARD_BUTTON_BACK_ID = "wizard.button.back";
    public static final String WIZARD_BUTTON_NEXT_ID = "wizard.button.next";

    public static final String WEB_BROWSER_LAUNCHER_ERROR_ID = "webBrowserLauncher.error";

    public static final String ABOUT_WEBSITE_ID = "about.website";
    public static final String ABOUT_BUTTON_LICENSE_ID = "about.button.license";

    public static final String PAGE_WELCOME_HEADER_ID = "page.welcome.header";
    public static final String PAGE_WELCOME_INTRODUCTION_ID = "page.welcome.introduction";
    public static final String PAGE_WELCOME_CHANGE_LANGUAGE_ID = "page.welcome.changeLanguage";

    public static final String PAGE_RESOURCE_SELECTION_HEADER_ID = "page.resourceSelection.header";
    public static final String PAGE_RESOURCE_SELECTION_INTRODUCTION_ID = "page.resourceSelection.introduction";
    public static final String PAGE_RESOURCE_SELECTION_FILE_CHOOSER_TITLE_ID = "page.resourceSelection.fileChooser.title";
    public static final String PAGE_RESOURCE_SELECTION_FILE_CHOOSER_RESOURCE_ERROR_ID = "page.resourceSelection.fileChooser.resourceError";

    public static final String PAGE_ANALYSIS_HEADER_ID = "page.analysis.header";
    public static final String PAGE_ANALYSIS_INTRODUCTION_ID = "page.analysis.introduction";
    public static final String PAGE_ANALYSIS_TOTAL_PROGRESS_ID = "page.analysis.totalProgress";
    public static final String PAGE_ANALYSIS_TOTAL_ELAPSED_TIME_ID = "page.analysis.totalElapsedTime";
    public static final String PAGE_ANALYSIS_TOTAL_REMAINING_TIME_ID = "page.analysis.totalRemainingTime";
    public static final String PAGE_ANALYSIS_OPERATION_LABEL_ID = "page.analysis.operationLabel";
    public static final String PAGE_ANALYSIS_OPERATION_RESOURCE_TRAVERSAL_ID = "page.analysis.operationResourceTraversing";
    public static final String PAGE_ANALYSIS_OPERATION_SIZE_COMPUTATION_ID = "page.analysis.operationSizeComputation";
    public static final String PAGE_ANALYSIS_OPERATION_HASH_COMPUTATION_ID = "page.analysis.operationHashComputation";
    public static final String PAGE_ANALYSIS_OPERATION_RESOURCE_DELETION_ID = "page.analysis.operationResourceDeletion";
    public static final String PAGE_ANALYSIS_OPERATION_PROGRESS_ID = "page.analysis.operationProgress";
    public static final String PAGE_ANALYSIS_OPERATION_TARGET_LABEL_ID = "page.analysis.operationTargetLabel";
    public static final String PAGE_ANALYSIS_ERROR_COUNTER_ID = "page.analysis.error.counter";

    public static final String FILE_CHOOSER_CURRENT_DIRECTORY_ID = "fileChooser.currentDirectory";
    public static final String FILE_CHOOSER_UP_ID = "fileChooser.up";
    public static final String FILE_CHOOSER_HOME_ID = "fileChooser.home";
    public static final String FILE_CHOOSER_SHOW_LIST_ID = "fileChooser.showList";
    public static final String FILE_CHOOSER_SHOW_DETAILS_ID = "fileChooser.showDetails";
    public static final String FILE_CHOOSER_NAME_ID = "fileChooser.name";
    public static final String FILE_CHOOSER_FILTER_LABEL_ID = "fileChooser.filterLabel";
    public static final String FILE_CHOOSER_FILTER_ALL_ID = "fileChooser.filterAll";
    public static final String FILE_CHOOSER_DETAILS_NAME_ID = "fileChooser.detailsName";
    public static final String FILE_CHOOSER_DETAILS_SIZE_ID = "fileChooser.detailsSize";
    public static final String FILE_CHOOSER_DETAILS_MODIFIED_ID = "fileChooser.detailsModified";
    public static final String FILE_CHOOSER_MENU_VIEW_ID = "fileChooser.menuView";
    public static final String FILE_CHOOSER_MENU_REFRESH_ID = "fileChooser.menuRefresh";

    public static final String TEXT_COPY_ID = "text.copy";
    public static final String TEXT_SELECT_ALL_ID = "text.selectAll";

    public static final String TIME_DESCRIPTION_HOURS_ID = "time.description.hours";
    public static final String TIME_DESCRIPTION_MINUTES_ID = "time.description.minutes";
    public static final String TIME_DESCRIPTION_SECONDS_ID = "time.description.seconds";

    private static final String MESSAGES_BASENAME = "messages";
    private static final String[] SUPPORTED_LANGUAGES = new String[]{"en", "de", "fr", "es", "pt", "ro", "ru", "ar", "iw", "ja", "hi", "zh_CN", "zh_TW"};


    private final JackLogger log;
    private final Lock lock;

    private final Collection<SupportedLocale> supportedLocales;
    private final Collection<LocaleChangeListener> listeners;

    private final ResourceBundle.Control resourceBundleControl;
    private ResourceBundle resourceBundle;
    private SupportedLocale locale;

    /**
     * Initialize the internationalization using the default locale.
     */
    public I18n() {
        log = JackLoggerFactory.getLogger(LoggerFactory.getLogger(I18n.class));
        lock = new ReentrantLock();
        supportedLocales = new ArrayList<SupportedLocale>();
        listeners = new LinkedHashSet<LocaleChangeListener>();
        resourceBundleControl = new XMLResourceBundleControl();
        populateSupportedLocales();
        setLocale(getDefaultLocale());
    }

    /**
     * The returned default locale is the locale {@link Locale#getDefault()} matched against the supported languages.
     *
     * @return the matched default locale or if there is no match then the locale for the English language is returned
     */
    private SupportedLocale getDefaultLocale() {
        Locale defaultLocale = Locale.getDefault();

        // 0 = no match; 1 = language match; 2 = language and country match; 3 = language, country and variant match
        int matchLevel = 0;
        SupportedLocale matchedLocale = resolveSupportedLocale(Locale.ENGLISH);
        Preconditions.checkState(matchedLocale != null);

        for (SupportedLocale loc : supportedLocales) {
            int currentMatchLevel = 0;
            if (loc.getLocale().getLanguage().equals(defaultLocale.getLanguage())) {
                currentMatchLevel++;
            }
            if (loc.getLocale().getCountry().equals(defaultLocale.getCountry())) {
                currentMatchLevel++;
            }
            if (loc.getLocale().getVariant().equals(defaultLocale.getVariant())) {
                currentMatchLevel++;
            }
            if (currentMatchLevel > matchLevel) {
                matchLevel = currentMatchLevel;
                matchedLocale = loc;
            }
        }

        log.info("Internationalization default language matched with the supported locales is <{}>.", matchedLocale);
        return matchedLocale;
    }

    @Nullable
    private SupportedLocale resolveSupportedLocale(Locale locale) {
        for (SupportedLocale supportedLocale : supportedLocales) {
            if (supportedLocale.getLocale().equals(locale)) {
                return supportedLocale;
            }
        }
        return null;
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
            supportedLocales.add(new SupportedLocale(locale, langName));
        }
        Preconditions.checkState(resolveSupportedLocale(Locale.ENGLISH) != null);
        if (log.isInfoEnabled()) {
            log.info("Internationalization supports the languages: {}.", Joiner.on(", ").join(supportedLocales));
        }
    }

    /**
     * Change the current locale, the registered {@link LocaleChangeListener}s will be notified.
     * If the provided {@code locale} is not supported it will fall back to the English locale.
     *
     * @param locale
     *         the new locale
     */
    public void setLocale(Locale locale) {
        Preconditions.checkNotNull(locale);
        SupportedLocale supportedLocale = resolveSupportedLocale(locale);
        if (supportedLocale == null) {
            supportedLocale = resolveSupportedLocale(Locale.ENGLISH);
            Preconditions.checkState(supportedLocale != null);
        }
        setLocale(supportedLocale);
    }

    /**
     * Change the current locale, the registered {@link LocaleChangeListener}s will be notified.
     * If the provided {@code locale} is not supported it will fall back to the English locale.
     *
     * @param supportedLocale
     *         the new locale
     */
    public void setLocale(SupportedLocale supportedLocale) {
        Preconditions.checkNotNull(supportedLocale);
        log.info("Internationalization is configured with the language <{}>.", supportedLocale);

        Collection<LocaleChangeListener> toNotify;
        lock.lock();
        try {
            locale = supportedLocale;
            resourceBundle = ResourceBundle.getBundle(MESSAGES_BASENAME, supportedLocale.getLocale(), resourceBundleControl);
            toNotify = new ArrayList<LocaleChangeListener>(listeners);
        } finally {
            lock.unlock();
        }
        Locale.setDefault(locale.getLocale());
        for (LocaleChangeListener listener : toNotify) {
            listener.onLocaleChange();
        }
    }

    /**
     * @return the current locale
     */
    public Locale getLocale() {
        lock.lock();
        try {
            return locale.getLocale();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return the current supported locale
     */
    public SupportedLocale getCurrentSupportedLocale() {
        lock.lock();
        try {
            return locale;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieve the localized string message identified by the provided {@code id}.
     *
     * @param id
     *         the unique identifier of the string
     * @param args
     *         optional arguments to be injected in the localized string
     * @return the localized string message
     */
    public String getString(String id, @Nullable String... args) {
        Preconditions.checkNotNull(id);

        lock.lock();
        try {
            String msg = resourceBundle.getString(id);

            if (args != null && args.length > 0) {
                msg = MessageFormat.format(msg, (Object[]) args);
            }
            return msg;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Register to receive locale change notifications.
     *
     * @param listener
     *         the locale change listener
     */
    public void registerListener(LocaleChangeListener listener) {
        Preconditions.checkNotNull(listener);
        lock.lock();
        try {
            listeners.add(listener);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Unregister from locale change notifications.
     *
     * @param listener
     *         the locale change listener
     */
    public void unregisterListener(LocaleChangeListener listener) {
        Preconditions.checkNotNull(listener);
        lock.lock();
        try {
            listeners.remove(listener);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieve the supported locales.
     *
     * @return the supported locales
     */
    public Collection<SupportedLocale> getSupportedLocales() {
        return Collections.unmodifiableCollection(supportedLocales);
    }

}
