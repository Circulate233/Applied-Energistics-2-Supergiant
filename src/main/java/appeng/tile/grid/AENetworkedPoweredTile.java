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

package appeng.tile.grid;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.api.implementations.blockentities.ICrankable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.me.helpers.IGridConnectedTile;
import appeng.me.helpers.TileNodeListener;
import appeng.tile.powersink.AEBasePoweredTile;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class AENetworkedPoweredTile extends AEBasePoweredTile implements IGridConnectedTile {

    protected static final int POWER_PER_CRANK_TURN = 160;

    private final IManagedGridNode mainNode;

    public AENetworkedPoweredTile() {
        super();
        this.mainNode = createMainNode()
            .setVisualRepresentation(getItemFromTile())
            .setInWorldNode(true)
            .setTagName("proxy");
        onGridConnectableSidesChanged();
    }

    protected IManagedGridNode createMainNode() {
        return GridHelper.createManagedNode(this, TileNodeListener.INSTANCE)
                         .addService(IAEPowerStorage.class, this);
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.getMainNode().loadFromNBT(data);
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        this.getMainNode().saveToNBT(data);
    }

    @Override
    public final IManagedGridNode getMainNode() {
        return this.mainNode;
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return AECableType.SMART;
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.getMainNode().destroy();
    }

    @Override
    public void onReady() {
        super.onReady();
        this.getMainNode().create(getWorld(), getPos());
        this.refreshBlockStateAfterReady();
    }

    @Override
    protected void onOrientationChanged(BlockOrientation orientation) {
        super.onOrientationChanged(orientation);
        onGridConnectableSidesChanged();
    }

    protected final void onGridConnectableSidesChanged() {
        getMainNode().setExposedOnSides(getGridConnectableSides(getOrientation()));
    }

    @Nullable
    protected ICrankable getCrankable(@Nullable EnumFacing side) {
        return null;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == AECapabilities.CRANKABLE && this.getCrankable(facing) != null) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == AECapabilities.CRANKABLE) {
            return (T) this.getCrankable(facing);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.getMainNode().destroy();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        scheduleInit();
    }

    public final class Crankable implements ICrankable {
        @Override
        public boolean canTurn() {
            return getInternalCurrentPower() < getInternalMaxPower();
        }

        @Override
        public void applyTurn() {
            injectExternalPower(PowerUnit.AE, POWER_PER_CRANK_TURN, Actionable.MODULATE);
            markForUpdate();
        }
    }
}
