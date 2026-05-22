package appeng.core.network.clientbound;

import appeng.client.gui.PreviousExternalGui;
import appeng.core.network.ClientboundPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;

public class RestorePreviousGuiPacket extends ClientboundPacket {
    private int windowId;

    public RestorePreviousGuiPacket() {
    }

    public RestorePreviousGuiPacket(int windowId) {
        this.windowId = windowId;
    }

    @Override
    protected void read(ByteBuf buf) {
        this.windowId = new PacketBuffer(buf).readVarInt();
    }

    @Override
    protected void write(ByteBuf buf) {
        new PacketBuffer(buf).writeVarInt(this.windowId);
    }

    @Override
    public void handleClient(Minecraft minecraft) {
        PreviousExternalGui.restore(minecraft, this.windowId);
    }
}
