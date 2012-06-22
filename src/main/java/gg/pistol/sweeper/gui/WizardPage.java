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

import java.awt.Font;

import gg.pistol.sweeper.gui.component.DecoratedPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

abstract class WizardPage extends DecoratedPanel {

    protected final Wizard wizard;

    WizardPage(Wizard wizard) {
        super(wizard.getI18n(), false, null);
        this.wizard = wizard;
    }

    @Override
    protected void addComponents(JPanel contentPanel) {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JLabel header = new JLabel(getPageHeader());
        Font font = header.getFont();
        header.setFont(font.deriveFont(font.getSize2D() + 2));
        contentPanel.add(alignVertically(header));

        contentPanel.add(Box.createVerticalStrut(20));
    }

    protected abstract String getPageHeader();

}
