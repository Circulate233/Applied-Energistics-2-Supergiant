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
package appeng.tile.storage;

import appeng.api.AECapabilities;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.implementations.blockentities.IViewCellStorage;
import appeng.api.implementations.blockentities.IMEChest;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.orientation.RelativeSide;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.ILinkStatus;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageCells;
import appeng.api.storage.StorageHelper;
import appeng.api.storage.SupplierStorage;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.StorageCell;
import appeng.api.util.AEColor;
import appeng.api.util.IConfigManager;
import appeng.api.util.KeyTypeSelection;
import appeng.api.util.KeyTypeSelectionHost;
import appeng.container.GuiIds;
import appeng.container.ISubGui;
import appeng.core.definitions.AEBlocks;
import appeng.core.gui.GuiOpener;
import appeng.helpers.IPriorityHost;
import appeng.me.helpers.MachineSource;
import appeng.me.storage.DelegatingMEInventory;
import appeng.tile.ServerTickingTile;
import appeng.tile.grid.AENetworkedPoweredTile;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

public class TileMEChest extends AENetworkedPoweredTile
    implements IMEChest, ITerminalHost, IPriorityHost, IColorableBlockEntity,
    IStorageProvider, ServerTickingTile, KeyTypeSelectionHost, IViewCellStorage {

    private final AppEngInternalInventory inputInventory = new AppEngInternalInventory(this, 1, 64,
        new InputInventoryFilter());
    private final AppEngInternalInventory cellInventory = new AppEngInternalInventory(this, 1, 1,
        new CellInventoryFilter());
    private final AppEngInternalInventory viewCellInventory = new AppEngInternalInventory(this, 5);
    private final InternalInventory internalInventory = new CombinedInternalInventory(this.inputInventory,
        this.cellInventory);
    private final MachineSource mySrc = new MachineSource(this);

    private final IConfigManager config = IConfigManager.builder(this::saveChanges)
                                                        .registerSetting(Settings.SORT_BY, SortOrder.NAME)
                                                        .registerSetting(Settings.VIEW_MODE, ViewItems.ALL)
                                                        .registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING)
                                                        .build();
    private final KeyTypeSelection keyTypeSelection = new KeyTypeSelection(this::saveChanges, TileMEChest::isVisibleKeyType);

    private int priority;
    private CellState clientCellState = CellState.ABSENT;
    private boolean clientPowered;
    private Item clientCellItem = Items.AIR;
    private boolean wasOnline;
    private AEColor paintedColor = AEColor.TRANSPARENT;
    private boolean isCached;
    private ChestMonitorHandler cellHandler;
    private IFluidHandler fluidHandler;
    private double idlePowerUsage;

    public TileMEChest() {
        setInternalMaxPower(PowerMultiplier.CONFIG.multiply(500));
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
        setInternalPublicPowerStorage(true);
        setInternalPowerFlow(AccessRestriction.WRITE);
    }

    private static boolean isVisibleKeyType(appeng.api.stacks.AEKeyType keyType) {
        return true;
    }

    @Override
    protected IManagedGridNode createMainNode() {
        return super.createMainNode()
                    .addService(IStorageProvider.class, this);
    }

    public ItemStack getCell() {
        return cellInventory.getStackInSlot(0);
    }

    public void setCell(ItemStack stack) {
        cellInventory.setItemDirect(0, stack);
    }

    @Override
    protected void emitPowerStateEvent(appeng.api.networking.events.GridPowerStorageStateChanged.PowerEventType x) {
        if (x == appeng.api.networking.events.GridPowerStorageStateChanged.PowerEventType.RECEIVE_POWER) {
            ifGridPresent(grid -> grid.postEvent(new appeng.api.networking.events.GridPowerStorageStateChanged(this, x)));
        } else {
            recalculateDisplay();
        }
    }

    private void recalculateDisplay() {
        var cellState = getCellStatus(0);
        var powered = isPowered();
        var cellItem = getResolvedCellItem();

        if (this.clientCellState != cellState
            || this.clientPowered != powered
            || this.clientCellItem != cellItem) {
            this.clientCellState = cellState;
            this.clientPowered = powered;
            this.clientCellItem = cellItem;
            markForUpdate();
        }
    }

    @Override
    public int getCellCount() {
        return 1;
    }

    private void updateHandler() {
        if (!this.isCached) {
            this.cellHandler = null;
            this.fluidHandler = null;
            this.idlePowerUsage = 0;
            this.getMainNode().setIdlePowerUsage(0);

            var cell = this.getCell();
            if (!cell.isEmpty()) {
                this.isCached = true;
                var newCell = StorageCells.getCellInventory(cell, this::onCellContentChanged);
                if (newCell != null) {
                    this.idlePowerUsage = 1.0 + newCell.getIdleDrain();
                    this.cellHandler = this.wrap(newCell);
                    this.getMainNode().setIdlePowerUsage(this.idlePowerUsage);
                    if (this.cellHandler != null) {
                        this.fluidHandler = new FluidHandler();
                    }
                }
            }
        }
    }

    @Nullable
    private ChestMonitorHandler wrap(StorageCell cellInventory) {
        return cellInventory != null ? new ChestMonitorHandler(this, cellInventory) : null;
    }

    @Override
    public ILinkStatus getLinkStatus() {
        updateHandler();
        if (this.cellHandler == null) {
            return ILinkStatus.ofDisconnected(new TextComponentString("Cannot read storage cell"));
        }
        if (!isPowered()) {
            return ILinkStatus.ofDisconnected(new TextComponentString("Out of power"));
        }
        return ILinkStatus.ofConnected();
    }

    @Override
    public CellState getCellStatus(int slot) {
        if (isClientSide()) {
            return clientCellState;
        }

        this.updateHandler();

        var cell = this.getCell();
        var handler = StorageCells.getHandler(cell);
        if (slot == 0 && this.cellHandler != null && handler != null) {
            return this.cellHandler.cellInventory.getStatus();
        }

        return CellState.ABSENT;
    }

    @Nullable
    @Override
    public Item getCellItem(int slot) {
        if (slot != 0) {
            return null;
        }
        if (isClientSide()) {
            return this.clientCellItem == Items.AIR ? null : this.clientCellItem;
        }
        var cellItem = getResolvedCellItem();
        return cellItem == Items.AIR ? null : cellItem;
    }

    @Nullable
    @Override
    public MEStorage getCellInventory(int slot) {
        if (slot == 0 && cellHandler != null) {
            return cellHandler;
        }
        return null;
    }

    @Nullable
    @Override
    public StorageCell getOriginalCellInventory(int slot) {
        if (slot == 0 && cellHandler != null) {
            return cellHandler.cellInventory;
        }
        return null;
    }

    @Override
    public boolean isPowered() {
        if (isClientSide()) {
            return this.clientPowered;
        }
        if (getMainNode().isPowered()) {
            return true;
        }
        return getAECurrentPower() > 1;
    }

    @Override
    public boolean isCellBlinking(int slot) {
        return false;
    }

    @Override
    protected double extractAEPower(double amt, Actionable mode) {
        double stash = 0.0;
        var grid = getMainNode().getGrid();
        if (grid != null) {
            var energy = grid.getEnergyService();
            stash = energy.extractAEPower(amt, mode, PowerMultiplier.ONE);
            if (stash >= amt) {
                return stash;
            }
        }
        return super.extractAEPower(amt - stash, mode) + stash;
    }

    @Override
    public void serverTick() {
        var grid = getMainNode().getGrid();
        if (grid == null || !grid.getEnergyService().isNetworkPowered()) {
            this.extractAEPower(idlePowerUsage, Actionable.MODULATE, PowerMultiplier.CONFIG);
            this.recalculateDisplay();
        }

        if (!this.inputInventory.isEmpty()) {
            this.tryToStoreContents();
        }
    }

    @Override
    protected void writeToStream(ByteBuf data) {
        super.writeToStream(data);
        this.clientCellState = getCellStatus(0);
        this.clientPowered = isPowered();
        this.clientCellItem = getResolvedCellItem();
        data.writeByte(this.clientCellState.ordinal());
        data.writeBoolean(this.clientPowered);
        data.writeByte(this.paintedColor.ordinal());
        data.writeInt(Item.getIdFromItem(this.clientCellItem));
    }

    @Override
    protected boolean readFromStream(ByteBuf data) {
        final boolean changed = super.readFromStream(data);
        var oldCellState = clientCellState;
        var oldPowered = clientPowered;
        var oldColor = paintedColor;
        var oldCellItem = clientCellItem;

        int stateOrdinal = data.readUnsignedByte();
        clientCellState = stateOrdinal >= 0 && stateOrdinal < CellState.values().length
            ? CellState.values()[stateOrdinal]
            : CellState.ABSENT;
        clientPowered = data.readBoolean();
        int colorOrdinal = data.readUnsignedByte();
        paintedColor = colorOrdinal >= 0 && colorOrdinal < AEColor.values().length
            ? AEColor.values()[colorOrdinal]
            : AEColor.TRANSPARENT;
        clientCellItem = Item.getItemById(data.readInt());
        if (clientCellItem == null) {
            clientCellItem = Items.AIR;
        }

        return changed || oldCellState != clientCellState || oldPowered != clientPowered
            || oldColor != paintedColor || oldCellItem != clientCellItem;
    }

    @Override
    protected void saveVisualState(NBTTagCompound data) {
        super.saveVisualState(data);
        data.setBoolean("powered", isPowered());
        data.setByte("cellState", (byte) getCellStatus(0).ordinal());
        data.setInteger("cellItem", Item.getIdFromItem(getResolvedCellItem()));
        data.setByte("paintedColor", (byte) this.paintedColor.ordinal());
    }

    @Override
    protected void loadVisualState(NBTTagCompound data) {
        super.loadVisualState(data);
        int stateOrdinal = data.getByte("cellState") & 0xFF;
        this.clientCellState = stateOrdinal < CellState.values().length
            ? CellState.values()[stateOrdinal]
            : CellState.ABSENT;
        this.clientPowered = data.getBoolean("powered");
        var item = Item.getItemById(data.getInteger("cellItem"));
        this.clientCellItem = item == null ? Items.AIR : item;
        if (data.hasKey("paintedColor")) {
            int colorOrdinal = data.getByte("paintedColor") & 0xFF;
            this.paintedColor = colorOrdinal < AEColor.values().length
                ? AEColor.values()[colorOrdinal]
                : AEColor.TRANSPARENT;
        }
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.config.readFromNBT(data);
        this.keyTypeSelection.readFromNBT(data);
        inputInventory.readFromNBT(data, "inputInventory");
        cellInventory.readFromNBT(data, "cellInventory");
        viewCellInventory.readFromNBT(data, "viewCellInventory");
        priority = data.getInteger("priority");
        if (data.hasKey("paintedColor")) {
            int colorOrdinal = data.getByte("paintedColor") & 0xFF;
            this.paintedColor = colorOrdinal < AEColor.values().length
                ? AEColor.values()[colorOrdinal]
                : AEColor.TRANSPARENT;
        }
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        this.config.writeToNBT(data);
        this.keyTypeSelection.writeToNBT(data);
        inputInventory.writeToNBT(data, "inputInventory");
        cellInventory.writeToNBT(data, "cellInventory");
        viewCellInventory.writeToNBT(data, "viewCellInventory");
        data.setInteger("priority", priority);
        data.setByte("paintedColor", (byte) this.paintedColor.ordinal());
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        var currentOnline = getMainNode().isOnline();
        if (wasOnline != currentOnline) {
            wasOnline = currentOnline;
            IStorageProvider.requestUpdate(getMainNode());
            recalculateDisplay();
        }
    }

    public MEStorage getInventory() {
        return new SupplierStorage(() -> {
            updateHandler();
            return TileMEChest.this.cellHandler;
        });
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.config;
    }

    @Override
    public KeyTypeSelection getKeyTypeSelection() {
        return this.keyTypeSelection;
    }

    @Override
    public InternalInventory getViewCellStorage() {
        return this.viewCellInventory;
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(AEBlocks.ME_CHEST.block());
    }

    public InternalInventory getInternalInventory() {
        return this.internalInventory;
    }

    @Override
    protected InternalInventory getExposedInventoryForSide(net.minecraft.util.EnumFacing side) {
        return side == this.getOrientation().getSide(RelativeSide.FRONT) ? this.cellInventory : this.inputInventory;
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        if (inv == this.cellInventory) {
            this.isCached = false;
            this.cellHandler = null;
            this.fluidHandler = null;
            this.idlePowerUsage = 0;
            this.getMainNode().setIdlePowerUsage(0);
            IStorageProvider.requestUpdate(getMainNode());
            this.markForUpdate();
        }
        if (inv == this.inputInventory && !inv.getStackInSlot(slot).isEmpty()) {
            this.tryToStoreContents();
        }
    }

    private void tryToStoreContents() {
        if (!this.inputInventory.isEmpty()) {
            this.updateHandler();
            if (this.cellHandler != null) {
                var stack = this.inputInventory.getStackInSlot(0);
                if (stack.isEmpty()) {
                    return;
                }

                var what = AEItemKey.of(stack);
                if (what == null) {
                    return;
                }

                var inserted = StorageHelper.poweredInsert(this, this.cellHandler, what, stack.getCount(), this.mySrc);
                if (inserted >= stack.getCount()) {
                    this.inputInventory.setItemDirect(0, ItemStack.EMPTY);
                } else {
                    stack.shrink((int) inserted);
                    this.inputInventory.setItemDirect(0, stack);
                }
            }
        }
    }

    @Override
    public void mountInventories(IStorageMounts storageMounts) {
        if (getMainNode().isOnline()) {
            updateHandler();
            if (cellHandler != null) {
                storageMounts.mount(cellHandler, priority);
            }
        }
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int newValue) {
        this.priority = newValue;
        this.cellHandler = null;
        this.isCached = false;
        IStorageProvider.requestUpdate(getMainNode());
        saveChanges();
    }

    private void blinkCell() {
        this.recalculateDisplay();
    }

    public boolean openGui(EntityPlayer player) {
        this.updateHandler();
        if (this.cellHandler != null && StorageCells.getHandler(this.getCell()) != null) {
            GuiOpener.openGui(player, GuiIds.GuiKey.BASIC_CELL_CHEST, this);
            return true;
        }
        return false;
    }

    public void openCellInventoryGui(EntityPlayer player) {
        GuiOpener.openGui(player, GuiIds.GuiKey.ME_CHEST, this);
    }

    @Override
    public AEColor getColor() {
        return this.paintedColor;
    }

    @Override
    public boolean recolourBlock(EnumFacing side, AEColor newPaintedColor, EntityPlayer who) {
        if (this.paintedColor == newPaintedColor) {
            return false;
        }

        this.paintedColor = newPaintedColor;
        this.saveChanges();
        this.markForUpdate();
        return true;
    }

    private void onCellContentChanged() {
        if (this.cellHandler != null) {
            this.cellHandler.cellInventory.persist();
        }
        this.saveChanges();
    }

    @Nullable
    public IFluidHandler getFluidHandler(@Nullable EnumFacing side) {
        this.updateHandler();
        return side != this.getOrientation().getSide(RelativeSide.FRONT) ? this.fluidHandler : null;
    }

    @Nullable
    public MEStorage getMEStorage(@Nullable EnumFacing side) {
        return side != this.getOrientation().getSide(RelativeSide.FRONT) ? this.getInventory() : null;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == AECapabilities.ME_STORAGE && this.getMEStorage(facing) != null) {
            return true;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && this.getFluidHandler(facing) != null) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == AECapabilities.ME_STORAGE) {
            return (T) this.getMEStorage(facing);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) this.getFluidHandler(facing);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return new ItemStack(AEBlocks.ME_CHEST.block());
    }

    @Override
    public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        GuiOpener.openGui(player, GuiIds.GuiKey.ME_CHEST, this, true);
    }

    private Item getResolvedCellItem() {
        var cell = this.getCell();
        return cell.isEmpty() ? Items.AIR : cell.getItem();
    }

    private static class ChestMonitorHandler extends DelegatingMEInventory {
        private final TileMEChest owner;
        private final StorageCell cellInventory;

        ChestMonitorHandler(TileMEChest owner, StorageCell cellInventory) {
            super(cellInventory);
            this.owner = owner;
            this.cellInventory = cellInventory;
        }

        @Override
        public long insert(appeng.api.stacks.AEKey what, long amount, Actionable mode,
                           appeng.api.networking.security.IActionSource source) {
            var inserted = super.insert(what, amount, mode, source);
            if (inserted > 0 && mode == Actionable.MODULATE) {
                owner.blinkCell();
            }
            return inserted;
        }

        @Override
        public long extract(appeng.api.stacks.AEKey what, long amount, Actionable mode,
                            appeng.api.networking.security.IActionSource source) {
            var extracted = super.extract(what, amount, mode, source);
            if (extracted > 0 && mode == Actionable.MODULATE) {
                owner.blinkCell();
            }
            return extracted;
        }
    }

    private static class CellInventoryFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return StorageCells.getHandler(stack) != null;
        }
    }

    private class FluidHandler implements IFluidHandler {
        private static final IFluidTankProperties[] EMPTY_TANKS = new IFluidTankProperties[0];

        private boolean canAcceptLiquids() {
            return TileMEChest.this.cellHandler != null;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            if (!canAcceptLiquids()) {
                return EMPTY_TANKS;
            }
            return new IFluidTankProperties[]{
                new FluidTankProperties(null, Fluid.BUCKET_VOLUME, true, false)
            };
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            TileMEChest.this.updateHandler();
            if (resource == null || resource.amount <= 0 || !canAcceptLiquids()) {
                return 0;
            }

            var what = AEFluidKey.of(resource);
            if (what == null) {
                return 0;
            }

            return (int) StorageHelper.poweredInsert(TileMEChest.this,
                TileMEChest.this.cellHandler,
                what,
                resource.amount,
                TileMEChest.this.mySrc,
                Actionable.ofSimulate(!doFill));
        }

        @Override
        public @org.jspecify.annotations.Nullable FluidStack drain(FluidStack resource, boolean doDrain) {
            return null;
        }

        @Override
        public @org.jspecify.annotations.Nullable FluidStack drain(int maxDrain, boolean doDrain) {
            return null;
        }
    }

    private class InputInventoryFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return false;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            if (isPowered()) {
                updateHandler();
                if (cellHandler == null) {
                    return false;
                }

                var what = AEItemKey.of(stack);
                if (what == null) {
                    return false;
                }

                return cellHandler.insert(what, stack.getCount(), Actionable.SIMULATE, mySrc) > 0;
            }
            return false;
        }
    }
}
