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

package appeng.parts.reporting;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.implementations.blockentities.IViewCellStorage;
import appeng.api.inventories.InternalInventory;
import appeng.api.parts.IPartItem;
import appeng.api.storage.ILinkStatus;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.MEStorage;
import appeng.api.storage.SupplierStorage;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigManagerBuilder;
import appeng.api.util.KeyTypeSelection;
import appeng.api.util.KeyTypeSelectionHost;
import appeng.container.GuiIds;
import appeng.container.ISubGui;
import appeng.core.gui.GuiOpener;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.List;

public abstract class AbstractTerminalPart extends AbstractDisplayPart
    implements ITerminalHost, IViewCellStorage, InternalInventoryHost, KeyTypeSelectionHost {

    private final IConfigManager cm;
    private final KeyTypeSelection keyTypeSelection = new KeyTypeSelection(this::saveChanges, keyType -> true);
    private final AppEngInternalInventory viewCell = new AppEngInternalInventory(this, 5);

    public AbstractTerminalPart(IPartItem<?> partItem) {
        super(partItem, true);

        IConfigManagerBuilder builder = IConfigManager.builder(this::saveChanges);
        registerSettings(builder);
        this.cm = builder.build();
    }

    @MustBeInvokedByOverriders
    protected void registerSettings(IConfigManagerBuilder builder) {
        builder.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        builder.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        builder.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        for (ItemStack is : this.viewCell) {
            if (!is.isEmpty()) {
                drops.add(is);
            }
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.viewCell.clear();
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.cm;
    }

    public void saveChanges() {
        this.getHost().markForSave();
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        saveChanges();
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.cm.readFromNBT(data);
        this.keyTypeSelection.readFromNBT(data);
        this.viewCell.readFromNBT(data, "viewCell");
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        this.cm.writeToNBT(data);
        this.keyTypeSelection.writeToNBT(data);
        this.viewCell.writeToNBT(data, "viewCell");
    }

    @Override
    public boolean onUseWithoutItem(EntityPlayer player, Vec3d pos) {
        if (!super.onUseWithoutItem(player, pos) && !player.world.isRemote) {
            GuiOpener.openPartGui(player, getGuiKey(player), this);
        }
        return true;
    }

    @Override
    public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        GuiOpener.openPartGui(player, getGuiKey(player), this, true);
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return getPartItem().asItemStack();
    }

    public GuiIds.GuiKey getGuiKey(EntityPlayer player) {
        return GuiIds.GuiKey.ME_STORAGE_TERMINAL;
    }

    @Override
    public MEStorage getInventory() {
        return new SupplierStorage(() -> {
            var grid = getMainNode().getGrid();
            if (grid != null) {
                return grid.getStorageService().getInventory();
            }
            return null;
        });
    }

    @Override
    public ILinkStatus getLinkStatus() {
        return ILinkStatus.ofManagedNode(getMainNode());
    }

    @Override
    public InternalInventory getViewCellStorage() {
        return this.viewCell;
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        this.getHost().markForSave();
    }

    @Override
    public KeyTypeSelection getKeyTypeSelection() {
        return this.keyTypeSelection;
    }
}
