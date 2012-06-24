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
package gg.pistol.sweeper.gui.component;

import gg.pistol.sweeper.i18n.I18n;

import java.awt.Window;

import javax.annotation.Nullable;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * A simple dialog that displays a text message.
 *
 * <p>Use this class only from the AWT event dispatching thread (see {@link SwingUtilities#invokeLater}).
 *
 * @author Bogdan Pistol
 */
public class MessageDialog {

    public MessageDialog(@Nullable Window owner, Type type, I18n i18n, final String title, final String message) {
        Icon image = null;
        switch (type) {
        case INFORMATION:
            image = UIManager.getIcon("OptionPane.informationIcon");
            break;
        case WARNING:
            image = UIManager.getIcon("OptionPane.warningIcon");
            break;
        case ERROR:
            image = UIManager.getIcon("OptionPane.errorIcon");
            break;
        }
        DecoratedPanel panel = new DecoratedPanel(i18n, true, image) {
            @Override
            protected void addComponents(JPanel contentPanel) {
                setTitle(title);
                contentPanel.add(new JLabel(message));
            }
        };
        BasicDialog dialog = new BasicDialog(owner, panel);
        dialog.setVisible(true);
    }

    /**
     * Dialog types
     */
    public static enum Type {
        PLAIN, INFORMATION, WARNING, ERROR
    }

}
