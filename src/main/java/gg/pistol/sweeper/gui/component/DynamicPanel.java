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
import gg.pistol.sweeper.i18n.LocaleChangeListener;

import java.awt.ComponentOrientation;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import javax.annotation.Nullable;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.common.base.Preconditions;

/**
 * A JPanel that changes its layout dynamically based on locale changes.
 *
 * <p>Use this class only from the AWT event dispatching thread (see {@link SwingUtilities#invokeLater}).
 *
 * @author Bogdan Pistol
 */
public abstract class DynamicPanel extends JPanel implements LocaleChangeListener {

    protected final I18n i18n;
    @Nullable protected Window parentWindow;

    /**
     * Constructor that initializes the component orientation based on the locale value.
     *
     * @param i18n
     *         the internationalization object
     */
    protected DynamicPanel(I18n i18n) {
        Preconditions.checkNotNull(i18n);
        this.i18n = i18n;
        setComponentOrientation(ComponentOrientation.getOrientation(i18n.getLocale()));
    }

    public void onLocaleChange() {
        /*
         * Ensure that the update() method is called only from the AWT event dispatching thread.
         * This is required because the onLocaleChange() method is called without knowing that this is a Swing component.
         */
        if (SwingUtilities.isEventDispatchThread()) {
            update();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    update();
                }
            });
        }
    }

    /**
     * Update the panel by removing all its components and re-adding them again according to the locale and layout.
     */
    public void update() {
        Preconditions.checkNotNull(parentWindow);
        setComponentOrientation(ComponentOrientation.getOrientation(i18n.getLocale()));
        removeAll();
        addComponents();
        parentWindow.pack();
    }

    /**
     * All the components contained by the panel should be added based on locale with this method.
     * The title of the parent window should be configured based on the locale with this method.
     *
     * <p>This method will be called whenever the locale changes.
     */
    protected abstract void addComponents();

    /**
     * Setter for the parent window that contains this panel.
     *
     * <p>This method will call {@link #addComponents} to init the components.
     *
     * @param parentWindow
     *         the parent window
     */
    public void setParentWindow(@Nullable Window parentWindow) {
        if (parentWindow == null) {
            i18n.unregisterListener(this);
            removeAll();
        } else {
            this.parentWindow = parentWindow;
            i18n.registerListener(this);
            addComponents();
        }
    }

    /**
     * Getter for the parent window.
     *
     * @return the parent window or {@code null} if none configured
     */
    @Nullable
    public Window getParentWindow() {
        return parentWindow;
    }

    /**
     * Helper method for setting the title of the parent window.
     *
     * <p>The title should be configured from the {@link #addComponents} method.
     *
     * @param title
     *         the title string
     */
    protected void setTitle(String title) {
        Preconditions.checkNotNull(title);
        if (parentWindow instanceof Dialog) {
            ((Dialog) parentWindow).setTitle(title);
        } else if (parentWindow instanceof Frame) {
            ((Frame) parentWindow).setTitle(title);
        }
    }

}
