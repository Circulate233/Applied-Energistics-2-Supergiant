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

package appeng.debug;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.pathing.ControllerState;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.hooks.ticking.TickHandler;
import appeng.items.AEBaseItem;
import appeng.me.Grid;
import appeng.me.GridNode;
import appeng.me.helpers.IGridConnectedTile;
import appeng.me.service.TickManagerService;
import appeng.parts.networking.CablePart;
import appeng.parts.p2p.P2PTunnelPart;
import appeng.tile.AEBaseTile;
import appeng.tile.networking.TileController;
import appeng.util.InteractionUtil;
import appeng.util.Platform;
import com.google.common.collect.Iterables;
import com.google.common.math.StatsAccumulator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.Set;

public class DebugCardItem extends AEBaseItem {

    public DebugCardItem() {
        this.setMaxStackSize(1);
    }

    private static TextComponentString style(TextComponentString component, TextFormatting... formattings) {
        Style style = new Style();
        for (var formatting : formattings) {
            if (formatting.isColor()) {
                style.setColor(formatting);
            } else if (formatting == TextFormatting.BOLD) {
                style.setBold(true);
            } else if (formatting == TextFormatting.ITALIC) {
                style.setItalic(true);
            } else if (formatting == TextFormatting.UNDERLINE) {
                style.setUnderlined(true);
            } else if (formatting == TextFormatting.STRIKETHROUGH) {
                style.setStrikethrough(true);
            } else if (formatting == TextFormatting.OBFUSCATED) {
                style.setObfuscated(true);
            }
        }
        component.setStyle(style);
        return component;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (InteractionUtil.isInAlternateUseMode(player) && !world.isRemote) {
            int grids = 0;

            var stats = new StatsAccumulator();
            for (var grid : TickHandler.instance().getGridList()) {
                if (grid instanceof Grid) {
                    grids++;
                    stats.add(grid.size());
                }
            }

            divider(player);
            outputMessage(player, "Grids", TextFormatting.BOLD);
            this.outputSecondaryMessage(player, "Grids", Integer.toString(grids));
            if (stats.count() > 0) {
                this.outputSecondaryMessage(player, "Total Nodes", "" + (long) stats.sum());
                this.outputSecondaryMessage(player, "Mean Nodes", "" + (long) stats.mean());
                this.outputSecondaryMessage(player, "Max Nodes", "" + (long) stats.max());
            }
            divider(player);
            outputMessage(player, "Ticking", TextFormatting.BOLD);
            this.outputSecondaryMessage(player, "Current Tick: ",
                Long.toString(TickHandler.instance().getCurrentTick()));
            for (var line : TickHandler.instance().getBlockEntityReport()) {
                player.sendMessage(line);
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX,
                                           float hitY, float hitZ, EnumHand hand) {
        if (world.isRemote) {
            return EnumActionResult.PASS;
        }

        if (player == null || InteractionUtil.isInAlternateUseMode(player)) {
            return EnumActionResult.PASS;
        }

        var gh = GridHelper.getNodeHost(world, pos);
        if (gh != null) {
            divider(player);
            var node = (GridNode) gh.getGridNode(side);
            if (node == null) {
                if (gh instanceof IGridConnectedTile gridConnectedTile) {
                    node = (GridNode) gridConnectedTile.getMainNode().getNode();
                    this.outputMessage(player, "Main node of IGridConnectedTile");
                }
            }
            if (node != null) {
                this.outputMessage(player, "-- Grid Details");
                final Grid g = node.getInternalGrid();
                final IGridNode center = g.getPivot();
                this.outputPrimaryMessage(player, "Grid Powered",
                    String.valueOf(g.getEnergyService().isNetworkPowered()));
                this.outputPrimaryMessage(player, "Grid Booted",
                    String.valueOf(!g.getPathingService().isNetworkBooting()));
                this.outputPrimaryMessage(player, "Nodes in grid", String.valueOf(Iterables.size(g.getNodes())));
                this.outputSecondaryMessage(player, "Grid Pivot Node", String.valueOf(center));

                var tmc = (TickManagerService) g.getTickManager();
                for (var c : g.getMachineClasses()) {
                    int o = 0;
                    long totalAverageTime = 0;
                    long singleMaximumTime = 0;

                    for (var oj : g.getMachineNodes(c)) {
                        o++;
                        totalAverageTime += tmc.getAverageTime(oj);
                        singleMaximumTime = Math.max(singleMaximumTime, tmc.getMaximumTime(oj));
                    }

                    String message = "#: " + o;

                    if (totalAverageTime > 0) {
                        message += "; average: " + Platform.formatTimeMeasurement(totalAverageTime);
                    }
                    if (singleMaximumTime > 0) {
                        message += "; max: " + Platform.formatTimeMeasurement(singleMaximumTime);
                    }

                    this.outputSecondaryMessage(player, c.getSimpleName(), message);
                }

                this.outputMessage(player, "-- Node Details");

                this.outputPrimaryMessage(player, "This Node", String.valueOf(node));
                this.outputPrimaryMessage(player, "This Node Active", String.valueOf(node.isActive()));
                this.outputSecondaryMessage(player, "Node exposed on side", side.getName());

                var pg = g.getPathingService();
                if (pg.getControllerState() == ControllerState.CONTROLLER_ONLINE) {

                    Set<IGridNode> next = new ReferenceOpenHashSet<>();
                    next.add(node);

                    final int maxLength = 10000;

                    int length = 0;
                    outer:
                    while (!next.isEmpty()) {
                        final Iterable<IGridNode> current = next;
                        next = new ReferenceOpenHashSet<>();

                        for (IGridNode n : current) {
                            if (n.getOwner() instanceof TileController) {
                                break outer;
                            }

                            for (var c : n.getConnections()) {
                                next.add(c.getOtherSide(n));
                            }
                        }

                        length++;

                        if (length > maxLength) {
                            break;
                        }
                    }

                    this.outputSecondaryMessage(player, "Cable Distance", Integer.toString(length));
                }

                if (center.getOwner() instanceof P2PTunnelPart<?> tunnelPart) {
                    this.outputSecondaryMessage(player, "Freq", Integer.toString(tunnelPart.getFrequency()));
                }
            } else {
                this.outputMessage(player, "No Node Available.");
            }
        } else {
            this.outputMessage(player, "Not Networked Block");
        }

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof IPartHost partHost) {
            this.outputMessage(player, "-- CableBus Details");
            outputSecondaryMessage(player, "In World", Boolean.toString(partHost.isInWorld()));
            outputSecondaryMessage(player, "Has Redstone", Boolean.toString(partHost.hasRedstone()));
            final IPart center = partHost.getPart(null);
            partHost.markForUpdate();
            if (center != null) {
                final GridNode n = (GridNode) center.getGridNode();
                if (n != null) {
                    this.outputSecondaryMessage(player, "Node Channels", Integer.toString(n.getUsedChannels()));
                    for (var entry : n.getInWorldConnections().entrySet()) {
                        this.outputSecondaryMessage(player, "Channels " + entry.getKey().getName(),
                            Integer.toString(entry.getValue().getUsedChannels()));
                    }
                }
            }
            if (center instanceof CablePart cablePart) {
                var msg = new TextComponentString("");
                for (var v : EnumFacing.values()) {
                    msg.appendSibling(
                        style(new TextComponentString(v.name().substring(0, 1)),
                            cablePart.isConnected(v) ? TextFormatting.GREEN : TextFormatting.DARK_GRAY));
                }
                player.sendMessage(style(new TextComponentString("Connected Sides: "), TextFormatting.GRAY)
                    .appendSibling(msg));
            }
        }

        if (te instanceof IAEPowerStorage ps) {
            this.outputMessage(player, "-- EnergyStorage Details");
            this.outputSecondaryMessage(player, "Energy", ps.getAECurrentPower() + " / " + ps.getAEMaxPower());

            if (gh != null) {
                final IGridNode node = gh.getGridNode(side);
                if (node != null) {
                    final IEnergyService eg = node.getGrid().getEnergyService();
                    this.outputSecondaryMessage(player, "GridEnergy",
                        eg.getStoredPower() + " : " + eg.getEnergyDemand(Double.MAX_VALUE));
                }
            }
        }

        if (te instanceof AEBaseTile be) {
            this.outputMessage(player, "-- Delayed Init Details");
            outputSecondaryMessage(player, "QueuedForReady", "" + be.getQueuedForReady());
            outputSecondaryMessage(player, "ReadyInvoked", "" + be.getReadyInvoked());
        }

        return EnumActionResult.SUCCESS;
    }

    private void divider(EntityPlayer player) {
        this.outputMessage(player, "---------------------------------------------", TextFormatting.BOLD,
            TextFormatting.DARK_PURPLE);
    }

    private void outputMessage(Entity player, String string, TextFormatting... chatFormattings) {
        player.sendMessage(style(new TextComponentString(string), chatFormattings));
    }

    private void outputMessage(Entity player, String string) {
        player.sendMessage(new TextComponentString(string));
    }

    private void outputPrimaryMessage(Entity player, String label, String value) {
        this.outputLabeledMessage(player, label, value, TextFormatting.BOLD, TextFormatting.LIGHT_PURPLE);
    }

    private void outputSecondaryMessage(Entity player, String label, String value) {
        this.outputLabeledMessage(player, label, value, TextFormatting.GRAY);
    }

    private void outputLabeledMessage(Entity player, String label, String value,
                                      TextFormatting... chatFormattings) {
        player.sendMessage(new TextComponentString("")
            .appendSibling(style(new TextComponentString(label + ": "), chatFormattings))
            .appendText(value));
    }
}
