/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
 */
package appeng.items.contents;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.features.HotkeyAction;
import appeng.api.implementations.blockentities.IViewCellStorage;
import appeng.api.implementations.guiobjects.IPortableTerminal;
import appeng.api.implementations.guiobjects.ItemGuiHost;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.ILinkStatus;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageCells;
import appeng.api.storage.StorageHelper;
import appeng.api.storage.SupplierStorage;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.api.util.IConfigManager;
import appeng.container.ISubGui;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.core.localization.GuiText;
import appeng.items.tools.powered.AbstractPortableCell;
import appeng.me.helpers.PlayerSource;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.SupplierInternalInventory;
import com.google.common.base.Preconditions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class PortableCellGuiHost<T extends AbstractPortableCell> extends ItemGuiHost<T>
    implements IPortableTerminal, IViewCellStorage {
    private static final String VIEW_CELL_TAG = "viewCell";

    private final BiConsumer<EntityPlayer, ISubGui> returnMainContainer;
    private final MEStorage cellStorage;
    private final AbstractPortableCell item;
    private final IConfigManager configManager;
    private final SupplierInternalInventory<InternalInventory> viewCellStorage;
    private ILinkStatus linkStatus = ILinkStatus.ofDisconnected();

    public PortableCellGuiHost(T item, EntityPlayer player, ItemGuiHostLocator locator,
                               BiConsumer<EntityPlayer, ISubGui> returnMainContainer) {
        super(item, player, locator);
        Preconditions.checkArgument(getItemStack().getItem() == item, "Stack doesn't match item");
        this.returnMainContainer = returnMainContainer;
        this.cellStorage = new SupplierStorage(new CellStorageSupplier());
        Objects.requireNonNull(cellStorage, "Portable cell doesn't expose a cell inventory.");
        this.item = item;
        this.viewCellStorage = new SupplierInternalInventory<>(
            new StackDependentSupplier<>(this::getItemStack, stack -> createViewCellStorage(player, stack)));
        this.updateLinkStatus();
        this.configManager = IConfigManager.builder(this::getItemStack)
                                           .registerSetting(Settings.SORT_BY, SortOrder.NAME)
                                           .registerSetting(Settings.VIEW_MODE, ViewItems.ALL)
                                           .registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING)
                                           .build();
    }

    private static InternalInventory createViewCellStorage(EntityPlayer player, ItemStack stack) {
        var viewCellStorage = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inv) {
                NBTTagCompound tag = stack.getTagCompound();
                if (tag == null) {
                    tag = new NBTTagCompound();
                    stack.setTagCompound(tag);
                }
                inv.writeToNBT(tag, VIEW_CELL_TAG);
            }

            @Override
            public boolean isClientSide() {
                return player.world.isRemote;
            }
        }, 5);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            viewCellStorage.readFromNBT(tag, VIEW_CELL_TAG);
        }
        return viewCellStorage;
    }

    @Override
    public void tick() {
        super.tick();
        consumeIdlePower(Actionable.MODULATE);
        updateLinkStatus();
    }

    @Override
    public long insert(EntityPlayer player, AEKey what, long amount, Actionable mode) {
        if (getLinkStatus().connected()) {
            var inv = getInventory();
            return inv == null ? 0 : StorageHelper.poweredInsert(this, inv, what, amount, new PlayerSource(player), mode);
        } else {
            var statusText = getLinkStatus().statusDescription();
            if (isClientSide() && statusText != null && !mode.isSimulate()) {
                player.sendStatusMessage(statusText, false);
            }
            return 0;
        }
    }

    private void updateLinkStatus() {
        if (!consumeIdlePower(Actionable.SIMULATE)) {
            this.linkStatus = ILinkStatus.ofDisconnected(GuiText.OutOfPower.text());
        } else {
            this.linkStatus = ILinkStatus.ofConnected();
        }
    }

    @Override
    public ILinkStatus getLinkStatus() {
        return linkStatus;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        amt = usePowerMultiplier.multiply(amt);
        if (mode == Actionable.SIMULATE) {
            return usePowerMultiplier.divide(Math.min(amt, this.item.getAECurrentPower(getItemStack())));
        }
        return usePowerMultiplier.divide(this.item.extractAEPower(getItemStack(), amt, Actionable.MODULATE));
    }

    @Override
    public MEStorage getInventory() {
        return cellStorage;
    }

    @Override
    public IConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public InternalInventory getViewCellStorage() {
        return this.viewCellStorage;
    }

    @Override
    public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        returnMainContainer.accept(player, subGui);
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return getItemStack();
    }

    @Override
    public String getCloseHotkey() {
        if (item instanceof IBasicCellItem cellItem) {
            if (cellItem.getKeyType().equals(AEKeyType.items())) {
                return HotkeyAction.PORTABLE_ITEM_CELL;
            } else if (cellItem.getKeyType().equals(AEKeyType.fluids())) {
                return HotkeyAction.PORTABLE_FLUID_CELL;
            }
        }
        return null;
    }

    private class CellStorageSupplier implements Supplier<MEStorage> {
        private MEStorage currentStorage;
        private ItemStack currentStack = ItemStack.EMPTY;

        @Override
        public MEStorage get() {
            var stack = getItemStack();
            if (stack != currentStack) {
                currentStorage = StorageCells.getCellInventory(stack, null);
                currentStack = stack;
            }
            return currentStorage;
        }
    }
}





