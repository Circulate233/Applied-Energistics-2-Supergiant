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

import appeng.api.config.FullnessMode;
import appeng.api.config.OperationMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.container.implementations.ContainerIOPort;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

public class GuiIOPort extends GuiUpgradeable<ContainerIOPort> {

    private final SettingToggleButton<FullnessMode> fullMode;
    private final SettingToggleButton<OperationMode> operationMode;
    private final SettingToggleButton<RedstoneMode> redstoneMode;

    public GuiIOPort(ContainerIOPort container, InventoryPlayer playerInventory, ITextComponent title, GuiStyle style) {
        super(container, playerInventory, title, style);

        this.fullMode = addToLeftToolbar(new ServerSettingToggleButton<>(Settings.FULLNESS_MODE, FullnessMode.EMPTY));
        this.redstoneMode = addToLeftToolbar(
            new ServerSettingToggleButton<>(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE));
        this.operationMode = new ServerSettingToggleButton<>(Settings.OPERATION_MODE, OperationMode.EMPTY);
        this.widgets.add("operationMode", this.operationMode);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.redstoneMode.set(this.container.getRedStoneMode());
        this.redstoneMode.setVisibility(this.container.hasUpgrade(AEItems.REDSTONE_CARD.item()));
        this.operationMode.set(this.container.getOperationMode());
        this.fullMode.set(this.container.getFullMode());
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY, partialTicks);

        this.drawItem(offsetX + 66 - 8, offsetY + 17, AEItems.ITEM_CELL_1K.stack());
        this.drawItem(offsetX + 94 + 8, offsetY + 17, AEBlocks.DRIVE.stack());
    }

    private void drawItem(int x, int y, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        RenderItem renderItem = minecraft.getRenderItem();
        GlStateManager.pushMatrix();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        renderItem.renderItemAndEffectIntoGUI(stack, x, y);
        renderItem.renderItemOverlayIntoGUI(minecraft.fontRenderer, stack, x, y, null);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();
    }
}
