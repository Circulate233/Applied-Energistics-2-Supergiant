package ae2.core.network.clientbound;

import ae2.container.implementations.ContainerCraftingTree;
import ae2.core.network.ClientboundPacket;
import ae2.integration.data.CraftingTreeStackRegistry;
import ae2.integration.data.LiteCraftTreeNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CraftingTreeDataPacket extends ClientboundPacket {
    public static final int CHUNK_BYTES = 128 * 1024;
    public static final int MAX_TOTAL_BYTES = 16 * 1024 * 1024;
    public static final int MAX_CHUNKS = MAX_TOTAL_BYTES / CHUNK_BYTES;

    private static final byte BEGIN = 0;
    private static final byte CHUNK = 1;
    private static final byte COMPLETE = 2;
    private static final byte ERROR = 3;
    private static final AtomicInteger REQUEST_IDS = new AtomicInteger();
    private static final ThreadPoolExecutor ENCODER = new ThreadPoolExecutor(
        1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8), task -> {
        var thread = new Thread(task, "AE2 Crafting Tree Encoder");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.AbortPolicy());

    private byte mode;
    private int windowId;
    private int requestId;
    private int chunkIndex;
    private int chunkCount;
    private int totalBytes;
    private byte[] data = new byte[0];

    public CraftingTreeDataPacket() {
    }

    private CraftingTreeDataPacket(byte mode, int windowId, int requestId) {
        this.mode = mode;
        this.windowId = windowId;
        this.requestId = requestId;
    }

    public static int nextRequestId() {
        return REQUEST_IDS.getAndUpdate(value -> value == Integer.MAX_VALUE ? 0 : value + 1);
    }

    public static CraftingTreeDataPacket begin(int windowId, int requestId, int chunkCount, int totalBytes) {
        var packet = new CraftingTreeDataPacket(BEGIN, windowId, requestId);
        packet.chunkCount = chunkCount;
        packet.totalBytes = totalBytes;
        return packet;
    }

    public static CraftingTreeDataPacket chunk(int windowId, int requestId, int index, byte[] data) {
        var packet = new CraftingTreeDataPacket(CHUNK, windowId, requestId);
        packet.chunkIndex = index;
        packet.data = data;
        return packet;
    }

    public static CraftingTreeDataPacket complete(int windowId, int requestId) {
        return new CraftingTreeDataPacket(COMPLETE, windowId, requestId);
    }

    public static CraftingTreeDataPacket error(int windowId, int requestId) {
        return new CraftingTreeDataPacket(ERROR, windowId, requestId);
    }

    public static CompletableFuture<byte[]> encodeAsync(LiteCraftTreeNode root) {
        var result = new CompletableFuture<byte[]>();
        try {
            ENCODER.execute(() -> {
                try {
                    result.complete(encode(root));
                } catch (RuntimeException failure) {
                    result.completeExceptionally(failure);
                }
            });
        } catch (RuntimeException failure) {
            result.completeExceptionally(failure);
        }
        return result;
    }

    private static byte[] encode(LiteCraftTreeNode root) {
        var registry = new CraftingTreeStackRegistry();
        ByteBuf tree = Unpooled.buffer();
        ByteBuf payload = Unpooled.buffer();
        try {
            root.writeToBuffer(tree, registry);
            registry.write(payload);
            payload.writeBytes(tree);
            if (payload.readableBytes() > MAX_TOTAL_BYTES) {
                throw new IllegalArgumentException("Crafting tree payload exceeds " + MAX_TOTAL_BYTES + " bytes");
            }
            var result = new byte[payload.readableBytes()];
            payload.readBytes(result);
            return result;
        } finally {
            tree.release();
            payload.release();
        }
    }

    @Override
    protected void read(ByteBuf buf) {
        var packet = new PacketBuffer(buf);
        mode = buf.readByte();
        windowId = packet.readVarInt();
        requestId = packet.readVarInt();
        if (mode == BEGIN) {
            chunkCount = packet.readVarInt();
            totalBytes = packet.readVarInt();
        } else if (mode == CHUNK) {
            chunkIndex = packet.readVarInt();
            int size = packet.readVarInt();
            if (size <= 0 || size > CHUNK_BYTES || size > packet.readableBytes()) {
                throw new IllegalArgumentException("Invalid crafting tree chunk size: " + size);
            }
            data = new byte[size];
            packet.readBytes(data);
        } else if (mode != COMPLETE && mode != ERROR) {
            throw new IllegalArgumentException("Unknown crafting tree packet mode: " + mode);
        }
        if (packet.isReadable()) {
            throw new IllegalArgumentException("Trailing crafting tree packet bytes: " + packet.readableBytes());
        }
    }

    @Override
    protected void write(ByteBuf buf) {
        var packet = new PacketBuffer(buf);
        buf.writeByte(mode);
        packet.writeVarInt(windowId);
        packet.writeVarInt(requestId);
        if (mode == BEGIN) {
            packet.writeVarInt(chunkCount);
            packet.writeVarInt(totalBytes);
        } else if (mode == CHUNK) {
            packet.writeVarInt(chunkIndex);
            packet.writeVarInt(data.length);
            packet.writeBytes(data);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        if (minecraft.player == null
            || !(minecraft.player.openContainer instanceof ContainerCraftingTree container)
            || container.windowId != windowId) {
            return;
        }
        var transfer = container.getTransfer();
        if (mode == BEGIN) {
            var result = transfer.begin(requestId, chunkCount, totalBytes);
            if (result == CraftingTreeTransferAccumulator.Result.ACCEPTED) {
                container.setClientLoading();
            } else if (result == CraftingTreeTransferAccumulator.Result.ERROR) {
                transfer.fail(requestId);
                container.setClientError("protocol");
            }
        } else if (mode == CHUNK) {
            if (transfer.chunk(requestId, chunkIndex, data) == CraftingTreeTransferAccumulator.Result.ERROR) {
                container.setClientError("protocol");
            }
        } else if (mode == COMPLETE) {
            var completion = transfer.complete(requestId);
            if (completion.result() == CraftingTreeTransferAccumulator.Result.IGNORED) {
                return;
            }
            if (completion.result() != CraftingTreeTransferAccumulator.Result.COMPLETE) {
                container.setClientError("protocol");
                return;
            }
            try {
                container.setClientRoot(decode(completion.payload()));
            } catch (RuntimeException failure) {
                container.setClientError("decode");
            }
        } else {
            if (transfer.fail(requestId) == CraftingTreeTransferAccumulator.Result.ACCEPTED) {
                container.setClientError("server");
            }
        }
    }

    private static LiteCraftTreeNode decode(byte[] payload) {
        var buffer = Unpooled.wrappedBuffer(payload);
        try {
            var limits = new CraftingTreeStackRegistry.DecodeLimits();
            var registry = new CraftingTreeStackRegistry();
            registry.read(buffer, limits);
            var root = LiteCraftTreeNode.fromBuffer(buffer, registry, null, limits, 0);
            if (buffer.isReadable()) {
                throw new IllegalArgumentException("Trailing crafting tree payload bytes: " + buffer.readableBytes());
            }
            return root;
        } finally {
            buffer.release();
        }
    }

    public static byte[] chunkBytes(byte[] payload, int index) {
        int from = index * CHUNK_BYTES;
        return Arrays.copyOfRange(payload, from, Math.min(from + CHUNK_BYTES, payload.length));
    }
}
