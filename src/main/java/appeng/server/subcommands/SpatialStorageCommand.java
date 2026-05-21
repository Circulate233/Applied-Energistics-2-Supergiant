package appeng.server.subcommands;

import appeng.api.features.IPlayerRegistry;
import appeng.core.definitions.AEItems;
import appeng.items.storage.SpatialStorageCellItem;
import appeng.server.ISubCommand;
import appeng.spatial.SpatialStorageDimensionIds;
import appeng.spatial.SpatialStoragePlot;
import appeng.spatial.SpatialStoragePlotManager;
import appeng.spatial.TransitionInfo;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.Comparator;

public class SpatialStorageCommand implements ISubCommand {

    private static void teleportPlayer(MinecraftServer server, EntityPlayerMP player, int dimensionId, BlockPos pos) {
        WorldServer targetLevel = server.getWorld(dimensionId);
        if (targetLevel == null) {
            DimensionManager.initDimension(dimensionId);
            targetLevel = server.getWorld(dimensionId);
        }
        if (targetLevel == null) {
            throw new IllegalStateException("Missing target dimension " + dimensionId);
        }

        if (player.dimension == dimensionId) {
            player.connection.setPlayerLocation(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.rotationYaw,
                player.rotationPitch);
            return;
        }

        server.getPlayerList().transferPlayerToDimension(player, dimensionId, new FixedTeleporter(targetLevel, pos));
        player.connection.setPlayerLocation(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.rotationYaw,
            player.rotationPitch);
    }

    private static EntityPlayerMP getPlayer(ICommandSender sender) throws CommandException {
        if (sender.getCommandSenderEntity() instanceof EntityPlayerMP player) {
            return player;
        }
        throw new CommandException("This command requires a player.");
    }

    private static SpatialStoragePlot getPlot(int plotId) throws CommandException {
        SpatialStoragePlot plot = SpatialStoragePlotManager.INSTANCE.getPlot(plotId);
        if (plot == null) {
            throw new CommandException("No plot found with id " + plotId);
        }
        return plot;
    }

    private static SpatialStoragePlot getCurrentPlot(ICommandSender sender) throws CommandException {
        BlockPos playerPos = sender.getPosition();
        if (playerPos == null || sender.getEntityWorld().provider.getDimension() != SpatialStorageDimensionIds.getDimensionId()) {
            throw new CommandException("Not in the spatial storage dimension.");
        }

        for (SpatialStoragePlot plot : SpatialStoragePlotManager.INSTANCE.getPlots()) {
            BlockPos origin = plot.getOrigin();
            BlockPos size = plot.getSize();
            if (playerPos.getX() >= origin.getX() && playerPos.getX() <= origin.getX() + size.getX()
                && playerPos.getZ() >= origin.getZ() && playerPos.getZ() <= origin.getZ() + size.getZ()) {
                return plot;
            }
        }

        throw new CommandException("No spatial storage plot at the current position.");
    }

