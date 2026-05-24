package appeng.hotkeys;

import appeng.api.features.HotkeyAction;
import appeng.helpers.WirelessTerminalActions;
import net.minecraft.entity.player.EntityPlayerMP;

public class MagnetHotkeyAction implements HotkeyAction {
    @Override
    public boolean run(EntityPlayerMP player) {
        return WirelessTerminalActions.cycleMagnetMode(player);
    }
}
