package appeng.hotkeys;

import appeng.api.features.HotkeyAction;
import appeng.helpers.WirelessTerminalActions;
import net.minecraft.entity.player.EntityPlayerMP;

public class StowHotkeyAction implements HotkeyAction {
    @Override
    public boolean run(EntityPlayerMP player) {
        return WirelessTerminalActions.stowInventory(player);
    }
}
