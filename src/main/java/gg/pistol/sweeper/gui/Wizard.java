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

import gg.pistol.lumberjack.JackLogger;
import gg.pistol.lumberjack.JackLoggerFactory;
import gg.pistol.sweeper.core.Sweeper;
import gg.pistol.sweeper.core.SweeperException;
import gg.pistol.sweeper.core.SweeperImpl;
import gg.pistol.sweeper.gui.WizardPage.WizardPageListener;
import gg.pistol.sweeper.gui.component.BasicDialog;
import gg.pistol.sweeper.gui.component.DecoratedPanel;
import gg.pistol.sweeper.gui.component.DynamicPanel;
import gg.pistol.sweeper.gui.component.MessageDialog;
import gg.pistol.sweeper.gui.component.WebBrowserLauncher;
import gg.pistol.sweeper.i18n.I18n;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * The main application dialog that will guide the user into cleaning the files/directories.
 *
 * @author Bogdan Pistol
 */
public class Wizard implements WizardPageListener {

    private static final String HELP_URL = "https://github.com/bogdanu/sweeper";
    private static final String BUTTON_GROUP = "buttons";

    private static final JackLogger LOG = JackLoggerFactory.getLogger(LoggerFactory.getLogger(Wizard.class));

    private final I18n i18n;
    private final Sweeper sweeper;
    private final Window window;
    private final WebBrowserLauncher webBrowserLauncher;

    private WizardPage currentPage;
    @Nullable private JButton cancelButton;
    @Nullable private JButton backButton;
    @Nullable private JButton nextButton;
    @Nullable private JButton finishButton;
    @Nullable private JButton closeButton;
    @Nullable private JPanel languagePanel;

    private Wizard(I18n i18n) throws SweeperException {
        this.i18n = i18n;
        sweeper = new SweeperImpl();
        webBrowserLauncher = new WebBrowserLauncher(i18n);

        currentPage = new WelcomePage(i18n, this, sweeper);
        window = new BasicDialog(null, createWizardPanel());
        window.setVisible(true);
    }

    private DynamicPanel createWizardPanel() {
        return new DecoratedPanel(i18n, false, null) {
            @Override
            protected void addComponents(JPanel contentPanel) {
                contentPanel.setLayout(new BorderLayout());
                setTitle(i18n.getString(I18n.WIZARD_TITLE_ID, I18n.APPLICATION_NAME));
                contentPanel.add(currentPage, BorderLayout.CENTER);

                languagePanel = createHorizontalPanel();
                contentPanel.add(languagePanel, BorderLayout.NORTH);
                languagePanel.add(Box.createHorizontalGlue());
                languagePanel.add(new JLabel(i18n.getString(I18n.WIZARD_CHANGE_LANGUAGE_ID)));

                JPanel southPanel = new JPanel(new BorderLayout());
                contentPanel.add(southPanel, BorderLayout.SOUTH);
                southPanel.add(new JSeparator(), BorderLayout.NORTH);

                JPanel buttons = createHorizontalPanel();
                southPanel.add(buttons, BorderLayout.SOUTH);
                buttons.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));

                buttons.add(createButton(i18n.getString(I18n.WIZARD_BUTTON_HELP_ID), helpAction(), BUTTON_GROUP));
                buttons.add(Box.createHorizontalStrut(5));
                buttons.add(createButton(i18n.getString(I18n.WIZARD_BUTTON_ABOUT_ID), aboutAction(), BUTTON_GROUP));
                buttons.add(Box.createHorizontalGlue());
                buttons.add(Box.createHorizontalStrut(40));

                buttons.add(cancelButton = createButton(i18n.getString(I18n.BUTTON_CANCEL_ID), cancelAction(), BUTTON_GROUP));
                buttons.add(Box.createHorizontalStrut(10));
                buttons.add(backButton = createButton(i18n.getString(I18n.WIZARD_BUTTON_BACK_ID), backAction(), BUTTON_GROUP));
                buttons.add(Box.createHorizontalStrut(5));
                buttons.add(nextButton = createButton(i18n.getString(I18n.WIZARD_BUTTON_NEXT_ID), nextAction(), BUTTON_GROUP));
                buttons.add(Box.createHorizontalStrut(10));
                buttons.add(finishButton = createButton(i18n.getString(I18n.WIZARD_BUTTON_FINISH_ID), finishAction(), BUTTON_GROUP));
                buttons.add(closeButton = createButton(i18n.getString(I18n.BUTTON_CLOSE_ID), closeAction(), BUTTON_GROUP));

                onButtonStateChange();
            }
        };
    }

    private ActionListener helpAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                webBrowserLauncher.openWebBrowser(HELP_URL, window);
            }
        };
    }

    private ActionListener aboutAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new About(window, i18n, webBrowserLauncher);
            }
        };
    }

    private ActionListener cancelAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentPage.cancel();
                onButtonStateChange();
            }
        };
    }

    private ActionListener backAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updatePage(currentPage.back());
            }
        };
    }

    private ActionListener nextAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updatePage(currentPage.next());
            }
        };
    }

    private ActionListener finishAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updatePage(currentPage.finish());
            }
        };
    }

    private void updatePage(WizardPage page) {
        Preconditions.checkState(page != null);
        currentPage = page;
        onButtonStateChange();
    }

    public void onButtonStateChange() {
        Preconditions.checkState(cancelButton != null);
        Preconditions.checkState(backButton != null);
        Preconditions.checkState(nextButton != null);
        Preconditions.checkState(finishButton != null);

        cancelButton.setVisible(currentPage.isCancelButtonVisible());
        cancelButton.setEnabled(currentPage.isCancelButtonEnabled());
        backButton.setEnabled(currentPage.isBackButtonEnabled());
        nextButton.setEnabled(currentPage.isNextButtonEnabled());
        finishButton.setEnabled(currentPage.isFinishButtonEnabled());

        finishButton.setVisible(!currentPage.isLastPage());
        closeButton.setVisible(currentPage.isLastPage());
        languagePanel.setVisible(currentPage.isLanguageSelectorVisible());
    }

    /**
     * Open the wizard window.
     */
    public static void open() {
        LOG.info("Opening the wizard.");
        final I18n i18n = new I18n();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    new Wizard(i18n);
                } catch(SweeperException e) {
                    LOG.error("Exception thrown while opening the wizard:", e);
                    new MessageDialog(null, MessageDialog.Type.ERROR, i18n, i18n.getString(I18n.LABEL_ERROR_ID),
                            i18n.getString(I18n.WIZARD_ERROR_OPEN_ID, e.getLocalizedMessage()));
                }
            }
        });
    }

}
