package appeng.client;

import appeng.core.network.InitNetwork;
import appeng.core.network.serverbound.HotkeyPacket;
import net.minecraft.client.settings.KeyBinding;

public record Hotkey(String name, KeyBinding mapping) {

    public void check() {
        while (mapping().isPressed()) {
            InitNetwork.sendToServer(new HotkeyPacket(this));
        }
    }
}
