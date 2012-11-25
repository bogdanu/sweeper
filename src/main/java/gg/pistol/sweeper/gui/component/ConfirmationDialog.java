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
package gg.pistol.sweeper.gui.component;

import gg.pistol.sweeper.i18n.I18n;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.google.common.base.Preconditions;

/**
 * Internationalized confirmation dialog with confirmation and cancellation buttons.
 *
 * <p>Use this class only from the AWT event dispatching thread (see {@link SwingUtilities#invokeLater}).
 *
 * @author Bogdan Pistol
 */
public class ConfirmationDialog {

    private static final String BUTTON_GROUP = "buttons";

    private boolean confirmed;

    public ConfirmationDialog(@Nullable Window owner, I18n i18n, final String title, final String message) {
        Preconditions.checkNotNull(i18n);
        Preconditions.checkNotNull(title);
        Preconditions.checkNotNull(message);

        DecoratedPanel panel = new DecoratedPanel(i18n, false, null) {
            @Override
            protected void addComponents(JPanel contentPanel) {
                Preconditions.checkNotNull(contentPanel);
                setTitle(title);

                JPanel messagePanel = createHorizontalPanel();
                contentPanel.add(alignCenter(messagePanel));
                messagePanel.add(new JLabel(UIManager.getIcon("OptionPane.questionIcon")));
                messagePanel.add(createHorizontalStrut(5));
                messagePanel.add(new JLabel(message));

                messagePanel.add(Box.createHorizontalGlue());

                contentPanel.add(createVerticalStrut(20));

                JPanel buttons = createHorizontalPanel();
                contentPanel.add(alignCenter((buttons)));

                buttons.add(Box.createHorizontalGlue());
                buttons.add(createButton(i18n.getString(I18n.BUTTON_YES_ID), okAction(parentWindow), BUTTON_GROUP));
                buttons.add(createHorizontalStrut(20));
                buttons.add(createButton(i18n.getString(I18n.BUTTON_NO_ID), closeAction(), BUTTON_GROUP));
                buttons.add(Box.createHorizontalGlue());
            }
        };
        BasicDialog dialog = new BasicDialog(owner, panel);
        dialog.setVisible(true);
    }

    private ActionListener okAction(final Window parentWindow) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                parentWindow.dispose();
            }
        };
    }

    /**
     * Getter for the status of the confirmation.
     *
     * @return the confirmation value
     */
    public boolean isConfirmed() {
        return confirmed;
    }

}
