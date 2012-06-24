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

import gg.pistol.sweeper.core.Sweeper;
import gg.pistol.sweeper.i18n.I18n;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.google.common.base.Preconditions;

// package private
class WelcomePage extends WizardPage {

    WelcomePage(I18n i18n, WizardPageListener listener, Sweeper sweeper) {
        super(Preconditions.checkNotNull(i18n), Preconditions.checkNotNull(listener), Preconditions.checkNotNull(sweeper));
    }

    @Override
    protected void addComponents(JPanel contentPanel) {
        Preconditions.checkNotNull(contentPanel);
        super.addComponents(contentPanel);

        contentPanel.add(createVerticalStrut(20));
        contentPanel.add(alignLeft(new JLabel(i18n.getString(I18n.PAGE_WELCOME_INTRODUCTION_ID))));
        contentPanel.add(createVerticalStrut(20));

        JPanel languagePanel = createHorizontalPanel();
        contentPanel.add(alignLeft(languagePanel));
        languagePanel.add(new JLabel(i18n.getString(I18n.PAGE_WELCOME_CHANGE_LANGUAGE_ID)));
        languagePanel.add(createHorizontalStrut(10));
        languagePanel.add(createLanguageSelector(200));

        contentPanel.add(Box.createVerticalGlue());
    }

    @Override
    protected String getPageHeader() {
        return i18n.getString(I18n.PAGE_WELCOME_HEADER_ID);
    }

    @Override
    boolean isCancelButtonVisible() {
        return false;
    }

    @Override
    boolean isCancelButtonEnabled() {
        return false;
    }

    @Override
    boolean isBackButtonEnabled() {
        return false;
    }

    @Override
    boolean isNextButtonEnabled() {
        return true;
    }

    @Override
    boolean isFinishButtonEnabled() {
        return false;
    }

    @Override
    boolean isLastPage() {
        return false;
    }

    @Override
    boolean isLanguageSelectorVisible() {
        return false;
    }

    @Override
    void cancel() {
    }

    @Override
    WizardPage back() {
        return null;
    }

    @Override
    WizardPage next() {
        return null;
    }

    @Override
    WizardPage finish() {
        return null;
    }

}
