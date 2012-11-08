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
import gg.pistol.sweeper.gui.component.MessageDialog.MessageType;
import gg.pistol.sweeper.gui.component.SwingMessages;
import gg.pistol.sweeper.gui.component.WebBrowserLauncher;
import gg.pistol.sweeper.i18n.I18n;

import java.awt.BorderLayout;
import java.awt.Dimension;
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

    @Nullable private JPanel pageContainer;
    @Nullable private WizardPage currentPage;
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

        window = new BasicDialog(null, createWizardPanel());
        window.setVisible(true);
    }

    private DynamicPanel createWizardPanel() {
        return new DecoratedPanel(i18n, false, null) {
            @Override
            protected void addComponents(JPanel contentPanel) {
                Preconditions.checkNotNull(contentPanel);
                contentPanel.setLayout(new BorderLayout());
                setTitle(i18n.getString(I18n.WIZARD_TITLE_ID, I18n.APPLICATION_NAME));

                pageContainer = createHorizontalPanel();
                contentPanel.add(pageContainer, BorderLayout.CENTER);
                pageContainer.add(getCurrentPage());
                getCurrentPage().update();

                JPanel northPanel = createHorizontalPanel();
                contentPanel.add(northPanel, BorderLayout.NORTH);

                languagePanel = createHorizontalPanel();
                northPanel.add(languagePanel);
                languagePanel.add(Box.createHorizontalGlue());
                languagePanel.add(new JLabel(i18n.getString(I18n.WIZARD_CHANGE_LANGUAGE_ID)));
                languagePanel.add(createHorizontalStrut(8));
                languagePanel.add(createLanguageSelector(130));

                northPanel.setPreferredSize(new Dimension(northPanel.getPreferredSize().width, languagePanel.getPreferredSize().height));

                JPanel southPanel = new JPanel(new BorderLayout());
                contentPanel.add(southPanel, BorderLayout.SOUTH);
                southPanel.add(new JSeparator(), BorderLayout.NORTH);

                JPanel buttons = createHorizontalPanel();
                southPanel.add(buttons, BorderLayout.SOUTH);
                buttons.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));

                buttons.add(createButton(i18n.getString(I18n.BUTTON_HELP_ID), helpAction(), BUTTON_GROUP));
                buttons.add(createHorizontalStrut(5));
                buttons.add(createButton(i18n.getString(I18n.BUTTON_ABOUT_ID), aboutAction(), BUTTON_GROUP));
                buttons.add(Box.createHorizontalGlue());
                buttons.add(createHorizontalStrut(40));

                buttons.add(cancelButton = createButton(i18n.getString(I18n.BUTTON_CANCEL_ID), cancelAction(), BUTTON_GROUP));
                buttons.add(createHorizontalStrut(10));
                buttons.add(backButton = createButton(i18n.getString(I18n.WIZARD_BUTTON_BACK_ID), backAction(), BUTTON_GROUP));
                buttons.add(createHorizontalStrut(5));
                buttons.add(nextButton = createButton(i18n.getString(I18n.WIZARD_BUTTON_NEXT_ID), nextAction(), BUTTON_GROUP));
                buttons.add(createHorizontalStrut(10));
                buttons.add(finishButton = createButton(i18n.getString(I18n.BUTTON_FINISH_ID), finishAction(), BUTTON_GROUP));
                buttons.add(closeButton = createButton(i18n.getString(I18n.BUTTON_CLOSE_ID), closeAction(), BUTTON_GROUP));

                onButtonStateChange();
            }
        };
    }

    private WizardPage getCurrentPage() {
        if (currentPage == null) {
            currentPage = new WelcomePage(i18n, this, sweeper);
        }
        return currentPage;
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
                Preconditions.checkState(currentPage != null);
                currentPage.cancel();
                onButtonStateChange();
            }
        };
    }

    private ActionListener backAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Preconditions.checkState(currentPage != null);
                updatePage(currentPage.back());
            }
        };
    }

    private ActionListener nextAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Preconditions.checkState(currentPage != null);
                updatePage(currentPage.next());
            }
        };
    }

    private ActionListener finishAction() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Preconditions.checkState(currentPage != null);
                updatePage(currentPage.finish());
            }
        };
    }

    private void updatePage(WizardPage page) {
        Preconditions.checkState(pageContainer != null);
        Preconditions.checkState(currentPage != null);
        Preconditions.checkState(page != null);

        pageContainer.remove(currentPage);
        currentPage = page;
        pageContainer.add(currentPage);
        currentPage.update();
        onButtonStateChange();
        window.pack();
    }

    public void onButtonStateChange() {
        Preconditions.checkState(currentPage != null);
        Preconditions.checkState(cancelButton != null);
        Preconditions.checkState(backButton != null);
        Preconditions.checkState(nextButton != null);
        Preconditions.checkState(finishButton != null);
        Preconditions.checkState(closeButton != null);
        Preconditions.checkState(languagePanel != null);

        cancelButton.setVisible(currentPage.isCancelButtonVisible());
        cancelButton.setEnabled(currentPage.isCancelButtonEnabled());
        backButton.setEnabled(currentPage.isBackButtonEnabled());
        nextButton.setEnabled(currentPage.isNextButtonEnabled());
        finishButton.setEnabled(currentPage.isFinishButtonEnabled());

        finishButton.setVisible(!currentPage.isLastPage());
        closeButton.setVisible(currentPage.isLastPage());
        languagePanel.setVisible(currentPage.isLanguageSelectorVisible());
    }

    public Window getWindow() {
        return window;
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
                    i18n.registerListener(new SwingMessages(i18n));
                    new Wizard(i18n);
                } catch (Exception e) {
                    LOG.error("Exception thrown while opening the wizard:", e);
                    new MessageDialog(null, MessageType.ERROR, i18n, i18n.getString(I18n.LABEL_ERROR_ID),
                            i18n.getString(I18n.WIZARD_ERROR_OPEN_ID), e.getLocalizedMessage());
                }
            }
        });
    }

}
