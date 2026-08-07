package ae2.core.network.clientbound;

import ae2.api.storage.ILinkStatus;
import ae2.container.implementations.CellTerminalClientState;
import ae2.container.implementations.CellTerminalClientState.CellTerminalTab;
import ae2.container.implementations.ContainerCellTerminal;
import ae2.core.AELog;
import ae2.core.network.ClientboundPacket;
import ae2.core.network.InitNetwork;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.Arrays;

public class CellTerminalSyncPacket extends ClientboundPacket {
    static final int CHUNK_PAYLOAD_BYTES = 262_144;
    static final int MAX_UNCOMPRESSED_BYTES = 8 * 1_048_576;
    static final int MAX_WIRE_BYTES = 8 * 1_048_576;
    private static final String TAG_WINDOW_ID = "windowId";
    private static final String TAG_STATE = "state";

    private int windowId;
    private boolean compressed;
    private int uncompressedLength;
    private byte[] payload = new byte[0];
    private boolean malformed;

    public CellTerminalSyncPacket() {
    }

    CellTerminalSyncPacket(int windowId, boolean compressed, int uncompressedLength, byte[] payload) {
        this.windowId = windowId;
        this.compressed = compressed;
        this.uncompressedLength = uncompressedLength;
        this.payload = Arrays.copyOf(payload, payload.length);
        validatePayloadMetadata();
    }

