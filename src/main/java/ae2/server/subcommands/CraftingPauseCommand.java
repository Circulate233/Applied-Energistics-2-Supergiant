/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2026, TeamAppliedEnergistics, All rights reserved.
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

package ae2.server.subcommands;

import ae2.core.AEConfig;
import ae2.core.AELog;
import ae2.core.Tags;
import ae2.core.localization.PlayerMessages;
import ae2.server.ISubCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;

public final class CraftingPauseCommand implements ISubCommand {

    private static final String USAGE = "commands.ae2.craftingpause";

    @Override
    public String getHelp(MinecraftServer srv) {
        return USAGE;
    }

    @Override
    public void call(MinecraftServer srv, String[] args, ICommandSender sender) throws CommandException {
        if (args.length == 1) {
            sender.sendMessage(PlayerMessages.CraftingPauseCurrent.text(AEConfig.CRAFTING.craftingCalculationPausingInterval));
            return;
        }

        if (args.length != 2) {
            AELog.warn("%s supplied an invalid crafting pause command argument count: %d", sender.getName(),
                args.length - 1);
            throw new WrongUsageException(USAGE);
        }

        int interval;
        try {
            interval = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            AELog.warn("%s supplied an invalid crafting pause interval: %s", sender.getName(), args[1]);
            throw new WrongUsageException(USAGE);
        }

        if (interval < 100 || interval > 100000) {
            AELog.warn("%s supplied an out-of-range crafting pause interval: %d", sender.getName(), interval);
            throw new WrongUsageException(USAGE);
        }

        AEConfig.CRAFTING.craftingCalculationPausingInterval = interval;
        ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        AELog.info("%s changed the crafting calculation pause interval to %d", sender.getName(), interval);
        sender.sendMessage(PlayerMessages.CraftingPauseSet.text(interval));
    }
}
