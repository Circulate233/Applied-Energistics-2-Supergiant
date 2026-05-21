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

import appeng.api.networking.GridHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.AECableType;
import appeng.me.helpers.IGridConnectedTile;
import appeng.me.helpers.TileNodeListener;
import appeng.tile.AEBaseInvTile;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

public class AENetworkedInvTile extends AEBaseInvTile implements IGridConnectedTile {

    private final IManagedGridNode mainNode;

    public AENetworkedInvTile() {
        super();
        this.mainNode = createMainNode()
            .setVisualRepresentation(getItemFromTile())
            .setInWorldNode(true)
            .setTagName("proxy");
        onGridConnectableSidesChanged();
    }

    protected IManagedGridNode createMainNode() {
        return GridHelper.createManagedNode(this, TileNodeListener.INSTANCE);
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
}
