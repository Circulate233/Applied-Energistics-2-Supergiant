package appeng.core.network.clientbound;

import appeng.container.networking.INetworkStatusContainer;
import appeng.container.networking.NetworkStatus;
import appeng.core.network.ClientboundPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class NetworkStatusPacket extends ClientboundPacket {
    private NetworkStatus status;
    private boolean canExportGrid;

    public NetworkStatusPacket() {
    }

    public NetworkStatusPacket(NetworkStatus status, boolean canExportGrid) {
        this.status = status;
        this.canExportGrid = canExportGrid;
    }

    @Override
    protected void read(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        this.status = NetworkStatus.read(packetBuffer);
        this.canExportGrid = packetBuffer.readBoolean();
    }

    @Override
    protected void write(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        status.write(packetBuffer);
        packetBuffer.writeBoolean(this.canExportGrid);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.player.openContainer instanceof INetworkStatusContainer container
            && this.status != null) {
            container.setStatus(this.status);
            container.setCanExportGrid(this.canExportGrid);
        }
    }
}