    private static int parsePlotId(String input) throws CommandException {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid plot id: " + input);
        }
    }

    private static String format(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static String describeOwner(MinecraftServer server, int ownerId) {
        if (ownerId == -1) {
            return "unknown";
        }

        EntityPlayerMP connectedPlayer = IPlayerRegistry.getConnected(server, ownerId);
        if (connectedPlayer != null) {
            return connectedPlayer.getGameProfile().getName() + " (online)";
        }

        java.util.UUID profileId = IPlayerRegistry.getMapping(server).getProfileId(ownerId);
        if (profileId != null) {
            com.mojang.authlib.GameProfile cachedProfile = server.getPlayerProfileCache().getProfileByUUID(profileId);
            if (cachedProfile != null) {
                return cachedProfile.getName() + " (offline)";
            }
            return profileId.toString();
        }

        return Integer.toString(ownerId);
    }

    private static java.time.Instant getLastTransitionTimestamp(SpatialStoragePlot plot) {
        TransitionInfo transition = plot.getLastTransition();
        return transition != null ? transition.timestamp() : java.time.Instant.MIN;
    }

    @Override
    public String getHelp(MinecraftServer srv) {
        return "commands.ae2.spatial";
    }

    @Override
    public void call(MinecraftServer srv, String[] args, ICommandSender sender) throws CommandException {
        try {
            SpatialStoragePlotManager.INSTANCE.getLevel();
        } catch (IllegalStateException e) {
            sender.sendMessage(new TextComponentString("Spatial storage level is unavailable: " + e.getMessage()));
            return;
        }

        if (args.length == 1) {
            listPlots(srv, sender);
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "info" -> {
                if (args.length == 2) {
                    showPlotInfo(sender, getCurrentPlot(sender));
                    return;
                }
                showPlotInfo(sender, getPlot(parsePlotId(args[2])));
                return;
            }
            case "tp" -> {
                if (args.length != 3) {
                    throw new WrongUsageException("commands.ae2.spatial");
                }
                teleportToPlot(srv, sender, parsePlotId(args[2]));
                return;
            }
            case "tpback" -> {
                if (args.length == 2) {
                    teleportBack(srv, sender, getCurrentPlot(sender));
                    return;
                }
                teleportBack(srv, sender, getPlot(parsePlotId(args[2])));
                return;
            }
            case "givecell" -> {
                if (args.length != 3) {
                    throw new WrongUsageException("commands.ae2.spatial");
                }
                giveCell(sender, parsePlotId(args[2]));
                return;
            }
        }

        throw new WrongUsageException("commands.ae2.spatial");
    }

    private void listPlots(MinecraftServer server, ICommandSender sender) {
        ObjectList<SpatialStoragePlot> plots = new ObjectArrayList<>(SpatialStoragePlotManager.INSTANCE.getPlots());
        if (plots.isEmpty()) {
            sender.sendMessage(new TextComponentString("No spatial storage plots exist."));
            return;
        }

        plots.sort(Comparator.comparing(SpatialStorageCommand::getLastTransitionTimestamp).reversed());
        for (int i = 0; i < Math.min(5, plots.size()); i++) {
            SpatialStoragePlot plot = plots.get(i);
            sender.sendMessage(new TextComponentString(
                "Plot #" + plot.getId() + " size=" + format(plot.getSize()) + " origin=" + format(plot.getOrigin())));
        }
    }

    private void showPlotInfo(ICommandSender sender, SpatialStoragePlot plot) {
        sender.sendMessage(new TextComponentString("Plot #" + plot.getId()));
        sender.sendMessage(new TextComponentString("Owner: " + describeOwner(sender.getServer(), plot.getOwner())));
        sender.sendMessage(new TextComponentString("Size: " + format(plot.getSize())));
        sender.sendMessage(new TextComponentString("Origin: " + format(plot.getOrigin())));
        sender.sendMessage(new TextComponentString("Region: " + plot.getRegionFilename()));

        TransitionInfo transition = plot.getLastTransition();
        if (transition != null) {
            sender.sendMessage(new TextComponentString("Last source: " + transition.worldId()));
            sender.sendMessage(new TextComponentString("Last min: " + format(transition.min())));
            sender.sendMessage(new TextComponentString("Last max: " + format(transition.max())));
            sender.sendMessage(new TextComponentString("Last time: " + transition.timestamp()));
        }
    }

    private void teleportToPlot(MinecraftServer server, ICommandSender sender, int plotId) throws CommandException {
        EntityPlayerMP player = getPlayer(sender);
        SpatialStoragePlot plot = getPlot(plotId);
        teleportPlayer(server, player, SpatialStorageDimensionIds.getDimensionId(), plot.getOrigin().add(0, 1, 0));
    }

    private void teleportBack(MinecraftServer server, ICommandSender sender, SpatialStoragePlot plot)
        throws CommandException {
        EntityPlayerMP player = getPlayer(sender);
        TransitionInfo transition = plot.getLastTransition();
        if (transition == null) {
            throw new CommandException("No previous transition recorded.");
        }

        int dimensionId = transition.dimensionId();
        teleportPlayer(server, player, dimensionId, transition.min().add(0, 1, 0));
    }

    private void giveCell(ICommandSender sender, int plotId) throws CommandException {
        EntityPlayerMP player = getPlayer(sender);
        SpatialStoragePlot plot = getPlot(plotId);
        ItemStack cell;
        int longestSide = Math.max(plot.getSize().getX(), Math.max(plot.getSize().getY(), plot.getSize().getZ()));
        if (longestSide <= 2) {
            cell = AEItems.SPATIAL_CELL2.stack();
        } else if (longestSide <= 16) {
            cell = AEItems.SPATIAL_CELL16.stack();
        } else {
            cell = AEItems.SPATIAL_CELL128.stack();
        }

        if (!(cell.getItem() instanceof SpatialStorageCellItem spatialCellItem)) {
            throw new CommandException("Not a spatial storage cell.");
        }

        spatialCellItem.setStoredDimension(cell, plot.getId(), plot.getSize());
        player.addItemStackToInventory(cell);
    }

    private static final class FixedTeleporter extends Teleporter {
        private final BlockPos pos;

        private FixedTeleporter(WorldServer worldIn, BlockPos pos) {
            super(worldIn);
            this.pos = pos;
        }

        @Override
        public void placeInPortal(net.minecraft.entity.Entity entity, float rotationYaw) {
            entity.setLocationAndAngles(this.pos.getX() + 0.5, this.pos.getY(), this.pos.getZ() + 0.5,
                entity.rotationYaw, entity.rotationPitch);
        }

        @Override
        public boolean placeInExistingPortal(net.minecraft.entity.Entity entity, float rotationYaw) {
            this.placeInPortal(entity, rotationYaw);
            return true;
        }

        @Override
        public boolean makePortal(net.minecraft.entity.Entity entity) {
            return true;
        }

        @Override
        public void removeStalePortalLocations(long worldTime) {
        }
    }
}
