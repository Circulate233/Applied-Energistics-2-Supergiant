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

package appeng.tile.crafting;

import appeng.api.ids.AEBlockIds;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridMultiblock;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.orientation.BlockOrientation;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.me.cluster.IAEMultiBlock;
import appeng.me.cluster.implementations.CraftingCPUCalculator;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.tile.grid.AENetworkedTile;
import appeng.util.NullConfigManager;
import appeng.util.Platform;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TileCraftingUnit extends AENetworkedTile
    implements IAEMultiBlock<CraftingCPUCluster>, IPowerChannelState, IConfigurableObject {

    private static final long KILOBYTE = 1024L;

    private final CraftingCPUCalculator calc = new CraftingCPUCalculator(this);
    private NBTTagCompound previousState;
    private boolean coreBlock;
    private CraftingCPUCluster cluster;

    public TileCraftingUnit() {
        this.getMainNode().setFlags(GridFlags.MULTIBLOCK, GridFlags.REQUIRE_CHANNEL)
            .addService(IGridMultiblock.class, this::getMultiblockNodes);
    }

    @Override
    public ItemStack getItemFromTile() {
        if (this.world == null) {
            return ItemStack.EMPTY;
        }

        Block block = this.world.getBlockState(this.pos).getBlock();
        Item item = Item.getItemFromBlock(block);
        return item == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    public void setName(String name) {
        this.setCustomName(name);
        if (this.cluster != null) {
            this.cluster.updateName();
        }
    }

    @Nullable
    private ResourceLocation getCraftingBlockId() {
        if (this.world == null) {
            return null;
        }
        return this.world.getBlockState(this.pos).getBlock().getRegistryName();
    }

    public long getStorageBytes() {
        ResourceLocation id = getCraftingBlockId();
        if (AEBlockIds.CRAFTING_STORAGE_1K.equals(id)) {
            return KILOBYTE;
        } else if (AEBlockIds.CRAFTING_STORAGE_4K.equals(id)) {
            return 4 * KILOBYTE;
        } else if (AEBlockIds.CRAFTING_STORAGE_16K.equals(id)) {
            return 16 * KILOBYTE;
        } else if (AEBlockIds.CRAFTING_STORAGE_64K.equals(id)) {
            return 64 * KILOBYTE;
        } else if (AEBlockIds.CRAFTING_STORAGE_256K.equals(id)) {
            return 256 * KILOBYTE;
        }
        return 0;
    }

    public int getAcceleratorThreads() {
        return AEBlockIds.CRAFTING_ACCELERATOR.equals(getCraftingBlockId()) ? 1 : 0;
    }

    @Override
    public void onReady() {
        super.onReady();
        this.getMainNode().setVisualRepresentation(this.getItemFromTile());
        if (this.world != null && !this.world.isRemote) {
            this.calc.calculateMultiblock(this.world, this.pos);
        }
    }

    public void updateMultiBlock(BlockPos changedPos) {
        if (this.world != null && !this.world.isRemote) {
            this.calc.updateMultiblockAfterNeighborUpdate(this.world, this.pos, changedPos);
        }
    }

    public void updateStatus(@Nullable CraftingCPUCluster cluster) {
        if (this.cluster != null && this.cluster != cluster) {
            this.cluster.breakCluster();
        }

        this.cluster = cluster;
        this.updateSubType(true);
    }

    public void updateSubType(boolean updateFormed) {
        if (this.world == null || this.isInvalid()) {
            return;
        }

        final boolean formed = this.isFormed();
        boolean power = this.getMainNode().isOnline();

        final IBlockState current = this.world.getBlockState(this.pos);
        IBlockState newState = setBooleanProperty(current, "powered", power);
        newState = setBooleanProperty(newState, "formed", formed);

        if (current != newState) {
            this.world.setBlockState(this.pos, newState, 2);
        }

        if (updateFormed) {
            onGridConnectableSidesChanged();
        }
    }

    private IBlockState setBooleanProperty(IBlockState state, String propertyName, boolean value) {
        for (IProperty<?> property : state.getPropertyKeys()) {
            if (property instanceof PropertyBool boolProperty && property.getName().equals(propertyName)) {
                return state.withProperty(boolProperty, value);
            }
        }
        return state;
    }

    @Nullable
    private Boolean getBooleanProperty(IBlockState state, String propertyName) {
        for (IProperty<?> property : state.getPropertyKeys()) {
            if (property instanceof PropertyBool boolProperty && property.getName().equals(propertyName)) {
                return state.getValue(boolProperty);
            }
        }
        return null;
    }

    @Override
    public Set<EnumFacing> getGridConnectableSides(BlockOrientation orientation) {
        if (isFormed()) {
            return EnumSet.allOf(EnumFacing.class);
        }
        return EnumSet.noneOf(EnumFacing.class);
    }

    public boolean isFormed() {
        if (this.world != null && this.world.isRemote) {
            Boolean formed = getBooleanProperty(this.world.getBlockState(this.pos), "formed");
            return formed != null && formed;
        }
        return this.cluster != null;
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        data.setBoolean("core", this.isCoreBlock());
        if (this.isCoreBlock() && this.cluster != null) {
            this.cluster.writeToNBT(data);
        }
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.setCoreBlock(data.getBoolean("core"));
        if (this.isCoreBlock()) {
            if (this.cluster != null) {
                this.cluster.readFromNBT(data);
            } else {
                this.setPreviousState(data.copy());
            }
        }
    }

    @Override
    public void disconnect(boolean update) {
        if (this.cluster != null) {
            this.cluster.destroy();
            if (update) {
                this.updateSubType(true);
            }
        }
    }

    @Override
    public CraftingCPUCluster getCluster() {
        return this.cluster;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        if (reason != IGridNodeListener.State.GRID_BOOT) {
            this.updateSubType(false);
        }
    }

    public void breakCluster() {
        if (this.cluster == null) {
            return;
        }

        this.cluster.cancelJob();
        var inv = this.cluster.craftingLogic.getInventory();

        if (this.world == null) {
            inv.clear();
            this.cluster.destroy();
            return;
        }

        var places = new ObjectArrayList<BlockPos>();
        Iterator<TileCraftingUnit> it = this.cluster.getBlockEntities();
        while (it.hasNext()) {
            TileCraftingUnit blockEntity = it.next();
            if (this == blockEntity) {
                places.add(this.pos);
            } else {
                for (EnumFacing facing : EnumFacing.values()) {
                    BlockPos place = blockEntity.pos.offset(facing);
                    if (this.world.isAirBlock(place)) {
                        places.add(place);
                    }
                }
            }
        }

        if (places.isEmpty()) {
            throw new IllegalStateException(this.cluster + " does not contain any kind of blocks, which were destroyed.");
        }

        for (var entry : inv.list) {
            var position = places.get(this.world.rand.nextInt(places.size()));
            var stacks = new ObjectArrayList<ItemStack>();
            entry.getKey().addDrops(entry.getLongValue(), stacks, this.world, position);
            Platform.spawnDrops(this.world, position, stacks);
        }

        inv.clear();
        this.cluster.destroy();
    }

    @Override
    public boolean isPowered() {
        if (this.world != null && this.world.isRemote) {
            Boolean powered = getBooleanProperty(this.world.getBlockState(this.pos), "powered");
            return powered != null && powered;
        }
        return this.getMainNode().isActive();
    }

    @Override
    public boolean isActive() {
        if (this.world == null || !this.world.isRemote) {
            return this.getMainNode().isActive();
        }
        return this.isPowered() && this.isFormed();
    }

    public boolean isCoreBlock() {
        return this.coreBlock;
    }

    public void setCoreBlock(boolean coreBlock) {
        this.coreBlock = coreBlock;
    }

    @Nullable
    public NBTTagCompound getPreviousState() {
        return this.previousState;
    }

    public void setPreviousState(@Nullable NBTTagCompound previousState) {
        this.previousState = previousState;
    }

    private Iterator<IGridNode> getMultiblockNodes() {
        if (this.getCluster() == null) {
            return java.util.Collections.emptyIterator();
        }

        List<IGridNode> nodes = new ObjectArrayList<>();
        Iterator<TileCraftingUnit> it = this.getCluster().getBlockEntities();
        while (it.hasNext()) {
            var node = it.next().getGridNode();
            if (node != null) {
                nodes.add(node);
            }
        }

        return nodes.iterator();
    }

    @Override
    public IConfigManager getConfigManager() {
        var cluster = this.getCluster();
        if (cluster != null) {
            return cluster.getConfigManager();
        }
        return NullConfigManager.INSTANCE;
    }
}
