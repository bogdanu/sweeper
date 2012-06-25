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
import gg.pistol.sweeper.gui.component.DecoratedPanel;
import gg.pistol.sweeper.i18n.I18n;

import java.awt.Dimension;
import java.awt.Font;

import javax.annotation.Nullable;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.google.common.base.Preconditions;

//package private
abstract class WizardPage extends DecoratedPanel {

    protected final WizardPageListener listener;
    protected final Sweeper sweeper;

    WizardPage(I18n i18n, WizardPageListener listener, Sweeper sweeper) {
        super(Preconditions.checkNotNull(i18n), false, null);
        Preconditions.checkNotNull(listener);
        Preconditions.checkNotNull(sweeper);
        this.listener = listener;
        this.sweeper = sweeper;
    }

    @Override
    protected void addComponents(JPanel contentPanel) {
        Preconditions.checkNotNull(contentPanel);

        JLabel header = new JLabel(getPageHeader());
        Font font = header.getFont();
        header.setFont(font.deriveFont(font.getSize2D() + 5));
        contentPanel.add(alignLeft(header));

        contentPanel.add(createVerticalStrut(10));
        contentPanel.setPreferredSize(new Dimension(700, 400));
    }

    protected abstract String getPageHeader();

    abstract boolean isCancelButtonVisible();

    abstract boolean isCancelButtonEnabled();

    abstract boolean isBackButtonEnabled();

    abstract boolean isNextButtonEnabled();

    abstract boolean isFinishButtonEnabled();

    abstract boolean isLastPage();

    abstract boolean isLanguageSelectorVisible();

    abstract void cancel();

    @Nullable
    abstract WizardPage back();

    @Nullable
    abstract WizardPage next();

    @Nullable
    abstract WizardPage finish();


    interface WizardPageListener {
        void onButtonStateChange();
    }
}
