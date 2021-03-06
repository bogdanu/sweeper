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

import java.awt.BorderLayout;
import java.awt.Window;

import javax.annotation.Nullable;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.google.common.base.Preconditions;

/**
 * A reusable dialog that provides basic functionality and wraps a {@link DynamicPanel}.
 *
 * <p>Use this class only from the AWT event dispatching thread (see {@link SwingUtilities#invokeLater}).
 *
 * @author Bogdan Pistol
 */
public class BasicDialog extends JDialog {

    private final DynamicPanel panel;

    public BasicDialog(@Nullable Window owner, DynamicPanel panel) {
        super(owner);
        Preconditions.checkNotNull(panel);
        this.panel = panel;

        setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        if (owner != null) {
            setModalityType(ModalityType.APPLICATION_MODAL);
        }
    }

    @Override
    public void dispose() {
        panel.setParentWindow(null);
        super.dispose();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            if (panel.getParentWindow() == null) {
                panel.setParentWindow(this);
            }
            pack();
            if (!isVisible()) {
                setLocationRelativeTo(getOwner());
            }
        }
        super.setVisible(visible);
    }

}
