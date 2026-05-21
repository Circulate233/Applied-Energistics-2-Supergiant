/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.client.gui.implementations;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.widgets.CommonButtons;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.container.implementations.ContainerSpatialAnchor;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class GuiSpatialAnchor extends AEBaseGui<ContainerSpatialAnchor> {

    private final SettingToggleButton<YesNo> overlayToggle;

    public GuiSpatialAnchor(ContainerSpatialAnchor container, InventoryPlayer playerInventory, ITextComponent title,
                            GuiStyle style) {
        super(container, playerInventory, style);
        this.addToLeftToolbar(CommonButtons.togglePowerUnit());
        this.overlayToggle = this.addToLeftToolbar(new ServerSettingToggleButton<>(Settings.OVERLAY_MODE, YesNo.NO));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.overlayToggle.set(this.container.overlayMode);
        if (this.container.getGuiTitle() == null) {
            setTextContent("dialog_title", new TextComponentTranslation("gui.ae2.SpatialAnchor"));
        }
        setTextContent("used_power", new TextComponentTranslation("gui.ae2.SpatialAnchorUsedPower",
            Platform.formatPowerLong(this.container.powerConsumption * 100, true)));
        setTextContent("loaded_chunks",
            new TextComponentTranslation("gui.ae2.SpatialAnchorLoadedChunks", this.container.loadedChunks));
        setTextContent("statistics_title", new TextComponentTranslation("gui.ae2.SpatialAnchorStatistics"));
        setTextContent("statistics_loaded", new TextComponentTranslation("gui.ae2.SpatialAnchorAllLoaded",
            this.container.allLoadedChunks, this.container.allLoadedWorlds));
        setTextContent("statistics_total",
            new TextComponentTranslation("gui.ae2.SpatialAnchorAll", this.container.allChunks, this.container.allWorlds));
    }
}
