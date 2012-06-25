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

import javax.swing.JPanel;

import com.google.common.base.Preconditions;

//package private
class TargetSelectionPage extends WizardPage {

    private final WizardPage previousPage;

    TargetSelectionPage(WizardPage previousPage, I18n i18n, WizardPageListener listener, Sweeper sweeper) {
        super(Preconditions.checkNotNull(i18n), Preconditions.checkNotNull(listener), Preconditions.checkNotNull(sweeper));
        Preconditions.checkNotNull(previousPage);
        this.previousPage = previousPage;
    }

    @Override
    protected void addComponents(JPanel contentPanel) {
        Preconditions.checkNotNull(contentPanel);
        super.addComponents(contentPanel);
    }

    @Override
    protected String getPageHeader() {
        return i18n.getString(I18n.PAGE_TARGET_SELECTION_HEADER_ID);
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
        return true;
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
        return true;
    }

    @Override
    void cancel() {
    }

    @Override
    WizardPage back() {
        return previousPage;
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
