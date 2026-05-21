package appeng.client.gui.networking;

import appeng.client.gui.me.networktool.GuiNetworkStatus;
import appeng.client.gui.style.GuiStyleManager;
import appeng.container.networking.ContainerControllerStatus;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiControllerStatus extends GuiNetworkStatus<ContainerControllerStatus> {

    public GuiControllerStatus(ContainerControllerStatus container, InventoryPlayer playerInventory) {
        super(container, playerInventory, GuiStyleManager.loadStyleDoc("/screens/network_status.json"));
    }
}
