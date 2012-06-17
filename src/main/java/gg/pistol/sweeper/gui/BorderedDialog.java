/*
 * Sweeper
 * Copyright (C) 2012 Bogdan Pistol
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.google.common.base.Preconditions;

class BorderedDialog extends JDialog {

    private static final int BORDER_SIZE = 5;

    private final JPanel contentPanel;
    private final I18n i18n;

    BorderedDialog(@Nullable Window owner, I18n i18n) {
        super(owner);
        Preconditions.checkNotNull(i18n);
        this.i18n = i18n;

        contentPanel = new JPanel();
        setBoxLayout(contentPanel, BoxLayout.LINE_AXIS);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        if (owner == null) {
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        }
    }

    void addCloseButton() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(0, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        setBoxLayout(panel, BoxLayout.LINE_AXIS);
        panel.add(Box.createHorizontalGlue());

        JButton close = new JButton(i18n.getString(I18n.BUTTON_CLOSE_ID));
        panel.add(close);
        panel.add(Box.createHorizontalGlue());

        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (getOwner() == null) {
                    dispose();
                } else {
                    setVisible(false);
                }
            }
        });
        getContentPane().add(panel, BorderLayout.SOUTH);
    }

    void addSideImage(Icon image) {
        JPanel panel = new JPanel();
        setBoxLayout(panel, BoxLayout.PAGE_AXIS);
        panel.add(new JLabel(image));
        panel.add(Box.createVerticalGlue());
        if (panel.getComponentOrientation().isLeftToRight()) {
            panel.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, 0));
            getContentPane().add(panel, BorderLayout.WEST);
        } else {
            panel.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, 0, BORDER_SIZE, BORDER_SIZE));
            getContentPane().add(panel, BorderLayout.EAST);
        }
    }

    void setBoxLayout(Container container, int axis) {
        container.setLayout(new BoxLayout(container, axis));
        container.setComponentOrientation(i18n.getComponentOrientation());
    }

    void addAlignedComponent(Container container, JComponent element) {
        element.setAlignmentX(0.0f);
        container.add(element);
    }

    JPanel getContentPanel() {
        return contentPanel;
    }

}
