package appeng.server.subcommands;

import appeng.core.localization.PlayerMessages;
import appeng.me.service.TickManagerService;
import appeng.server.ISubCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
public class TickMonitoring implements ISubCommand {
    @Override
    public String getHelp(MinecraftServer srv) {
        return "commands.ae2.tickmonitor";
    }

    @Override
    public void call(MinecraftServer srv, String[] args, ICommandSender sender) throws CommandException {
        if (args.length != 2) {
            throw new WrongUsageException("commands.ae2.tickmonitor");
        }

        if ("true".equalsIgnoreCase(args[1]) || "on".equalsIgnoreCase(args[1])) {
            TickManagerService.MONITORING_ENABLED = true;
        } else if ("false".equalsIgnoreCase(args[1]) || "off".equalsIgnoreCase(args[1])) {
            TickManagerService.MONITORING_ENABLED = false;
        } else {
            throw new WrongUsageException("commands.ae2.tickmonitor");
        }

        sender.sendMessage((TickManagerService.MONITORING_ENABLED
            ? PlayerMessages.TickMonitoringEnabled
            : PlayerMessages.TickMonitoringDisabled).text());
    }
}
