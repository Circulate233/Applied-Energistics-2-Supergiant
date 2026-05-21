/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.ISpatialStorageCell;
import appeng.core.AELog;
import appeng.core.localization.GuiText;
import appeng.items.AEBaseItem;
import appeng.spatial.SpatialStorageHelper;
import appeng.spatial.SpatialStoragePlot;
import appeng.spatial.SpatialStoragePlotManager;
import appeng.spatial.TransitionInfo;
import appeng.util.Platform;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

public class SpatialStorageCellItem extends AEBaseItem implements ISpatialStorageCell {
    private final int maxRegion;

    public SpatialStorageCellItem(int spatialScale) {
        this.maxRegion = spatialScale;
        this.setMaxStackSize(1);
    }

    @Override
    protected void addCheckedInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag advancedTooltips) {
        super.addCheckedInformation(stack, world, lines, advancedTooltips);

        SpatialPlotInfo plotInfo = getPlotInfo(stack);
        if (plotInfo == null) {
            lines.add(TextFormatting.ITALIC + GuiText.SpatialCellEmpty.getLocal());
            lines.add(GuiText.SpatialCapacity.getLocal(this.maxRegion, this.maxRegion, this.maxRegion));
            return;
        }

        String serialNumber = String.format(Locale.ROOT, "SP-%04d", plotInfo.id());
        BlockPos size = plotInfo.size();
        lines.add(GuiText.SpatialSerialNumber.getLocal(serialNumber));
        lines.add(GuiText.SpatialStoredSize.getLocal(size.getX(), size.getY(), size.getZ()));
    }

    @Override
    public boolean isSpatialStorage(ItemStack is) {
        return true;
    }

    @Override
    public int getMaxStoredDim(ItemStack is) {
        return this.maxRegion;
    }

    @Override
    public int getAllocatedPlotId(ItemStack stack) {
        SpatialPlotInfo plotInfo = getPlotInfo(stack);
        if (plotInfo != null) {
            try {
                if (SpatialStoragePlotManager.INSTANCE.getPlot(plotInfo.id()) == null) {
                    return -1;
                }
                return plotInfo.id();
            } catch (Exception e) {
                AELog.warn(e, String.format("Failed to retrieve spatial storage plot %s", plotInfo.id()));
            }
        }
        return -1;
    }

    @Override
    public boolean doSpatialTransition(ItemStack is, WorldServer level, BlockPos min, BlockPos max, int playerId) {
        final int targetX = max.getX() - min.getX() - 1;
        final int targetY = max.getY() - min.getY() - 1;
        final int targetZ = max.getZ() - min.getZ() - 1;
        final int maxSize = this.getMaxStoredDim(is);
        if (targetX > maxSize || targetY > maxSize || targetZ > maxSize) {
            AELog.info(
                "Failing spatial transition because the transfer area (%dx%dx%d) exceeds the cell capacity (%s).",
                targetX, targetY, targetZ, maxSize);
            return false;
        }

        final BlockPos targetSize = new BlockPos(targetX, targetY, targetZ);
        SpatialStoragePlotManager manager = SpatialStoragePlotManager.INSTANCE;

        int originalPlotId = this.getAllocatedPlotId(is);
        SpatialStoragePlot plot = manager.getPlot(originalPlotId);
        boolean createdPlot = false;

        if (plot != null) {
            if (!plot.getSize().equals(targetSize)) {
                AELog.info(
                    "Failing spatial transition because the transfer area (%dx%dx%d) does not match the spatial storage plot's size (%s).",
                    targetX, targetY, targetZ, plot.getSize());
                return false;
            }
        } else {
            plot = manager.allocatePlot(targetSize, playerId);
            createdPlot = true;
        }

        manager.setLastTransition(plot.getId(),
            new TransitionInfo(level.provider.getDimensionType().getName(),
                level.provider.getDimension(),
                min, max, Instant.now()));

        try {
            WorldServer cellLevel = manager.getLevel();
            BlockPos offset = plot.getOrigin();
            setStoredDimension(is, plot.getId(), plot.getSize());
            SpatialStorageHelper.getInstance().swapRegions(level, min.getX() + 1, min.getY() + 1, min.getZ() + 1,
                cellLevel,
                offset.getX(), offset.getY(), offset.getZ(), targetX - 1, targetY - 1, targetZ - 1);
            return true;
        } catch (Exception e) {
            if (createdPlot) {
                manager.freePlot(plot.getId(), true);
                clearStoredDimension(is);
            }
            throw e;
        }
    }

    public void setStoredDimension(ItemStack stack, int plotId, BlockPos size) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        AEComponents.SPATIAL_PLOT_INFO_COMPONENT.writeTo(tag, new SpatialPlotInfo(plotId, size).writeToNBT());
    }

    private void clearStoredDimension(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            tag.removeTag(AEComponents.SPATIAL_PLOT_INFO_COMPONENT.name());
            if (Platform.isNbtEmpty(tag)) {
                stack.setTagCompound(null);
            }
        }
    }

    private @Nullable SpatialPlotInfo getPlotInfo(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            NBTTagCompound plotInfo = AEComponents.SPATIAL_PLOT_INFO_COMPONENT.readFrom(tag);
            if (plotInfo != null) {
                return SpatialPlotInfo.fromNBT(plotInfo);
            }
        }
        return null;
    }
}
