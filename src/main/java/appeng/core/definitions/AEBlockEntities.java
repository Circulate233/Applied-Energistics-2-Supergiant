/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.core.definitions;

import appeng.api.ids.AEBlockIds;
import appeng.core.AppEng;
import appeng.core.Tags;
import appeng.debug.TileCubeGenerator;
import appeng.debug.TileEnergyGenerator;
import appeng.debug.TileItemGen;
import appeng.debug.TilePhantomNode;
import appeng.tile.crafting.TileCraftingMonitor;
import appeng.tile.crafting.TileCraftingUnit;
import appeng.tile.crafting.TileMolecularAssembler;
import appeng.tile.crafting.TilePatternProvider;
import appeng.tile.misc.TileCaner;
import appeng.tile.misc.TileCellWorkbench;
import appeng.tile.misc.TileCharger;
import appeng.tile.misc.TileCondenser;
import appeng.tile.misc.TileCrank;
import appeng.tile.misc.TileCrystalAssembler;
import appeng.tile.misc.TileCrystalFixer;
import appeng.tile.misc.TileGrowthAccelerator;
import appeng.tile.misc.TileIngredientBuffer;
import appeng.tile.misc.TileInscriber;
import appeng.tile.misc.TileInterface;
import appeng.tile.misc.TileLightDetector;
import appeng.tile.misc.TileMysteriousCube;
import appeng.tile.misc.TilePaint;
import appeng.tile.misc.TileVibrationChamber;
import appeng.tile.networking.TileCableBus;
import appeng.tile.networking.TileController;
import appeng.tile.networking.TileCreativeEnergyCell;
import appeng.tile.networking.TileCrystalResonanceGenerator;
import appeng.tile.networking.TileEnergyAcceptor;
import appeng.tile.networking.TileEnergyCell;
import appeng.tile.networking.TileWirelessAccessPoint;
import appeng.tile.qnb.TileQuantumBridge;
import appeng.tile.spatial.TileSpatialAnchor;
import appeng.tile.spatial.TileSpatialIOPort;
import appeng.tile.spatial.TileSpatialPylon;
import appeng.tile.storage.TileDrive;
import appeng.tile.storage.TileIOPort;
import appeng.tile.storage.TileMEChest;
import appeng.tile.storage.TileSkyChest;
import appeng.tile.storage.TileSkyStoneTank;
import net.minecraftforge.fml.common.registry.GameRegistry;

@SuppressWarnings("deprecation")
public final class AEBlockEntities {

    public static final TileDefinition<TileCableBus> CABLE_BUS = new TileDefinition<>(AppEng.makeId("cable_bus"),
        TileCableBus.class);
    public static final TileDefinition<TileQuantumBridge> QUANTUM_BRIDGE = new TileDefinition<>(
        AppEng.makeId("quantum_ring"), TileQuantumBridge.class);
    public static final TileDefinition<TileMysteriousCube> MYSTERIOUS_CUBE = new TileDefinition<>(
        AEBlockIds.MYSTERIOUS_CUBE, TileMysteriousCube.class);
    public static final TileDefinition<TileSkyStoneTank> SKY_STONE_TANK = new TileDefinition<>(
        AEBlockIds.SKY_STONE_TANK, TileSkyStoneTank.class);
    public static final TileDefinition<TileSkyChest> SKY_CHEST = new TileDefinition<>(AppEng.makeId("sky_chest"),
        TileSkyChest.class);
    public static final TileDefinition<TileVibrationChamber> VIBRATION_CHAMBER = new TileDefinition<>(
        AppEng.makeId("vibration_chamber"), TileVibrationChamber.class);
    public static final TileDefinition<TileLightDetector> LIGHT_DETECTOR = new TileDefinition<>(
        AEBlockIds.LIGHT_DETECTOR, TileLightDetector.class);
    public static final TileDefinition<TileCrank> CRANK = new TileDefinition<>(AEBlockIds.CRANK, TileCrank.class);
    public static final TileDefinition<TileEnergyGenerator> DEBUG_ENERGY_GEN = new TileDefinition<>(
        AEBlockIds.DEBUG_ENERGY_GEN, TileEnergyGenerator.class);
    public static final TileDefinition<TileItemGen> DEBUG_ITEM_GEN = new TileDefinition<>(
        AEBlockIds.DEBUG_ITEM_GEN, TileItemGen.class);
    public static final TileDefinition<TileCubeGenerator> DEBUG_CUBE_GEN = new TileDefinition<>(
        AEBlockIds.DEBUG_CUBE_GEN, TileCubeGenerator.class);
    public static final TileDefinition<TilePhantomNode> DEBUG_PHANTOM_NODE = new TileDefinition<>(
        AEBlockIds.DEBUG_PHANTOM_NODE, TilePhantomNode.class);

