/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
 */
package appeng.helpers;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.features.HotkeyAction;
import appeng.api.implementations.blockentities.IViewCellStorage;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.implementations.guiobjects.IPortableTerminal;
import appeng.api.implementations.guiobjects.ItemGuiHost;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEKey;
import appeng.api.storage.ILinkStatus;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.storage.SupplierStorage;
import appeng.api.util.IConfigManager;
import appeng.api.util.KeyTypeSelection;
import appeng.api.util.KeyTypeSelectionHost;
import appeng.container.ISubGui;
import appeng.core.AEConfig;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.items.contents.StackDependentSupplier;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.me.helpers.PlayerSource;
import appeng.me.storage.NullInventory;
import appeng.tile.networking.TileWirelessAccessPoint;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.SupplierInternalInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class WirelessTerminalGuiHost<T extends WirelessTerminalItem> extends ItemGuiHost<T>
    implements IPortableTerminal, IActionHost, KeyTypeSelectionHost, IViewCellStorage, InternalInventoryHost {

    private static final String VIEW_CELL_TAG = "viewCell";

    private final BiConsumer<EntityPlayer, ISubGui> returnToMainContainer;
    private final MEStorage storage;
    private final SupplierInternalInventory<InternalInventory> viewCellStorage;
    protected double currentDistanceFromGrid = Double.MAX_VALUE;
    protected double currentRemainingRange = Double.MIN_VALUE;
    @Nullable
    private IWirelessAccessPoint currentAccessPoint;
    private ILinkStatus linkStatus = ILinkStatus.ofDisconnected();

    public WirelessTerminalGuiHost(T item, EntityPlayer player, ItemGuiHostLocator locator,
                                   BiConsumer<EntityPlayer, ISubGui> returnToMainContainer) {
        super(item, player, locator);
        this.returnToMainContainer = returnToMainContainer;
        this.storage = new SupplierStorage(new StackDependentSupplier<>(this::getItemStack, this::getStorageFromStack));
        this.viewCellStorage = new SupplierInternalInventory<>(
            new StackDependentSupplier<>(this::getItemStack, stack -> createViewCellStorage(player, stack)));
        updateConnectedAccessPoint();
        updateLinkStatus();
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
    public ILinkStatus getLinkStatus() {
        return linkStatus;
    }

    @Nullable
    private MEStorage getStorageFromStack(ItemStack stack) {
        var targetGrid = getLinkedGrid(stack);
        return targetGrid != null ? targetGrid.getStorageService().getInventory() : NullInventory.of();
    }

    @Nullable
    private IGrid getLinkedGrid(ItemStack stack) {
        return getItem().getLinkedGrid(stack, getPlayer().world, null);
    }

    @Override
    public MEStorage getInventory() {
        return this.storage;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        final double extracted = Math.min(amt, getItem().getAECurrentPower(getItemStack()));
        if (mode == Actionable.SIMULATE) {
            return extracted;
        }
        return getItem().usePower(getPlayer(), extracted, getItemStack()) ? extracted : 0;
    }

    @Override
    public IConfigManager getConfigManager() {
        return getItem().getConfigManager(this::getItemStack);
    }

    @Override
    public KeyTypeSelection getKeyTypeSelection() {
        return KeyTypeSelection.forStack(getItemStack(), ignored -> true);
    }

    @Override
    public InternalInventory getViewCellStorage() {
        return this.viewCellStorage;
    }

    @Override
    public IGridNode getActionableNode() {
        return this.currentAccessPoint != null ? this.currentAccessPoint.getActionableNode() : null;
    }

    protected void updateConnectedAccessPoint() {
        this.currentAccessPoint = null;
        this.currentDistanceFromGrid = Double.MAX_VALUE;
        this.currentRemainingRange = Double.MIN_VALUE;

        var targetGrid = getLinkedGrid(getItemStack());
        if (targetGrid != null) {
            IWirelessAccessPoint bestWap = null;
            double bestSqDistance = Double.MAX_VALUE;
            double bestSqRemainingRange = Double.MIN_VALUE;

            for (var wap : targetGrid.getMachines(TileWirelessAccessPoint.class)) {
                var signal = getAccessPointSignal(wap);
                if (signal.distanceSquared < bestSqDistance) {
                    bestSqDistance = signal.distanceSquared;
                    bestWap = wap;
                }
                if (signal.remainingRangeSquared > bestSqRemainingRange) {
                    bestSqRemainingRange = signal.remainingRangeSquared;
                }
            }

            this.currentAccessPoint = bestWap;
            this.currentDistanceFromGrid = Math.sqrt(bestSqDistance);
            this.currentRemainingRange = Math.sqrt(bestSqRemainingRange);
        }
    }

    protected AccessPointSignal getAccessPointSignal(IWirelessAccessPoint wap) {
        double rangeLimit = wap.getRange();
        rangeLimit *= rangeLimit;

        var dc = wap.getLocation();
        if (dc.getLevel() == this.getPlayer().world) {
            var offX = dc.getPos().getX() - this.getPlayer().posX;
            var offY = dc.getPos().getY() - this.getPlayer().posY;
            var offZ = dc.getPos().getZ() - this.getPlayer().posZ;
            double r = offX * offX + offY * offY + offZ * offZ;
            if (r < rangeLimit && wap.isActive()) {
                return new AccessPointSignal(r, rangeLimit - r);
            }
        }

        return new AccessPointSignal(Double.MAX_VALUE, Double.MIN_VALUE);
    }

    @Override
    public void tick() {
        updateConnectedAccessPoint();
        consumeIdlePower(Actionable.MODULATE);
        updateLinkStatus();
    }

    protected void updateLinkStatus() {
        if (!consumeIdlePower(Actionable.SIMULATE)) {
            this.linkStatus = ILinkStatus.ofDisconnected(GuiText.OutOfPower.text());
        } else if (currentAccessPoint != null) {
            this.linkStatus = ILinkStatus.ofConnected();
        } else {
            MutableObject<ITextComponent> errorHolder = new MutableObject<>();
            if (getItem().getLinkedGrid(getItemStack(), getPlayer().world, errorHolder::setValue) == null) {
                this.linkStatus = ILinkStatus.ofDisconnected(errorHolder.get());
            } else {
                this.linkStatus = ILinkStatus.ofDisconnected(PlayerMessages.OutOfRange.text());
            }
        }
    }

    @Override
    protected double getPowerDrainPerTick() {
        if (currentAccessPoint != null && currentDistanceFromGrid < Double.MAX_VALUE) {
            return AEConfig.instance().wireless_getDrainRate(currentDistanceFromGrid);
        }
        return 0.0;
    }

    @Override
    public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        returnToMainContainer.accept(player, subGui);
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return getItemStack();
    }

    @Override
    public String getCloseHotkey() {
        return HotkeyAction.WIRELESS_TERMINAL;
    }

    @Override
    public long insert(EntityPlayer player, AEKey what, long amount, Actionable mode) {
        if (isClientSide()) {
            return 0;
        }
        if (getLinkStatus().connected()) {
            return StorageHelper.poweredInsert(this, getInventory(), what, amount, new PlayerSource(player), mode);
        } else {
            var statusText = getLinkStatus().statusDescription();
            if (statusText != null && !mode.isSimulate()) {
                player.sendStatusMessage(statusText, false);
            }
            return 0;
        }
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
    }

    protected static final class AccessPointSignal {
        final double distanceSquared;
        final double remainingRangeSquared;

        AccessPointSignal(double distanceSquared, double remainingRangeSquared) {
            this.distanceSquared = distanceSquared;
            this.remainingRangeSquared = remainingRangeSquared;
        }
    }
}





