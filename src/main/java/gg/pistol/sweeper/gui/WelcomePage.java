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

import gg.pistol.sweeper.i18n.I18n;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

class WelcomePage extends WizardPage {

    WelcomePage(Wizard wizard) {
        super(wizard);
    }

    @Override
    protected void addComponents(JPanel contentPanel) {
        super.addComponents(contentPanel);
        contentPanel.add(alignVertically(new JLabel(i18n.getString(I18n.PAGE_WELCOME_INTRODUCTION_ID))));
        contentPanel.add(Box.createVerticalStrut(20));

        JPanel panel = createHorizontalPanel();
        panel.add(new JLabel(i18n.getString(I18n.PAGE_WELCOME_LANGUAGE_LABEL_ID)));
        contentPanel.add(alignVertically(panel));
    }

    @Override
    protected String getPageHeader() {
        return i18n.getString(I18n.PAGE_WELCOME_HEADER_ID);
    }

}
