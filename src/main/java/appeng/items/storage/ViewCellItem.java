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

package appeng.items.storage;

import appeng.api.config.FuzzyMode;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.AEKeyFilter;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import appeng.items.AEBaseItem;
import appeng.items.contents.CellConfig;
import appeng.util.ConfigInventory;
import appeng.util.prioritylist.FuzzyPriorityList;
import appeng.util.prioritylist.IPartitionList;
import appeng.util.prioritylist.MergedPriorityList;
import appeng.util.prioritylist.PrecisePriorityList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;

import java.util.Collection;

public class ViewCellItem extends AEBaseItem implements ICellWorkbenchItem {
    public ViewCellItem() {
        this.setMaxStackSize(1);
    }

    public static IPartitionList createItemFilter(Collection<ItemStack> list) {
        return createFilter(AEItemKey.filter(), list);
    }

    public static IPartitionList createFilter(AEKeyFilter filter, Collection<ItemStack> list) {
        IPartitionList partitionList = null;
        MergedPriorityList mergedList = new MergedPriorityList();

        for (ItemStack currentViewCell : list) {
            if (currentViewCell == null) {
                continue;
            }

            if (currentViewCell.getItem() instanceof ViewCellItem vc) {
                KeyCounter priorityList = new KeyCounter();

                ConfigInventory config = vc.getConfigInventory(currentViewCell);
                FuzzyMode fuzzyMode = vc.getFuzzyMode(currentViewCell);

                for (int i = 0; i < config.size(); i++) {
                    var what = config.getKey(i);
                    if (what != null && filter.matches(what)) {
                        priorityList.add(what, 1);
                    }
                }

                if (!priorityList.isEmpty()) {
                    IUpgradeInventory upgrades = vc.getUpgrades(currentViewCell);
                    boolean hasInverter = upgrades.isInstalled(AEItems.INVERTER_CARD.asItem());
                    if (upgrades.isInstalled(AEItems.FUZZY_CARD.asItem())) {
                        mergedList.addNewList(new FuzzyPriorityList(priorityList, fuzzyMode), !hasInverter);
                    } else {
                        mergedList.addNewList(new PrecisePriorityList(priorityList), !hasInverter);
                    }

                    partitionList = mergedList;
                }
            }
        }

        return partitionList;
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack is) {
        return UpgradeInventories.forItem(is, 2);
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack is) {
        return CellConfig.create(is);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack is) {
        NBTTagCompound tag = is.getTagCompound();
        if (tag != null) {
            try {
                var value = AEComponents.STORAGE_CELL_FUZZY_MODE_COMPONENT.readFrom(tag);
                if (value != null) {
                    return FuzzyMode.valueOf(value.getString());
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            is.setTagCompound(tag);
        }
        AEComponents.STORAGE_CELL_FUZZY_MODE_COMPONENT.writeTo(tag, new NBTTagString(fzMode.name()));
    }
}
