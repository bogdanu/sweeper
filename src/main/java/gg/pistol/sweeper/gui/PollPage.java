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

import com.google.common.base.Preconditions;
import gg.pistol.sweeper.core.Sweeper;
import gg.pistol.sweeper.i18n.I18n;

import javax.annotation.Nullable;
import javax.swing.*;

// package private
class PollPage extends WizardPage {

    private final int number;

    @Nullable private PollPage prevPage;

    PollPage(I18n i18n, WizardPageListener listener, Sweeper sweeper, int number, @Nullable PollPage prevPage) {
        super(Preconditions.checkNotNull(i18n), Preconditions.checkNotNull(listener), Preconditions.checkNotNull(sweeper));
        Preconditions.checkArgument(number >= 0);

        this.number = number;
        this.prevPage = prevPage;
    }

    @Override
    protected void addComponents(JPanel contentPanel) {
        Preconditions.checkNotNull(contentPanel);
        super.addComponents(contentPanel);

        contentPanel.add(alignLeft(createLabel(i18n.getString(I18n.PAGE_POLL_DESCRIPTION))));
        contentPanel.add(createVerticalStrut(20));

        contentPanel.add(alignLeft(createLabel(i18n.getString(I18n.PAGE_POLL_STAT_ANALYSED,
                formatInt(sweeper.getCount().getTotalTargets()), formatSize(sweeper.getCount().getTotalSize()),
                formatInt(sweeper.getCount().getDuplicateTargets()), formatSize(sweeper.getCount().getDuplicateSize())))));
        contentPanel.add(createVerticalStrut(20));

        contentPanel.add(alignLeft(createLabel(i18n.getString(I18n.PAGE_POLL_STAT_DELETE,
                formatInt(sweeper.getCount().getToDeleteTargets()), formatSize(sweeper.getCount().getToDeleteSize())))));
    }

    private String formatSize(long size) {
        int gb = (int) (size >> 30);
        int mb = (int) (size >> 20 & 0x3FF);
        int kb = (int) (size >> 10 & 0x3FF);
        int bytes = (int) (size & 0x3FF);

        if (gb > 0) {
            return i18n.getString(I18n.SIZE_DESCRIPTION_GB_ID, formatInt(gb), formatInt(mb));
        }
        if (mb > 0) {
            return i18n.getString(I18n.SIZE_DESCRIPTION_MB_ID, formatInt(mb));
        }
        if (kb > 0) {
            return i18n.getString(I18n.SIZE_DESCRIPTION_KB_ID, formatInt(kb));
        }
        return i18n.getString(I18n.SIZE_DESCRIPTION_BYTE_ID, formatInt(bytes));
    }

    private String formatInt(int val) {
        return String.format(i18n.getLocale(), "%1$,d", val);
    }

    @Override
    protected String getPageHeader() {
        return i18n.getString(I18n.PAGE_POLL_HEADER_ID);
    }

    @Override
    boolean isCancelButtonVisible() {
        return false;
    }

    @Override
    boolean isCancelButtonEnabled() {
        return false;
    }

    @Override
    boolean isBackButtonEnabled() {
        return number > 1;
    }

    @Override
    boolean isNextButtonEnabled() {
        return true;
    }

    @Override
    boolean isFinishButtonEnabled() {
        return true;
    }

    @Override
    boolean isLastPage() {
        return false;
    }

    @Override
    boolean isLanguageSelectorVisible() {
        return true;
    }

    @Override
    void cancel() {
    }

    @Override
    @Nullable
    WizardPage back() {
        if (sweeper.previousPoll() == null) {
            return null;
        }
        freePrevPage();
        PollPage page = new PollPage(i18n, listener, sweeper, number - 1, this);
        page.setParentWindow(getParentWindow());
        return page;
    }

    @Override
    WizardPage next() {
        freePrevPage();
        WizardPage page;
        if (sweeper.nextPoll() != null) {
            page = new PollPage(i18n, listener, sweeper, number + 1, this);
            page.setParentWindow(getParentWindow());
        } else {
            page = null;
        }
        return page;
    }

    @Override
    WizardPage finish() {
        return null;
    }

    private void freePrevPage() {
        if (prevPage != null) {
            prevPage.setParentWindow(null);
            prevPage = null;
        }
    }
}
