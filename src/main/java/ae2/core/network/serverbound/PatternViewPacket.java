package ae2.core.network.serverbound;

import ae2.container.pattern.PatternGuiHandler;
import ae2.core.network.ServerboundPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextComponentTranslation;

import java.io.IOException;

public class PatternViewPacket extends ServerboundPacket {

    private static final int MAX_PACKET_BYTES = 128 * 1024;
    private ItemStack pattern = ItemStack.EMPTY;
    private static long nextWarning = -1;

    public PatternViewPacket() {
    }

    public PatternViewPacket(ItemStack pattern) {
        this.pattern = pattern.copy();
    }

    @Override
    protected void read(ByteBuf buf) {
        if (buf.readableBytes() > MAX_PACKET_BYTES) {
            this.invalidateMalformed(buf, new IllegalArgumentException("Pattern view packet exceeds maximum size"));
            return;
        }

        try {
            PacketBuffer packetBuffer = new PacketBuffer(buf);
            this.pattern = packetBuffer.readItemStack();
            if (packetBuffer.isReadable()) {
                this.invalidateMalformed(packetBuffer, new IllegalArgumentException(
                    "Trailing pattern view packet payload bytes: " + packetBuffer.readableBytes()));
            }
        } catch (IOException | RuntimeException e) {
            this.pattern = ItemStack.EMPTY;
            this.invalidateMalformed(buf, e instanceof RuntimeException runtimeException
                ? runtimeException
                : new IllegalArgumentException("Could not read pattern view packet", e));
        }
    }

    @Override
    protected void write(ByteBuf buf) {
        new PacketBuffer(buf).writeItemStack(this.pattern);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (!this.isInvalid() && !this.pattern.isEmpty() && PatternGuiHandler.open(player, this.pattern)) {
            return;
        }

        if (nextWarning < System.currentTimeMillis()) {
            nextWarning = System.currentTimeMillis() + 2000;
            player.sendMessage(new TextComponentTranslation("chat.pattern_view.error"));
        }
    }
}
