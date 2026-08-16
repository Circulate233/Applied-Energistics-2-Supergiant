package ae2.integration.data;

import ae2.api.stacks.GenericStack;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.network.PacketBuffer;

import java.util.Map;

public class CraftingTreeStackRegistry {
    public static final int MAX_REGISTRY_ENTRIES = 32_768;
    public static final int MAX_TREE_NODES = 32_768;
    public static final int MAX_TREE_PROCESSES = 32_768;
    public static final int MAX_TREE_DEPTH = 128;
    public static final int MAX_CHILDREN_PER_NODE = 16_384;
    public static final int MAX_CHILDREN_PER_PROCESS = 16_384;
    public static final int MAX_MACHINES_PER_PROCESS = 4_096;
    public static final int MAX_MACHINE_LOCATIONS_PER_MACHINE = 256;
    public static final int MAX_MACHINE_LOCATIONS_TOTAL = 4096;

    private final Map<Entry, Entry> entries = new Object2ObjectOpenHashMap<>();
    private final ObjectList<Entry> entryList = new ObjectArrayList<>();

    public int add(GenericStack stack) {
        GenericStack keyStack = new GenericStack(stack.what(), 1);
        Entry entry = entries.get(new Entry(keyStack, -1));
        if (entry == null) {
            if (entryList.size() >= MAX_REGISTRY_ENTRIES) {
                throw new IllegalArgumentException("Crafting tree registry size out of range: "
                    + (entryList.size() + 1));
            }
            entry = new Entry(keyStack, entryList.size());
            entries.put(entry, entry);
            entryList.add(entry);
        }
        return entry.id();
    }

    public GenericStack get(int id) {
        return get((long) id);
    }

    public GenericStack get(long id) {
        if (id < 0 || id >= entryList.size()) {
            throw new IllegalArgumentException("Crafting tree stack id out of range: " + id);
        }
        Entry entry = entryList.get((int) id);
        return new GenericStack(entry.stack().what(), entry.stack().amount());
    }

    public void write(ByteBuf buf) {
        new PacketBuffer(buf).writeVarInt(entryList.size());
        for (int i = 0, size = entryList.size(); i < size; i++) {
            Entry entry = entryList.get(i);
            GenericStack.writeBuffer(entry.stack, new PacketBuffer(buf));
        }
    }

    public void read(ByteBuf buf) {
        read(buf, new DecodeLimits());
    }

    public void read(ByteBuf buf, DecodeLimits limits) {
        int size = new PacketBuffer(buf).readVarInt();
        limits.checkRegistrySize(size);
        for (int i = 0; i < size; i++) {
            addInternal(GenericStack.readBuffer(new PacketBuffer(buf)));
        }
    }

    private void addInternal(GenericStack stack) {
        Entry entry = new Entry(stack, entryList.size());
        entryList.add(entry);
        entries.put(entry, entry);
    }

    private record Entry(GenericStack stack, int id) {
        @Override
        public int hashCode() {
            return stack.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Entry entry && stack.equals(entry.stack);
        }
    }

    public static final class DecodeLimits {
        private int nodes;
        private int processes;
        private int machineLocations;

        void checkRegistrySize(int size) {
            if (size < 0 || size > MAX_REGISTRY_ENTRIES) {
                throw new IllegalArgumentException("Crafting tree registry size out of range: " + size);
            }
        }

        void addNode(int depth) {
            if (depth > MAX_TREE_DEPTH) {
                throw new IllegalArgumentException("Crafting tree depth limit exceeded: " + depth);
            }
            nodes++;
            if (nodes > MAX_TREE_NODES) {
                throw new IllegalArgumentException("Crafting tree node limit exceeded: " + nodes);
            }
        }

        void addProcess() {
            processes++;
            if (processes > MAX_TREE_PROCESSES) {
                throw new IllegalArgumentException("Crafting tree process limit exceeded: " + processes);
            }
        }

        void checkNodeChildCount(int size) {
            if (size < 0 || size > MAX_CHILDREN_PER_NODE) {
                throw new IllegalArgumentException("Crafting tree node child count out of range: " + size);
            }
        }

        void checkProcessChildCount(int size) {
            if (size < 0 || size > MAX_CHILDREN_PER_PROCESS) {
                throw new IllegalArgumentException("Crafting tree process child count out of range: " + size);
            }
        }

        void checkMachineCount(int size) {
            if (size < 0 || size > MAX_MACHINES_PER_PROCESS) {
                throw new IllegalArgumentException("Crafting tree machine count out of range: " + size);
            }
        }

        void addMachineLocations(int size) {
            if (size < 0 || size > MAX_MACHINE_LOCATIONS_PER_MACHINE) {
                throw new IllegalArgumentException("Crafting tree machine location count out of range: " + size);
            }
            machineLocations += size;
            if (machineLocations > MAX_MACHINE_LOCATIONS_TOTAL) {
                throw new IllegalArgumentException("Crafting tree machine location limit exceeded: " + machineLocations);
            }
        }
    }
}
