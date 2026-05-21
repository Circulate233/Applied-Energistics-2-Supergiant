/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.gui.me.common;

import appeng.client.component.TextComponents;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.Icon;
import appeng.client.gui.style.GuiStyleManager;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.TabButton;
import appeng.container.me.common.ContainerMEStorage;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import appeng.integration.abstraction.ItemListMod;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class GuiTerminalSettings<C extends ContainerMEStorage> extends AEBaseGui<C> {
    private final GuiMEStorage<C> parent;
    private final AECheckbox pinAutoCraftedItemsCheckbox;
    private final AECheckbox notifyForFinishedCraftingJobsCheckbox;
    private final AECheckbox clearGridOnCloseCheckbox;
    private final AECheckbox useInternalSearchRadio;
    private final AECheckbox useExternalSearchRadio;
    private final AECheckbox rememberCheckbox;
    private final AECheckbox autoFocusCheckbox;
    private final AECheckbox syncWithExternalCheckbox;
    private final AECheckbox clearExternalCheckbox;

    public GuiTerminalSettings(GuiMEStorage<C> parent) {
        super(parent.getContainer(), parent.getContainer().getPlayerInventory(),
            GuiStyleManager.loadStyleDoc("/screens/terminals/terminal_settings.json"));
        this.parent = parent;

        ITextComponent externalSearchName = new TextComponentString(
            ItemListMod.isEnabled() ? ItemListMod.getShortName() : "HEI");

        widgets.add("back", new TabButton(Icon.BACK, TextComponents.of(parent.getContainer().getHost().getMainContainerIcon()),
            this::returnToParent));

        this.pinAutoCraftedItemsCheckbox = widgets.addCheckbox("pinAutoCraftedItemsCheckbox",
            GuiText.TerminalSettingsPinAutoCraftedItems.text(), this::save);
        this.notifyForFinishedCraftingJobsCheckbox = widgets.addCheckbox("notifyForFinishedCraftingJobsCheckbox",
            GuiText.TerminalSettingsNotifyForFinishedJobs.text(), this::save);
        this.clearGridOnCloseCheckbox = widgets.addCheckbox("clearGridOnCloseCheckbox",
            GuiText.TerminalSettingsClearGridOnClose.text(), this::save);
        this.useInternalSearchRadio = widgets.addCheckbox("useInternalSearchRadio",
            GuiText.SearchSettingsUseInternalSearch.text(), this::switchToAeSearch);
        this.useInternalSearchRadio.setRadio(true);
        this.useExternalSearchRadio = widgets.addCheckbox("useExternalSearchRadio",
            GuiText.SearchSettingsUseExternalSearch.text(externalSearchName), this::switchToExternalSearch);
        this.useExternalSearchRadio.setRadio(true);
        this.rememberCheckbox = widgets.addCheckbox("rememberCheckbox", GuiText.SearchSettingsRememberSearch.text(),
            this::save);
        this.autoFocusCheckbox = widgets.addCheckbox("autoFocusCheckbox", GuiText.SearchSettingsAutoFocus.text(),
            this::save);
        this.syncWithExternalCheckbox = widgets.addCheckbox("syncWithExternalCheckbox",
            GuiText.SearchSettingsSyncWithExternal.text(externalSearchName), this::save);
        this.clearExternalCheckbox = widgets.addCheckbox("clearExternalCheckbox",
            GuiText.SearchSettingsClearExternal.text(externalSearchName), this::save);

        setTextContent(TEXT_ID_DIALOG_TITLE, GuiText.TerminalSettingsTitle.text());
        setTextContent("search_settings_title", GuiText.SearchSettingsTitle.text());
        updateState();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        updateState();
    }

    private void returnToParent() {
        this.parent.onCloseTerminalSettings();
        switchToScreen(parent);
        parent.returnFromSubScreen(this);
    }

    private void switchToAeSearch() {
        this.useInternalSearchRadio.setSelected(true);
        this.useExternalSearchRadio.setSelected(false);
        save();
    }

    private void switchToExternalSearch() {
        if (!hasExternalSearch()) {
            this.useInternalSearchRadio.setSelected(true);
            this.useExternalSearchRadio.setSelected(false);
            updateState();
            return;
        }
        this.useInternalSearchRadio.setSelected(false);
        this.useExternalSearchRadio.setSelected(true);
        save();
    }

    private void save() {
        AEConfig config = AEConfig.instance();
        config.setPinAutoCraftedItems(pinAutoCraftedItemsCheckbox.isSelected());
        config.setNotifyForFinishedCraftingJobs(notifyForFinishedCraftingJobsCheckbox.isSelected());
        config.setClearGridOnClose(clearGridOnCloseCheckbox.isSelected());
        config.setUseExternalSearch(hasExternalSearch() && this.useExternalSearchRadio.isSelected());
        config.setRememberLastSearch(this.rememberCheckbox.isSelected());
        config.setAutoFocusSearch(this.autoFocusCheckbox.isSelected());
        config.setSyncWithExternalSearch(this.syncWithExternalCheckbox.isSelected());
        config.setClearExternalSearchOnOpen(this.clearExternalCheckbox.isSelected());
        updateState();
    }

    private void updateState() {
        AEConfig config = AEConfig.instance();
        pinAutoCraftedItemsCheckbox.setSelected(config.isPinAutoCraftedItems());
        notifyForFinishedCraftingJobsCheckbox.setSelected(config.isNotifyForFinishedCraftingJobs());
        clearGridOnCloseCheckbox.setSelected(config.isClearGridOnClose());
        boolean hasExternalSearch = hasExternalSearch();
        if (!hasExternalSearch && config.isUseExternalSearch()) {
            config.setUseExternalSearch(false);
        }
        boolean useExternalSearch = hasExternalSearch && config.isUseExternalSearch();
        this.useInternalSearchRadio.setSelected(!useExternalSearch);
        this.useExternalSearchRadio.setSelected(useExternalSearch);
        this.useExternalSearchRadio.enabled = hasExternalSearch;
        this.rememberCheckbox.setSelected(config.isRememberLastSearch());
        this.autoFocusCheckbox.setSelected(config.isAutoFocusSearch());
        this.syncWithExternalCheckbox.setSelected(config.isSyncWithExternalSearch());
        this.syncWithExternalCheckbox.enabled = hasExternalSearch;
        this.clearExternalCheckbox.setSelected(config.isClearExternalSearchOnOpen());
        this.clearExternalCheckbox.enabled = hasExternalSearch;
        this.rememberCheckbox.visible = this.useInternalSearchRadio.isSelected();
        this.autoFocusCheckbox.visible = this.useInternalSearchRadio.isSelected();
        this.syncWithExternalCheckbox.visible = this.useInternalSearchRadio.isSelected();
        this.clearExternalCheckbox.visible = this.useExternalSearchRadio.isSelected();
    }

    private boolean hasExternalSearch() {
        return ItemListMod.isEnabled();
    }
}
