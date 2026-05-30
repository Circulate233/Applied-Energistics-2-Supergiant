package appeng.container.implementations;

import appeng.api.config.CondenserOutput;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.items.contents.VoidCellGuiHost;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerVoidCell extends AEBaseContainer {

    private final VoidCellGuiHost host;

    @GuiSync(0)
    public CondenserOutput output = CondenserOutput.TRASH;

    public ContainerVoidCell(InventoryPlayer ip, VoidCellGuiHost host) {
        super(ip, host);
        this.host = host;

        registerClientAction("setMode", Integer.class, this::setMode);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            this.output = this.host.getMode();
        }

        super.broadcastChanges();
    }

    public CondenserOutput getOutput() {
        return this.output;
    }

    public void setModeFromClient(CondenserOutput mode) {
        sendClientAction("setMode", mode.ordinal());
    }

    public void setMode(Integer mode) {
        if (mode == null || mode < 0 || mode >= CondenserOutput.values().length) {
            return;
        }

        this.host.setMode(CondenserOutput.values()[mode]);
        this.output = this.host.getMode();
        this.detectAndSendChanges();
    }
}
