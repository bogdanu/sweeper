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

import java.util.Locale;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * A locale supported by the application. Every locale has associated with it an internationalized language name.
 *
 * @author Bogdan Pistol
 */
public class SupportedLocale {

    private final Locale locale;

    private final String languageName;

    // package private
    SupportedLocale(Locale locale, String languageName) {
        Preconditions.checkNotNull(locale);
        Preconditions.checkNotNull(languageName);
        this.locale = locale;
        this.languageName = languageName;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getLanguageName() {
        return languageName;
    }

    @Override
    public String toString() {
        return languageName;
    }

    @Override
    public int hashCode() {
        return locale.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SupportedLocale other = (SupportedLocale) obj;
        return locale.equals(other.locale);
    }

}
