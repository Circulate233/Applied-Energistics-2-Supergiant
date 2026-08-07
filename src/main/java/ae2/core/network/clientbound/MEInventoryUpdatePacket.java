package ae2.core.network.clientbound;

import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.AEKeyFilter;
import ae2.container.me.common.ContainerMEStorage;
import ae2.container.me.common.GridInventoryEntry;
import ae2.container.me.common.IClientRepo;
import ae2.container.me.common.IncrementalUpdateHelper;
import ae2.core.AELog;
import ae2.core.network.ClientboundPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class MEInventoryUpdatePacket extends ClientboundPacket {
    private static final int UNCOMPRESSED_PACKET_BYTE_LIMIT = 512 * 1024;
    private static final int MIN_ENCODED_ENTRY_BYTES = 5;
    private static final int MAX_ENCODED_ENTRY_COUNT = Short.MAX_VALUE;
    private static final int INITIAL_BUFFER_CAPACITY = 2 * 1024;

    private boolean fullUpdate;
    @Nullable
    private List<GridInventoryEntry> entries;
    private int encodedEntryCount;
    private byte[] encodedEntries;

    public MEInventoryUpdatePacket() {
    }

    public MEInventoryUpdatePacket(boolean fullUpdate, @Nullable List<GridInventoryEntry> entries,
                                   int encodedEntryCount, byte[] encodedEntries) {
        this.fullUpdate = fullUpdate;
        this.entries = entries;
        this.encodedEntryCount = encodedEntryCount;
        this.encodedEntries = encodedEntries;
    }

    public static Builder builder(boolean fullUpdate) {
        return new Builder(fullUpdate);
    }

    public static GridInventoryEntry readEntry(PacketBuffer buffer) {
        long serial = buffer.readVarLong();
        AEKey what = AEKey.readOptionalKey(buffer);
        long storedAmount = buffer.readVarLong();
        long requestableAmount = buffer.readVarLong();
        boolean craftable = buffer.readBoolean();
        return new GridInventoryEntry(serial, what, storedAmount, requestableAmount, craftable);
    }

    private static void writeEntry(PacketBuffer buffer, GridInventoryEntry entry) {
        buffer.writeVarLong(entry.serial());
        AEKey.writeOptionalKey(buffer, entry.what());
        buffer.writeVarLong(entry.storedAmount());
        buffer.writeVarLong(entry.requestableAmount());
        buffer.writeBoolean(entry.craftable());
    }

    private static List<GridInventoryEntry> decodeEntriesPayload(int entryCount, byte[] payload) {
        ByteBuf raw = Unpooled.wrappedBuffer(payload);
        try {
            PacketBuffer data = new PacketBuffer(raw);
            List<GridInventoryEntry> entries = new ObjectArrayList<>(entryCount);
            for (int i = 0; i < entryCount; i++) {
                entries.add(readEntry(data));
            }
            if (data.isReadable()) {
                throw new IllegalArgumentException("Trailing ME inventory update payload bytes: " + data.readableBytes());
            }
            return entries;
        } finally {
            raw.release();
        }
    }

    private static byte[] encodeEntries(List<GridInventoryEntry> entries) {
        PacketBuffer encoded = new PacketBuffer(Unpooled.buffer(INITIAL_BUFFER_CAPACITY,
            UNCOMPRESSED_PACKET_BYTE_LIMIT));
        try {
            for (int i = 0; i < entries.size(); i++) {
                writeEntry(encoded, entries.get(i));
            }
            byte[] payload = new byte[encoded.writerIndex()];
            encoded.getBytes(0, payload);
            return payload;
        } catch (IndexOutOfBoundsException e) {
            AELog.error(e, "ME inventory update entries exceeded the 512 KiB uncompressed packet limit");
            throw new IllegalArgumentException("ME inventory update entries exceeded the uncompressed packet limit",
                e);
        } finally {
            encoded.release();
        }
    }

    @Nullable
    private static byte[] encodeEntry(GridInventoryEntry entry) {
        PacketBuffer encoded = new PacketBuffer(Unpooled.buffer(INITIAL_BUFFER_CAPACITY,
            UNCOMPRESSED_PACKET_BYTE_LIMIT));
        try {
            writeEntry(encoded, entry);
            byte[] payload = new byte[encoded.writerIndex()];
            encoded.getBytes(0, payload);
            return payload;
        } catch (IndexOutOfBoundsException e) {
            AELog.error(e, String.format(
                "Skipping ME inventory update entry serial %d because it exceeds the 512 KiB uncompressed packet limit",
                entry.serial()));
            return null;
        } finally {
            encoded.release();
        }
    }

    private static void validatePayload(int entryCount, byte[] payload) {
        if (entryCount < 0 || entryCount > MAX_ENCODED_ENTRY_COUNT
            || entryCount > payload.length / MIN_ENCODED_ENTRY_BYTES) {
            throw new IllegalArgumentException("Invalid ME inventory update entry count: " + entryCount);
        }
        if (payload.length > UNCOMPRESSED_PACKET_BYTE_LIMIT) {
            throw new IllegalArgumentException("ME inventory update payload exceeds its uncompressed limit: "
                + payload.length);
        }
    }

    @Override
    protected void read(ByteBuf buf) {
        PacketBuffer data = new PacketBuffer(buf);
        this.fullUpdate = data.readBoolean();
        int entryCount = data.readVarInt();
        if (data.readableBytes() < 9) {
            throw new IllegalArgumentException("Incomplete ME inventory update payload metadata");
        }
        boolean compressed = data.readBoolean();
        int uncompressedLength = data.readInt();
        int payloadLength = data.readInt();
        if (payloadLength < 0 || payloadLength > UNCOMPRESSED_PACKET_BYTE_LIMIT
            || uncompressedLength < 0 || uncompressedLength > UNCOMPRESSED_PACKET_BYTE_LIMIT
            || payloadLength != data.readableBytes()) {
            throw new IllegalArgumentException("Invalid ME inventory update payload length: " + payloadLength);
        }
        if (entryCount < 0 || entryCount > MAX_ENCODED_ENTRY_COUNT
            || entryCount > uncompressedLength / MIN_ENCODED_ENTRY_BYTES) {
            throw new IllegalArgumentException("Invalid ME inventory update entry count: " + entryCount);
        }

        this.encodedEntryCount = entryCount;
        byte[] payload = new byte[payloadLength];
        data.readBytes(payload);
        this.encodedEntries = TerminalPayloadCodec.decode(compressed, uncompressedLength, payload,
            UNCOMPRESSED_PACKET_BYTE_LIMIT);
        this.entries = decodeEntriesPayload(this.encodedEntryCount, this.encodedEntries);
    }

    @Override
    protected void write(ByteBuf buf) {
        PacketBuffer data = new PacketBuffer(buf);
        data.writeBoolean(this.fullUpdate);

        byte[] payload;
        int entryCount;
        if (this.encodedEntries != null) {
            payload = this.encodedEntries;
            entryCount = this.encodedEntryCount;
        } else {
            payload = this.entries == null ? new byte[0] : encodeEntries(this.entries);
            entryCount = this.entries == null ? 0 : this.entries.size();
        }

        validatePayload(entryCount, payload);
        TerminalPayloadCodec.EncodedPayload encoded = TerminalPayloadCodec.encode(payload,
            UNCOMPRESSED_PACKET_BYTE_LIMIT);
        data.writeVarInt(entryCount);
        data.writeBoolean(encoded.compressed());
        data.writeInt(encoded.uncompressedLength());
        data.writeInt(encoded.payload().length);
        data.writeBytes(encoded.payload());
    }

    @Override
    public void handleClient(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        if (!(minecraft.player.openContainer instanceof ContainerMEStorage meContainer)) {
            return;
        }

        IClientRepo clientRepo = meContainer.getClientRepo();
        if (clientRepo == null) {
            AELog.info("Ignoring ME inventory update packet because no client repo is available.");
            return;
        }

        List<GridInventoryEntry> actualEntries = this.entries;
        if (actualEntries == null && this.encodedEntries != null) {
            actualEntries = decodeEntriesPayload(this.encodedEntryCount, this.encodedEntries);
        }

        if (actualEntries != null) {
            clientRepo.handleUpdate(this.fullUpdate, actualEntries);
        }
    }

    public static class Builder {
        private final List<MEInventoryUpdatePacket> packets = new ObjectArrayList<>();
        private boolean fullUpdate;
        @Nullable
        private PacketBuffer encodedEntries;
        private int entryCount;
        @Nullable
        private AEKeyFilter filter;

        public Builder(boolean fullUpdate) {
            this.fullUpdate = fullUpdate;
        }

        public void setFilter(@Nullable AEKeyFilter filter) {
            this.filter = filter;
        }

        @SuppressWarnings("unused")
        public void addFull(IncrementalUpdateHelper updateHelper, KeyCounter networkStorage, Set<AEKey> craftables,
                            KeyCounter requestables) {
            Set<AEKey> keys = new ObjectOpenHashSet<>();
            keys.addAll(networkStorage.keySet());
            keys.addAll(craftables);
            keys.addAll(requestables.keySet());

            for (AEKey key : keys) {
                if (this.filter != null && !this.filter.matches(key)) {
                    continue;
                }

                long serial = updateHelper.getOrAssignSerial(key);
                if (!addEntry(new GridInventoryEntry(serial, key, networkStorage.get(key), requestables.get(key),
                    craftables.contains(key)))) {
                    updateHelper.removeSerial(key);
                }
            }
        }

        public void addChanges(IncrementalUpdateHelper updateHelper, KeyCounter networkStorage, Set<AEKey> craftables,
                               KeyCounter requestables) {
            for (AEKey key : updateHelper) {
                if (this.filter != null && !this.filter.matches(key)) {
                    continue;
                }

                Long serial = updateHelper.getSerial(key);
                boolean hasExistingSerial = serial != null;

                if (serial == null) {
                    serial = updateHelper.getOrAssignSerial(key);
                }

                long storedAmount = networkStorage.get(key);
                boolean craftable = craftables.contains(key);
                long requestable = requestables.get(key);
                if (storedAmount <= 0 && requestable <= 0 && !craftable) {
                    addEntry(new GridInventoryEntry(serial, null, 0, 0, false));
                    updateHelper.removeSerial(key);
                } else {
                    AEKey sendKey = hasExistingSerial ? null : key;
                    if (!addEntry(new GridInventoryEntry(serial, sendKey, storedAmount, requestable, craftable))) {
                        if (hasExistingSerial) {
                            addEntry(new GridInventoryEntry(serial, null, 0, 0, false));
                        }
                        updateHelper.removeSerial(key);
                    }
                }
            }

        }

        public void add(GridInventoryEntry entry) {
            addEntry(entry);
        }

        private boolean addEntry(GridInventoryEntry entry) {
            byte[] encodedEntry = encodeEntry(entry);
            if (encodedEntry == null) {
                return false;
            }

            if (this.encodedEntries != null && (this.entryCount == MAX_ENCODED_ENTRY_COUNT
                || this.encodedEntries.writerIndex() + encodedEntry.length > UNCOMPRESSED_PACKET_BYTE_LIMIT)) {
                flushData();
            }

            PacketBuffer data = ensureData();
            data.writeBytes(encodedEntry);
            ++this.entryCount;

            if (data.writerIndex() == UNCOMPRESSED_PACKET_BYTE_LIMIT || this.entryCount == MAX_ENCODED_ENTRY_COUNT) {
                flushData();
            }
            return true;
        }

        public List<MEInventoryUpdatePacket> build() {
            flushData();
            if (this.packets.isEmpty() && this.fullUpdate) {
                this.packets.add(new MEInventoryUpdatePacket(true, null, 0, new byte[0]));
                this.fullUpdate = false;
            }
            return this.packets;
        }

        public void buildAndSend(Consumer<MEInventoryUpdatePacket> sender) {
            List<MEInventoryUpdatePacket> builtPackets = build();
            for (int i = 0; i < builtPackets.size(); i++) {
                sender.accept(builtPackets.get(i));
            }
        }

        private PacketBuffer ensureData() {
            if (this.encodedEntries == null) {
                this.encodedEntries = new PacketBuffer(Unpooled.buffer(INITIAL_BUFFER_CAPACITY,
                    UNCOMPRESSED_PACKET_BYTE_LIMIT));
            }
            return this.encodedEntries;
        }

        private void flushData() {
            if (this.encodedEntries != null) {
                byte[] payload = new byte[this.encodedEntries.writerIndex()];
                this.encodedEntries.getBytes(0, payload);
                this.packets.add(new MEInventoryUpdatePacket(this.fullUpdate, null, this.entryCount,
                    payload));
                this.encodedEntries.release();
                this.encodedEntries = null;
                this.entryCount = 0;
                this.fullUpdate = false;
            }
        }
    }
}