    public static void sendToClient(EntityPlayerMP player, int windowId, CellTerminalClientState state) {
        EncodedSyncPayload encoded = encodeStatePayload(windowId, state);
        if (encoded == null) {
            sendOfflineState(player, windowId, state.tab());
            return;
        }

        if (encoded.payload().length <= CHUNK_PAYLOAD_BYTES) {
            InitNetwork.sendToClient(player, new CellTerminalSyncPacket(windowId, encoded.compressed(),
                encoded.uncompressedLength(), encoded.payload()));
            return;
        }

        int transferId = (int) (System.nanoTime() ^ ((long) windowId << 16) ^ state.cacheRevision());
        int totalChunks = (encoded.payload().length + CHUNK_PAYLOAD_BYTES - 1) / CHUNK_PAYLOAD_BYTES;
        for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            int start = chunkIndex * CHUNK_PAYLOAD_BYTES;
            int length = Math.min(CHUNK_PAYLOAD_BYTES, encoded.payload().length - start);
            byte[] chunk = Arrays.copyOfRange(encoded.payload(), start, start + length);
            InitNetwork.sendToClient(player, new CellTerminalSyncChunkPacket(
                windowId,
                transferId,
                chunkIndex,
                totalChunks,
                encoded.compressed(),
                encoded.uncompressedLength(),
                encoded.payload().length,
                chunk));
        }
    }

    static EncodedSyncPayload encodeStatePayload(int windowId, CellTerminalClientState state) {
        try {
            return encodeRawPayload(encodeRoot(rootTag(windowId, state)));
        } catch (RuntimeException e) {
            AELog.error(e, "Could not encode Cell Terminal sync state");
            return null;
        }
    }

    static EncodedSyncPayload encodeRawPayload(byte[] rawPayload) {
        if (rawPayload.length > MAX_UNCOMPRESSED_BYTES) {
            AELog.error("Cell Terminal sync state exceeded %s raw bytes: %s", MAX_UNCOMPRESSED_BYTES,
                rawPayload.length);
            return null;
        }

        var encoded = TerminalPayloadCodec.encode(rawPayload, MAX_UNCOMPRESSED_BYTES);
        if (encoded.payload().length > MAX_WIRE_BYTES) {
            AELog.error("Cell Terminal sync state exceeded %s wire bytes: %s", MAX_WIRE_BYTES,
                encoded.payload().length);
            return null;
        }
        return new EncodedSyncPayload(encoded.compressed(), encoded.uncompressedLength(), encoded.payload());
    }

    private static void sendOfflineState(EntityPlayerMP player, int windowId, CellTerminalTab tab) {
        EncodedSyncPayload encoded = encodeStatePayload(windowId,
            CellTerminalClientState.offline(tab, ILinkStatus.ofDisconnected()));
        if (encoded == null) {
            AELog.error("Could not encode Cell Terminal offline sync state");
            return;
        }
        if (encoded.payload().length > CHUNK_PAYLOAD_BYTES) {
            AELog.error("Cell Terminal offline sync state unexpectedly exceeded single packet limit: %s",
                encoded.payload().length);
            return;
        }
        InitNetwork.sendToClient(player, new CellTerminalSyncPacket(windowId, encoded.compressed(),
            encoded.uncompressedLength(), encoded.payload()));
    }

    static NBTTagCompound rootTag(int windowId, CellTerminalClientState state) {
        var root = new NBTTagCompound();
        root.setInteger(TAG_WINDOW_ID, windowId);
        root.setTag(TAG_STATE, state.toTag());
        return root;
    }

    static byte[] encodeRoot(NBTTagCompound tag) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            new PacketBuffer(buffer).writeCompoundTag(tag);
            byte[] payload = new byte[buffer.readableBytes()];
            buffer.readBytes(payload);
            return payload;
        } finally {
            buffer.release();
        }
    }

    static DecodedState decodePayload(int expectedWindowId, boolean compressed, int uncompressedLength,
                                     byte[] payload) {
        byte[] rawPayload = TerminalPayloadCodec.decode(compressed, uncompressedLength, payload,
            MAX_UNCOMPRESSED_BYTES);
        ByteBuf buffer = Unpooled.wrappedBuffer(rawPayload);
        try {
            NBTTagCompound root = new PacketBuffer(buffer).readCompoundTag();
            if (root == null || !root.hasKey(TAG_STATE)) {
                throw new IllegalArgumentException("Cell Terminal sync payload has no state tag");
            }
            int actualWindowId = root.getInteger(TAG_WINDOW_ID);
            if (actualWindowId != expectedWindowId) {
                throw new IllegalArgumentException("Cell Terminal sync window id does not match packet header");
            }
            if (buffer.isReadable()) {
                throw new IllegalArgumentException("Trailing Cell Terminal sync payload bytes: "
                    + buffer.readableBytes());
            }
            return new DecodedState(actualWindowId, CellTerminalClientState.fromTag(root.getCompoundTag(TAG_STATE)));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not decode Cell Terminal sync payload", e);
        } finally {
            buffer.release();
        }
    }

    DecodedState decodePayload() {
        return decodePayload(this.windowId, this.compressed, this.uncompressedLength, this.payload);
    }

    @SideOnly(Side.CLIENT)
    static void applyPayload(Minecraft minecraft, int windowId, boolean compressed, int uncompressedLength,
                             byte[] payload) {
        try {
            DecodedState decoded = decodePayload(windowId, compressed, uncompressedLength, payload);
            applyState(minecraft, decoded.windowId(), decoded.state());
        } catch (RuntimeException e) {
            AELog.warn(e, "Ignoring malformed Cell Terminal sync payload");
        }
    }

    @SideOnly(Side.CLIENT)
    static void applyState(Minecraft minecraft, int windowId, CellTerminalClientState state) {
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.player.openContainer instanceof ContainerCellTerminal container
            && container.windowId == windowId) {
            container.applyClientState(state);
        }
    }

    @Override
    protected void read(ByteBuf buf) {
        var data = new PacketBuffer(buf);
        try {
            this.windowId = data.readVarInt();
            this.compressed = data.readBoolean();
            this.uncompressedLength = data.readVarInt();
            int payloadLength = data.readVarInt();
            if (payloadLength <= 0 || payloadLength > CHUNK_PAYLOAD_BYTES
                || payloadLength != data.readableBytes()) {
                throw new IllegalArgumentException("Cell Terminal sync payload length does not match packet bytes");
            }
            this.payload = new byte[payloadLength];
            data.readBytes(this.payload);
            validatePayloadMetadata();
        } catch (RuntimeException e) {
            this.malformed = true;
            buf.skipBytes(buf.readableBytes());
            AELog.warn(e, "Ignoring malformed Cell Terminal sync packet");
        }
    }

    @Override
    protected void write(ByteBuf buf) {
        validatePayloadMetadata();
        var data = new PacketBuffer(buf);
        data.writeVarInt(this.windowId);
        data.writeBoolean(this.compressed);
        data.writeVarInt(this.uncompressedLength);
        data.writeVarInt(this.payload.length);
        data.writeBytes(this.payload);
    }

    private void validatePayloadMetadata() {
        if (this.uncompressedLength <= 0 || this.uncompressedLength > MAX_UNCOMPRESSED_BYTES) {
            throw new IllegalArgumentException("Invalid Cell Terminal sync uncompressed length: "
                + this.uncompressedLength);
        }
        if (this.payload.length <= 0 || this.payload.length > CHUNK_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Invalid Cell Terminal sync wire payload length: "
                + this.payload.length);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        if (this.malformed || minecraft.player == null) {
            return;
        }
        applyPayload(minecraft, this.windowId, this.compressed, this.uncompressedLength, this.payload);
    }

    record DecodedState(int windowId, CellTerminalClientState state) {
    }

    record EncodedSyncPayload(boolean compressed, int uncompressedLength, byte[] payload) {
        EncodedSyncPayload {
            payload = Arrays.copyOf(payload, payload.length);
        }
    }
}
