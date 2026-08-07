package ae2.core.network.clientbound;

import ae2.core.AELog;
import ae2.core.network.ClientboundPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CellTerminalSyncChunkPacket extends ClientboundPacket {
    private static final int MAX_PENDING_TRANSFERS = 16;
    private static final int MAX_PENDING_WIRE_BYTES = CellTerminalSyncPacket.MAX_WIRE_BYTES;
    private static final long PENDING_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);
    static final PendingTransferMap PENDING_TRANSFERS = new PendingTransferMap();

    private int windowId;
    private int transferId;
    private int chunkIndex;
    private int totalChunks;
    private boolean compressed;
    private int uncompressedLength;
    private int totalWireBytes;
    private byte[] payload = new byte[0];
    private boolean malformed;

    public CellTerminalSyncChunkPacket() {
    }

    public CellTerminalSyncChunkPacket(int windowId, int transferId, int chunkIndex, int totalChunks,
                                       boolean compressed, int uncompressedLength, int totalWireBytes, byte[] payload) {
        this.windowId = windowId;
        this.transferId = transferId;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.compressed = compressed;
        this.uncompressedLength = uncompressedLength;
        this.totalWireBytes = totalWireBytes;
        this.payload = Arrays.copyOf(payload, payload.length);
        validateHeader();
    }

    /**
     * Releases incomplete transfers owned by a Cell Terminal window when its GUI closes.
     */
    @SideOnly(Side.CLIENT)
    public static void clearPendingTransfers(int windowId) {
        PENDING_TRANSFERS.removeWindow(windowId);
    }

    @Override
    protected void read(ByteBuf buf) {
        var data = new PacketBuffer(buf);
        try {
            this.windowId = data.readVarInt();
            this.transferId = data.readInt();
            this.chunkIndex = data.readVarInt();
            this.totalChunks = data.readVarInt();
            this.compressed = data.readBoolean();
            this.uncompressedLength = data.readVarInt();
            this.totalWireBytes = data.readVarInt();
            int payloadSize = data.readVarInt();
            if (payloadSize <= 0 || payloadSize > CellTerminalSyncPacket.CHUNK_PAYLOAD_BYTES
                || payloadSize != data.readableBytes()) {
                throw new IllegalArgumentException("Cell Terminal chunk payload length does not match packet bytes");
            }
            this.payload = new byte[payloadSize];
            data.readBytes(this.payload);
            validateHeader();
        } catch (RuntimeException e) {
            this.malformed = true;
            buf.skipBytes(buf.readableBytes());
            AELog.warn(e, "Ignoring malformed Cell Terminal sync chunk");
        }
    }

    @Override
    protected void write(ByteBuf buf) {
        validateHeader();
        var data = new PacketBuffer(buf);
        data.writeVarInt(this.windowId);
        data.writeInt(this.transferId);
        data.writeVarInt(this.chunkIndex);
        data.writeVarInt(this.totalChunks);
        data.writeBoolean(this.compressed);
        data.writeVarInt(this.uncompressedLength);
        data.writeVarInt(this.totalWireBytes);
        data.writeVarInt(this.payload.length);
        data.writeBytes(this.payload);
    }

    private void validateHeader() {
        if (this.uncompressedLength <= 0 || this.uncompressedLength > CellTerminalSyncPacket.MAX_UNCOMPRESSED_BYTES) {
            throw new IllegalArgumentException("Invalid Cell Terminal chunk uncompressed length: "
                + this.uncompressedLength);
        }
        if (this.totalWireBytes <= CellTerminalSyncPacket.CHUNK_PAYLOAD_BYTES
            || this.totalWireBytes > CellTerminalSyncPacket.MAX_WIRE_BYTES) {
            throw new IllegalArgumentException("Invalid Cell Terminal chunk total wire bytes: " + this.totalWireBytes);
        }
        int maximumChunks = (CellTerminalSyncPacket.MAX_WIRE_BYTES + CellTerminalSyncPacket.CHUNK_PAYLOAD_BYTES - 1)
            / CellTerminalSyncPacket.CHUNK_PAYLOAD_BYTES;
        if (this.totalChunks < 2 || this.totalChunks > maximumChunks) {
            throw new IllegalArgumentException("Invalid Cell Terminal chunk count: " + this.totalChunks);
        }
        if (this.chunkIndex < 0 || this.chunkIndex >= this.totalChunks) {
            throw new IllegalArgumentException("Invalid Cell Terminal chunk index: " + this.chunkIndex);
        }
        if (this.payload.length <= 0 || this.payload.length > CellTerminalSyncPacket.CHUNK_PAYLOAD_BYTES
            || this.payload.length > this.totalWireBytes) {
            throw new IllegalArgumentException("Invalid Cell Terminal chunk payload size: " + this.payload.length);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }

        long now = System.nanoTime();
        int currentWindowId = minecraft.player.openContainer.windowId;
        PENDING_TRANSFERS.removeStale(currentWindowId, now);
        if (this.malformed || this.windowId != currentWindowId) {
            return;
        }

        ChunkKey key = new ChunkKey(this.windowId, this.transferId);
        PendingTransfer transfer = PENDING_TRANSFERS.get(key);
        if (transfer == null) {
            transfer = new PendingTransfer(this.totalChunks, this.compressed, this.uncompressedLength,
                this.totalWireBytes, now);
            if (!PENDING_TRANSFERS.reserve(key, transfer)) {
                AELog.warn("Discarding Cell Terminal sync chunk because pending wire budget is exhausted: "
                    + "window=%s, transfer=%s, bytes=%s", this.windowId, this.transferId, this.totalWireBytes);
                return;
            }
        }
        if (!transfer.accept(this.chunkIndex, this.totalChunks, this.compressed, this.uncompressedLength,
            this.totalWireBytes, this.payload, now)) {
            PENDING_TRANSFERS.remove(key);
            AELog.warn("Discarding inconsistent Cell Terminal sync chunk transfer: window=%s, transfer=%s",
                this.windowId, this.transferId);
            return;
        }
        if (!transfer.complete()) {
            return;
        }

        PENDING_TRANSFERS.remove(key);
        CellTerminalSyncPacket.applyPayload(minecraft, this.windowId, transfer.compressed(),
            transfer.uncompressedLength(), transfer.combine());
    }

    record ChunkKey(int windowId, int transferId) {
    }

    static final class PendingTransferMap extends LinkedHashMap<ChunkKey, PendingTransfer> {
        private int pendingWireBytes;

        PendingTransferMap() {
            super(MAX_PENDING_TRANSFERS, 0.75f, true);
        }

        boolean reserve(ChunkKey key, PendingTransfer transfer) {
            if (containsKey(key)) {
                throw new IllegalStateException("Cell Terminal transfer key is already reserved: " + key);
            }
            Map.Entry<ChunkKey, PendingTransfer> eldest = size() == MAX_PENDING_TRANSFERS
                ? entrySet().iterator().next()
                : null;
            long prospectiveBytes = (long) this.pendingWireBytes + transfer.totalWireBytes()
                - (eldest == null ? 0 : eldest.getValue().totalWireBytes());
            if (prospectiveBytes > MAX_PENDING_WIRE_BYTES) {
                return false;
            }
            if (eldest != null) {
                Iterator<Map.Entry<ChunkKey, PendingTransfer>> iterator = entrySet().iterator();
                release(iterator.next().getValue());
                iterator.remove();
            }
            super.put(key, transfer);
            this.pendingWireBytes += transfer.totalWireBytes();
            return true;
        }

        void removeStale(int currentWindowId, long now) {
            Iterator<Map.Entry<ChunkKey, PendingTransfer>> iterator = entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ChunkKey, PendingTransfer> entry = iterator.next();
                PendingTransfer transfer = entry.getValue();
                if (entry.getKey().windowId() != currentWindowId
                    || now - transfer.lastUpdatedNanos() > PENDING_TIMEOUT_NANOS) {
                    release(transfer);
                    iterator.remove();
                }
            }
        }

        void removeWindow(int windowId) {
            Iterator<Map.Entry<ChunkKey, PendingTransfer>> iterator = entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ChunkKey, PendingTransfer> entry = iterator.next();
                if (entry.getKey().windowId() == windowId) {
                    release(entry.getValue());
                    iterator.remove();
                }
            }
        }

        @Override
        public PendingTransfer remove(Object key) {
            PendingTransfer removed = super.remove(key);
            if (removed != null) {
                release(removed);
            }
            return removed;
        }

        private void release(PendingTransfer transfer) {
            this.pendingWireBytes -= transfer.totalWireBytes();
            if (this.pendingWireBytes < 0) {
                throw new IllegalStateException("Cell Terminal pending wire budget underflow");
            }
        }

        @Override
        public void clear() {
            super.clear();
            this.pendingWireBytes = 0;
        }

        @Override
        public PendingTransfer put(ChunkKey key, PendingTransfer value) {
            throw new UnsupportedOperationException("Use reserve() to add Cell Terminal pending transfers");
        }

        @Override
        public void putAll(Map<? extends ChunkKey, ? extends PendingTransfer> map) {
            throw new UnsupportedOperationException("Use reserve() to add Cell Terminal pending transfers");
        }

        int pendingWireBytes() {
            return this.pendingWireBytes;
        }

        @Override
        public Object clone() {
            throw new AssertionError();
        }
    }

    static final class PendingTransfer {
        private final byte[][] chunks;
        private final boolean compressed;
        private final int uncompressedLength;
        private final int totalWireBytes;
        private int receivedWireBytes;
        private int receivedChunks;
        private long lastUpdatedNanos;

        PendingTransfer(int totalChunks, boolean compressed, int uncompressedLength, int totalWireBytes,
                        long lastUpdatedNanos) {
            this.chunks = new byte[totalChunks][];
            this.compressed = compressed;
            this.uncompressedLength = uncompressedLength;
            this.totalWireBytes = totalWireBytes;
            this.lastUpdatedNanos = lastUpdatedNanos;
        }

        boolean accept(int chunkIndex, int totalChunks, boolean compressed, int uncompressedLength, int totalWireBytes,
                       byte[] chunk, long now) {
            if (totalChunks != this.chunks.length || compressed != this.compressed
                || uncompressedLength != this.uncompressedLength || totalWireBytes != this.totalWireBytes
                || chunkIndex < 0 || chunkIndex >= this.chunks.length || chunk.length == 0
                || chunk.length > CellTerminalSyncPacket.CHUNK_PAYLOAD_BYTES) {
                return false;
            }
            byte[] previous = this.chunks[chunkIndex];
            if (previous != null) {
                boolean matches = Arrays.equals(previous, chunk);
                if (matches) {
                    this.lastUpdatedNanos = now;
                }
                return matches;
            }
            if (this.receivedWireBytes + chunk.length > this.totalWireBytes) {
                return false;
            }
            this.chunks[chunkIndex] = Arrays.copyOf(chunk, chunk.length);
            this.receivedWireBytes += chunk.length;
            this.receivedChunks++;
            this.lastUpdatedNanos = now;
            return true;
        }

        int totalWireBytes() {
            return this.totalWireBytes;
        }

        long lastUpdatedNanos() {
            return this.lastUpdatedNanos;
        }

        boolean complete() {
            return this.receivedChunks == this.chunks.length && this.receivedWireBytes == this.totalWireBytes;
        }

        boolean compressed() {
            return this.compressed;
        }

        int uncompressedLength() {
            return this.uncompressedLength;
        }

        byte[] combine() {
            if (!complete()) {
                throw new IllegalStateException("Cannot combine incomplete Cell Terminal chunk transfer");
            }
            byte[] combined = new byte[this.totalWireBytes];
            int offset = 0;
            for (byte[] chunk : this.chunks) {
                System.arraycopy(chunk, 0, combined, offset, chunk.length);
                offset += chunk.length;
            }
            return combined;
        }
    }
}
