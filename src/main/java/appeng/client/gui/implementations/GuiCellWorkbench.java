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

import appeng.api.config.ActionItems;
import appeng.api.config.CopyMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.AEKeySlotFilter;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.client.gui.Icon;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.container.implementations.ContainerCellWorkbench;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.util.ConfigInventory;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public class GuiCellWorkbench extends GuiUpgradeable<ContainerCellWorkbench> {
    private final ToggleButton copyMode;
    private final SettingToggleButton<FuzzyMode> fuzzyMode;

    public GuiCellWorkbench(ContainerCellWorkbench container, InventoryPlayer playerInventory, ITextComponent title,
                            GuiStyle style) {
        super(container, playerInventory, title, style);

        this.fuzzyMode = addToLeftToolbar(
            new SettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL, this::toggleFuzzyMode));
        this.addToLeftToolbar(new ActionButton(ActionItems.COG, act -> container.partition()));
        this.addToLeftToolbar(new ActionButton(ActionItems.CLOSE, act -> container.clear()));
        this.copyMode = this.addToLeftToolbar(new ToggleButton(Icon.COPY_MODE_ON, Icon.COPY_MODE_OFF,
            GuiText.CopyMode.text(), GuiText.CopyModeDesc.text(), act -> container.nextWorkBenchCopyMode()));
    }

    private static void addIncompatibleWithCellTooltip(List<String> lines) {
        lines.add(GuiText.IncompatibleWithCell.text()
                                              .setStyle(new Style().setColor(TextFormatting.RED))
                                              .getFormattedText());
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.copyMode.setState(this.container.getCopyMode() == CopyMode.CLEAR_ON_REMOVE);
        this.fuzzyMode.set(this.container.getFuzzyMode());
        this.fuzzyMode.setVisibility(container.getUpgrades().isInstalled(AEItems.FUZZY_CARD.item()));
    }

    private void toggleFuzzyMode(SettingToggleButton<FuzzyMode> button, boolean backwards) {
        container.setCellFuzzyMode(button.getNextValue(backwards));
    }

    @Override
    public List<String> getItemToolTip(ItemStack stack) {
        List<String> lines = new ObjectArrayList<>(super.getItemToolTip(stack));
        ItemStack cell = this.container.getWorkbenchItem();
        if (cell.isEmpty() || cell == stack) {
            return lines;
        }

        ICellWorkbenchItem workbenchItem = this.container.getHost().getCell();
        if (workbenchItem == null) {
            return lines;
        }

        AEKey what;
        GenericStack genericStack = GenericStack.unwrapItemStack(stack);
        if (genericStack != null) {
            what = genericStack.what();
        } else {
            what = AEItemKey.of(stack);
        }

        if (what == null) {
            return lines;
        }

        ConfigInventory configInventory = workbenchItem.getConfigInventory(cell);
        if (!configInventory.isSupportedType(what.getType())) {
            addIncompatibleWithCellTooltip(lines);
            return lines;
        }

        AEKeySlotFilter filter = configInventory.getFilter();
        if (filter != null) {
            boolean anySlotMatches = false;
            for (int i = 0; i < configInventory.size(); i++) {
                if (configInventory.isAllowedIn(i, what)) {
                    anySlotMatches = true;
                    break;
                }
            }

            if (!anySlotMatches) {
                addIncompatibleWithCellTooltip(lines);
            }
        }

        return lines;
    }

}
