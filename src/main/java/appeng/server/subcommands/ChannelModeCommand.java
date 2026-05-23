package appeng.server.subcommands;

import appeng.api.networking.pathing.ChannelMode;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.localization.PlayerMessages;
import appeng.hooks.ticking.TickHandler;
import appeng.me.Grid;
import appeng.server.ISubCommand;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Locale;

public class ChannelModeCommand implements ISubCommand {
    private static ChannelMode parseMode(String name) throws WrongUsageException {
        try {
            return ChannelMode.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new WrongUsageException("commands.ae2.channelmode");
        }
    }

    @Override
    public String getHelp(MinecraftServer srv) {
        return "commands.ae2.channelmode";
    }

    private static String[] getModeNames() {
        ChannelMode[] modes = ChannelMode.values();
        String[] names = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            names[i] = modes[i].name().toLowerCase(Locale.ROOT);
        }
        return names;
    }

    @Override
    public void call(MinecraftServer srv, String[] args, ICommandSender sender) throws CommandException {
        if (args.length == 1) {
            ChannelMode mode = AEConfig.instance().getChannelMode();
            sender.sendMessage(PlayerMessages.ChannelModeCurrent.text(mode.name().toLowerCase(Locale.ROOT)));
            return;
        }

        if (args.length != 2) {
            throw new WrongUsageException("commands.ae2.channelmode");
        }

        ChannelMode mode = parseMode(args[1]);
        AELog.info("%s is changing channel mode to %s", sender.getName(), mode);
        AEConfig.instance().setChannelMode(mode);
        AEConfig.instance().save();

        int gridCount = 0;
        for (Grid grid : TickHandler.instance().getGridList()) {
            grid.getPathingService().repath();
            gridCount++;
        }

        sender.sendMessage(PlayerMessages.ChannelModeSet.text(mode.name().toLowerCase(Locale.ROOT), gridCount));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer srv, ICommandSender sender, String[] args,
                                          BlockPos targetPos) {
        if (args.length != 2) {
            return ISubCommand.super.getTabCompletions(srv, sender, args, targetPos);
        }

        return CommandBase.getListOfStringsMatchingLastWord(args, getModeNames());
    }
}
