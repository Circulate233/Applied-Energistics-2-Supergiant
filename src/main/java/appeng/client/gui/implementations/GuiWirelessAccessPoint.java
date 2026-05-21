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
package appeng.client.gui.implementations;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.widgets.CommonButtons;
import appeng.container.implementations.ContainerWirelessAccessPoint;
import appeng.core.localization.GuiText;
import appeng.util.Platform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class GuiWirelessAccessPoint extends AEBaseGui<ContainerWirelessAccessPoint> {

    public GuiWirelessAccessPoint(ContainerWirelessAccessPoint container, InventoryPlayer playerInventory, ITextComponent title,
                                  GuiStyle style) {
        super(container, playerInventory, style);
        this.addToLeftToolbar(CommonButtons.togglePowerUnit());
        this.widgets.addBackgroundPanel("linkPanel");
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        ITextComponent rangeText = new TextComponentString("");
        ITextComponent energyUseText = new TextComponentString("");
        if (this.container.getRange() > 0) {
            double rangeBlocks = this.container.getRange() / 10.0;
            rangeText = GuiText.WirelessRange.text(rangeBlocks);
            energyUseText = GuiText.PowerUsageRate.text(Platform.formatPowerLong(this.container.getDrain(), true));
        }

        setTextContent("range", rangeText);
        setTextContent("energy_use", energyUseText);
    }
}

