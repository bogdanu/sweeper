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
package gg.pistol.sweeper.gui;

import com.google.common.base.Preconditions;
import gg.pistol.sweeper.core.Sweeper;
import gg.pistol.sweeper.i18n.I18n;

import javax.annotation.Nullable;
import javax.swing.*;

// package private
class NoDuplicatePage extends WizardPage {

    NoDuplicatePage(I18n i18n, WizardPageListener listener, Sweeper sweeper) {
        super(i18n, listener, sweeper);
    }

    @Override
    protected void addComponents(JPanel contentPanel) {
        Preconditions.checkNotNull(contentPanel);
        super.addComponents(contentPanel);

        contentPanel.add(alignLeft(new JLabel(i18n.getString(I18n.PAGE_NO_DUPLICATE_TEXT_ID))));
    }

    @Override
    protected String getPageHeader() {
        return i18n.getString(I18n.PAGE_NO_DUPLICATE_HEADER_ID);
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
        return false;
    }

    @Override
    boolean isFinishButtonEnabled() {
        return true;
    }

    @Override
    boolean isLastPage() {
        return true;
    }

    @Override
    boolean isLanguageSelectorVisible() {
        return true;
    }

    @Override
    @Nullable
    void cancel() {
    }

    @Override
    @Nullable
    WizardPage back() {
        return null;
    }

    @Override
    @Nullable
    WizardPage next() {
        return null;
    }

    @Override
    @Nullable
    WizardPage finish() {
        return null;
    }
}