    private AEBlockEntities() {
    }

    public static void init() {
        CABLE_BUS.register();
        QUANTUM_BRIDGE.register();
        MYSTERIOUS_CUBE.register();
        SKY_STONE_TANK.register();
        SKY_CHEST.register();
        VIBRATION_CHAMBER.register();
        LIGHT_DETECTOR.register();
        CRANK.register();
        DEBUG_ENERGY_GEN.register();
        DEBUG_ITEM_GEN.register();
        DEBUG_CUBE_GEN.register();
        DEBUG_PHANTOM_NODE.register();
        GameRegistry.registerTileEntity(TileController.class, Tags.MOD_ID + ":controller");
        GameRegistry.registerTileEntity(TileCreativeEnergyCell.class, Tags.MOD_ID + ":creative_energy_cell");
        GameRegistry.registerTileEntity(TileCrystalResonanceGenerator.class,
            Tags.MOD_ID + ":crystal_resonance_generator");
        GameRegistry.registerTileEntity(TileEnergyAcceptor.class, Tags.MOD_ID + ":energy_acceptor");
        GameRegistry.registerTileEntity(TileEnergyCell.class, Tags.MOD_ID + ":energy_cell");
        GameRegistry.registerTileEntity(TileWirelessAccessPoint.class, Tags.MOD_ID + ":wireless_access_point");
        GameRegistry.registerTileEntity(TileMEChest.class, Tags.MOD_ID + ":chest");
        GameRegistry.registerTileEntity(TileDrive.class, Tags.MOD_ID + ":drive");
        GameRegistry.registerTileEntity(TileCellWorkbench.class, Tags.MOD_ID + ":cell_workbench");
        GameRegistry.registerTileEntity(TileCondenser.class, Tags.MOD_ID + ":condenser");

        GameRegistry.registerTileEntity(TileCharger.class, Tags.MOD_ID + ":charger");
        GameRegistry.registerTileEntity(TileGrowthAccelerator.class, Tags.MOD_ID + ":growth_accelerator");
        GameRegistry.registerTileEntity(TileInscriber.class, Tags.MOD_ID + ":inscriber");
        GameRegistry.registerTileEntity(TileCrystalAssembler.class, Tags.MOD_ID + ":crystal_assembler");
        GameRegistry.registerTileEntity(TileCrystalFixer.class, Tags.MOD_ID + ":crystal_fixer");
        GameRegistry.registerTileEntity(TileIngredientBuffer.class, Tags.MOD_ID + ":ingredient_buffer");
        GameRegistry.registerTileEntity(TileCaner.class, Tags.MOD_ID + ":caner");
        GameRegistry.registerTileEntity(TilePatternProvider.class, Tags.MOD_ID + ":pattern_provider");
        GameRegistry.registerTileEntity(TileCraftingUnit.class, Tags.MOD_ID + ":crafting_unit");
        GameRegistry.registerTileEntity(TileCraftingMonitor.class, Tags.MOD_ID + ":crafting_monitor");
        GameRegistry.registerTileEntity(TileMolecularAssembler.class, Tags.MOD_ID + ":molecular_assembler");
        GameRegistry.registerTileEntity(TileInterface.class, Tags.MOD_ID + ":interface");
        GameRegistry.registerTileEntity(TileIOPort.class, Tags.MOD_ID + ":io_port");
        GameRegistry.registerTileEntity(TileSpatialPylon.class, Tags.MOD_ID + ":spatial_pylon");
        GameRegistry.registerTileEntity(TileSpatialIOPort.class, Tags.MOD_ID + ":spatial_io_port");
        GameRegistry.registerTileEntity(TileSpatialAnchor.class, Tags.MOD_ID + ":spatial_anchor");
        GameRegistry.registerTileEntity(TilePaint.class, Tags.MOD_ID + ":paint");
    }
}
