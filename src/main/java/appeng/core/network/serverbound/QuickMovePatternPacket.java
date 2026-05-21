package appeng.core.network.serverbound;

import appeng.container.implementations.ContainerPatternAccessTerm;
import appeng.core.network.ServerboundPacket;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

public class QuickMovePatternPacket extends ServerboundPacket {
    private int windowId;
    private int clickedSlot;
    private LongList allowedPatternContainers = LongLists.emptyList();

    public QuickMovePatternPacket() {
    }

    public QuickMovePatternPacket(int windowId, int clickedSlot, LongCollection allowedPatternContainers) {
        this.windowId = windowId;
        this.clickedSlot = clickedSlot;
        this.allowedPatternContainers = new LongArrayList(allowedPatternContainers);
    }

    @Override
    protected void read(ByteBuf buf) {
        var packetBuffer = new PacketBuffer(buf);
        this.windowId = packetBuffer.readVarInt();
        this.clickedSlot = packetBuffer.readVarInt();
        int size = packetBuffer.readVarInt();
        var ids = new LongArrayList(size);
        for (int i = 0; i < size; i++) {
            ids.add(packetBuffer.readVarLong());
        }
        this.allowedPatternContainers = ids;
    }

    @Override
    protected void write(ByteBuf buf) {
        var packetBuffer = new PacketBuffer(buf);
        packetBuffer.writeVarInt(this.windowId);
        packetBuffer.writeVarInt(this.clickedSlot);
        packetBuffer.writeVarInt(this.allowedPatternContainers.size());
        for (var id : this.allowedPatternContainers) {
            packetBuffer.writeVarLong(id);
        }
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (player.openContainer.windowId == this.windowId
            && player.openContainer instanceof ContainerPatternAccessTerm container) {
            container.quickMovePattern(player, this.clickedSlot, this.allowedPatternContainers);
        }
    }
}
