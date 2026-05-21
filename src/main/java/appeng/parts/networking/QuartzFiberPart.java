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

package appeng.parts.networking;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.me.energy.IEnergyOverlayGridConnection;
import appeng.me.service.EnergyService;
import appeng.parts.AEBasePart;
import appeng.parts.PartModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class QuartzFiberPart extends AEBasePart {

    @PartModels
    private static final IPartModel MODELS = new PartModel(AppEng.makeId("part/quartz_fiber"));

    private final IManagedGridNode outerNode;

    public QuartzFiberPart(IPartItem<?> partItem) {
        super(partItem);
        this.getMainNode()
            .setIdlePowerUsage(0)
            .setFlags(GridFlags.CANNOT_CARRY)
            .addService(IEnergyOverlayGridConnection.class, this::getTheirEnergyServices);
        this.outerNode = GridHelper.createManagedNode(this, NodeListener.INSTANCE)
                                   .setTagName("outer")
                                   .setIdlePowerUsage(0)
                                   .setVisualRepresentation(partItem.asItem())
                                   .setFlags(GridFlags.CANNOT_CARRY)
                                   .setInWorldNode(true)
                                   .addService(IEnergyOverlayGridConnection.class, this::getOurEnergyServices);
    }

    private List<EnergyService> getOurEnergyServices() {
        var grid = Objects.requireNonNull(getMainNode().getGrid());
        return Collections.singletonList((EnergyService) grid.getEnergyService());
    }

    private List<EnergyService> getTheirEnergyServices() {
        var grid = Objects.requireNonNull(this.outerNode.getGrid());
        return Collections.singletonList((EnergyService) grid.getEnergyService());
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(6, 6, 10, 10, 10, 16);
    }

    @Override
    public void readFromNBT(NBTTagCompound extra) {
        super.readFromNBT(extra);
        this.outerNode.loadFromNBT(extra);
    }

    @Override
    public void writeToNBT(NBTTagCompound extra) {
        super.writeToNBT(extra);
        this.outerNode.saveToNBT(extra);
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        this.outerNode.destroy();
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.outerNode.create(getLevel(), getTileEntity().getPos());
    }

    @Override
    public void setPartHostInfo(EnumFacing side, IPartHost host, TileEntity blockEntity) {
        super.setPartHostInfo(side, host, blockEntity);
        this.outerNode.setExposedOnSides(EnumSet.of(side));
    }

    @Override
    public IGridNode getExternalFacingNode() {
        return this.outerNode.getNode();
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 16;
    }

    @Override
    public void onPlacement(EntityPlayer player) {
        super.onPlacement(player);
        this.outerNode.setOwningPlayer(player);
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS;
    }
}


