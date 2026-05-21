package appeng.core.network.serverbound;

import appeng.container.AEBaseContainer;
import appeng.core.network.ServerboundPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.Nullable;

public class GuiActionPacket extends ServerboundPacket {
    private int windowId;
    private String actionName;
    @Nullable
    private String jsonPayload;

    public GuiActionPacket() {
    }

    public GuiActionPacket(int windowId, String actionName, @Nullable String jsonPayload) {
        this.windowId = windowId;
        this.actionName = actionName;
        this.jsonPayload = jsonPayload;
    }

    @Override
    protected void read(ByteBuf buf) {
        var packetBuffer = new PacketBuffer(buf);
        this.windowId = packetBuffer.readInt();
        this.actionName = packetBuffer.readString(32767);
        if (packetBuffer.readBoolean()) {
            this.jsonPayload = packetBuffer.readString(32767);
        } else {
            this.jsonPayload = null;
        }
    }

    @Override
    protected void write(ByteBuf buf) {
        var packetBuffer = new PacketBuffer(buf);
        packetBuffer.writeInt(this.windowId);
        packetBuffer.writeString(this.actionName);
        packetBuffer.writeBoolean(this.jsonPayload != null);
        if (this.jsonPayload != null) {
            packetBuffer.writeString(this.jsonPayload);
        }
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (player.openContainer.windowId == this.windowId && player.openContainer instanceof AEBaseContainer container) {
            container.receiveClientAction(this.actionName, this.jsonPayload);
        }
    }
}
