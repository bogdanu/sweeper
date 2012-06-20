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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Extension of {@link DynamicPanel} that provides decorations: border, side image and/or close button.
 *
 * <p>The side image position is taking into account the locale (left-to-right or right-to-left language) and the close
 * button contains a localized string message.
 *
 * @author Bogdan Pistol
 */
public abstract class DecoratedPanel extends DynamicPanel {

    private static final int DEFAULT_BORDER = 5;

    private final int border;
    private final boolean closeButton;
    @Nullable private final Icon sideImage;

    private final Multimap<String, JComponent> sizeGroups;

    /**
     * Creates an instance with a default border value.
     *
     * @param i18n
     *            the internationalization instance
     * @param closeButton
     *            whether to have a close button or not
     * @param sideImage
     *            the side image or null
     */
    protected DecoratedPanel(I18n i18n, boolean closeButton, @Nullable Icon sideImage) {
        this(i18n, DEFAULT_BORDER, closeButton, sideImage);
    }

    /**
     * Constructor
     *
     * @param i18n
     *            the internationalization instance
     * @param border
     *            the border size or 0 for no border
     * @param closeButton
     *            whether to have a close button or not
     * @param sideImage
     *            the side image or null
     */
    protected DecoratedPanel(I18n i18n, int border, boolean closeButton, @Nullable Icon sideImage) {
        super(i18n);
        Preconditions.checkArgument(border >= 0);
        this.border = border;
        this.closeButton = closeButton;
        this.sideImage = sideImage;
        sizeGroups = ArrayListMultimap.create();

        if (border > 0) {
            setBorder(BorderFactory.createEmptyBorder(border, border, border, border));
        }
        setLayout(new BorderLayout());
    }

    /**
     * This method is final to not be further extended. To add components to the decorated panel extend
     * the {@link #addComponents(JPanel)} method.
     */
    final protected void addComponents() {
        if (closeButton) {
            addCloseButton();
        }
        if (sideImage != null) {
            addSideImage();
        }
        JPanel contentPanel = createHorizontalPanel();
        add(contentPanel, BorderLayout.CENTER);

        addComponents(contentPanel);
        computePreferredSizes();
    }

    private void computePreferredSizes() {
        for (String key : sizeGroups.keySet()) {
            int width = -1;
            int height = -1;
            for (JComponent component : sizeGroups.get(key)) {
                if (width < component.getPreferredSize().width) {
                    width = component.getPreferredSize().width;
                }
                if (height < component.getPreferredSize().height) {
                    height = component.getPreferredSize().height;
                }
            }

            // Sometimes the preferred width of a component can be too small to accommodate the contained text and in
            // that case the text is truncated and ellipsis will be shown. To fix it the width is increased.
            width += 2;

            for (JComponent component : sizeGroups.get(key)) {
                component.setPreferredSize(new Dimension(width, height));
            }
        }
        sizeGroups.clear();
    }

    /**
     * All the components should be added to the provided {@code contentPanel} based on the locale. The title of the
     * parent window should be configured based on the locale with this method.
     *
     * <p>This method will be called whenever the locale changes.
     *
     * @param contentPanel
     *            the container where to add all the components, its layout manager is a horizontal {@link BoxLayout}
     *            that takes into account the locale
     */
    protected abstract void addComponents(JPanel contentPanel);

    private void addSideImage() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel(sideImage));
        panel.add(Box.createVerticalGlue());

        if (ComponentOrientation.getOrientation(i18n.getLocale()).isLeftToRight()) {
            panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, border));
            add(panel, BorderLayout.WEST);
        } else {
            panel.setBorder(BorderFactory.createEmptyBorder(0, border, 0, 0));
            add(panel, BorderLayout.EAST);
        }
    }

    private void addCloseButton() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createHorizontalGlue());
        JButton button = new JButton(i18n.getString(I18n.BUTTON_CLOSE_ID));
        panel.add(button);
        panel.add(Box.createHorizontalGlue());
        button.addActionListener(closeAction());

        panel.setBorder(BorderFactory.createEmptyBorder(0, border, border, border));
        add(panel, BorderLayout.SOUTH);
    }

    private ActionListener closeAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (parentWindow == null) { // if there is no parent yet configured then ignore the action
                    return;
                }
                if (parentWindow.getOwner() == null) { // dispose the windows without any owner
                    parentWindow.dispose();
                } else {
                    parentWindow.setVisible(false);
                }
            }
        };
    }

    /**
     * Helper factory method for creating selectable text labels (for copy & paste support)
     *
     * @param text
     *            the string that will be selectable
     * @return the selectable text component
     */
    protected JComponent createTextLabel(String text) {
        Preconditions.checkNotNull(text);

        JTextField textLabel = new JTextField(text);
        textLabel.setEditable(false);
        textLabel.setBorder(null);
        textLabel.setOpaque(false);
        textLabel.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        return textLabel;
    }

    /**
     * Helper factory method for creating clickable links.
     *
     * @param linkText
     *            a string that will be displayed as a link
     * @param action
     *            the action performed at click
     * @return the link component
     */
    protected JComponent createLink(String linkText, final Runnable action) {
        Preconditions.checkNotNull(linkText);
        Preconditions.checkNotNull(action);

        JLabel link = new JLabel("<html><a href=''>" + linkText + "</a></html>");
        link.setComponentOrientation(ComponentOrientation.getOrientation(i18n.getLocale()));
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        return link;
    }

    /**
     * Helper method to set the alignment.
     *
     * @param component
     *            the component to align
     * @return the aligned component
     */
    protected <T extends JComponent> T alignVertically(T component) {
        Preconditions.checkNotNull(component);

        component.setComponentOrientation(ComponentOrientation.getOrientation(i18n.getLocale()));
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    /**
     * Helper factory method for creating a horizontal box layout {@link JPanel} that takes into account the locale.
     *
     * @return the created panel
     */
    protected JPanel createHorizontalPanel() {
        JPanel panel = new JPanel();
        panel.setComponentOrientation(ComponentOrientation.getOrientation(i18n.getLocale()));
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        return panel;
    }

    /**
     * Assign the provided {@code component} to a group of same preferred size components. All the components in
     * the group will have the preferred size of the biggest component from the group.
     *
     * @param groupId
     *            an identifier for the group
     * @param component
     *            the component to add to the group
     * @return the grouped component
     */
    protected <T extends JComponent> T sizeGroup(String groupId, T component) {
        Preconditions.checkNotNull(groupId);
        Preconditions.checkNotNull(component);

        sizeGroups.put(groupId, component);
        return component;
    }

    /**
     * Helper factory method for creating a button.
     *
     * @param text
     *            the text of the button
     * @param action
     *            the action of the button
     * @return the newly created button
     */
    protected JButton createButton(String text, ActionListener action) {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(action);

        JButton button = new JButton(text);
        button.addActionListener(action);
        return button;
    }

    /**
     * Helper factory method for creating a button and placing it in a group of same size components.
     *
     * @param text
     *            the text of the button
     * @param action
     *            the action of the button
     * @param sizeGroupId
     *            the group identifier of the same size components
     * @return the newly created button
     */
    protected JButton createButton(String text, ActionListener action, String sizeGroupId) {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(action);
        Preconditions.checkNotNull(sizeGroupId);

        JButton button = new JButton(text);
        button.addActionListener(action);
        return sizeGroup(sizeGroupId, button);
    }

}
