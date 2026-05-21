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

package appeng.parts.misc;

import appeng.api.networking.IGridNode;
import appeng.api.parts.BusSupport;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public class CableAnchorPart implements IPart {

    @PartModels
    public static final PartModel DEFAULT_MODELS = new PartModel(false, AppEng.makeId("part/cable_anchor"));

    @PartModels
    public static final PartModel FACADE_MODELS = new PartModel(false, AppEng.makeId("part/cable_anchor_short"));

    private final IPartItem<CableAnchorPart> partItem;
    private IPartHost host;
    private EnumFacing side = EnumFacing.UP;

    public CableAnchorPart(IPartItem<CableAnchorPart> partItem) {
        this.partItem = partItem;
    }

    @Override
    public IPartItem<?> getPartItem() {
        return this.partItem;
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        if (this.host != null && this.host.getFacadeContainer().getFacade(this.side) != null) {
            bch.addBox(7, 7, 10, 9, 9, 14);
        } else {
            bch.addBox(7, 7, 10, 9, 9, 16);
        }
    }

    @Override
    public boolean isLadder(EntityLivingBase entity) {
        return this.side.getYOffset() == 0 && (entity.collidedHorizontally || !entity.onGround);
    }

    @Override
    public IGridNode getGridNode() {
        return null;
    }

    @Override
    public void setPartHostInfo(EnumFacing side, IPartHost host, TileEntity blockEntity) {
        this.host = host;
        this.side = side;
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 0;
    }

    @Override
    public boolean canBePlacedOn(BusSupport what) {
        return what == BusSupport.CABLE || what == BusSupport.DENSE_CABLE;
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.host != null && this.host.getFacadeContainer().getFacade(this.side) != null) {
            return FACADE_MODELS;
        }
        return DEFAULT_MODELS;
    }
}
