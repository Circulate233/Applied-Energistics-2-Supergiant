package appeng.capabilities;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.ICrankable;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.me.storage.NullInventory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import org.jspecify.annotations.Nullable;

public final class Capabilities {

    private static boolean registered;

    private Capabilities() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        CapabilityManager.INSTANCE.register(MEStorage.class, createNullStorage(), NullInventory::of);
        CapabilityManager.INSTANCE.register(ICraftingMachine.class, createNullStorage(), NullCraftingMachine::new);
        CapabilityManager.INSTANCE.register(GenericInternalInventory.class, createNullStorage(), EmptyGenericInternalInventory::new);
        CapabilityManager.INSTANCE.register(IInWorldGridNodeHost.class, createNullStorage(), EmptyInWorldGridNodeHost::new);
        CapabilityManager.INSTANCE.register(ICrankable.class, createNullStorage(), EmptyCrankable::new);
        registered = true;
    }

    private static <T> Capability.IStorage<T> createNullStorage() {
        return new Capability.IStorage<>() {
            @Override
            public NBTBase writeNBT(Capability<T> capability, T instance, EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(Capability<T> capability, T instance, EnumFacing side, NBTBase nbt) {
            }
        };
    }

    private static final class NullCraftingMachine implements ICraftingMachine {
        @Override
        public PatternContainerGroup getCraftingMachineInfo() {
            return PatternContainerGroup.nothing();
        }

        @Override
        public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, EnumFacing ejectionDirection) {
            return false;
        }

        @Override
        public boolean acceptsPlans() {
            return false;
        }
    }

    private static final class EmptyGenericInternalInventory implements GenericInternalInventory {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public @Nullable GenericStack getStack(int slot) {
            return null;
        }

        @Override
        public @Nullable AEKey getKey(int slot) {
            return null;
        }

        @Override
        public long getAmount(int slot) {
            return 0;
        }

        @Override
        public long getMaxAmount(AEKey key) {
            return 0;
        }

        @Override
        public long getCapacity(AEKeyType keyType) {
            return 0;
        }

        @Override
        public boolean canInsert() {
            return false;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public void setStack(int slot, GenericStack newStack) {
        }

        @Override
        public boolean isSupportedType(AEKeyType type) {
            return false;
        }

        @Override
        public boolean isAllowedIn(int slot, AEKey what) {
            return false;
        }

        @Override
        public long insert(int slot, AEKey what, long amount, Actionable mode) {
            return 0;
        }

        @Override
        public long extract(int slot, AEKey what, long amount, Actionable mode) {
            return 0;
        }

        @Override
        public void beginBatch() {
        }

        @Override
        public void endBatch() {
        }

        @Override
        public void endBatchSuppressed() {
        }

        @Override
        public void onChange() {
        }
    }

    private static final class EmptyInWorldGridNodeHost implements IInWorldGridNodeHost {
        @Override
        public @Nullable IGridNode getGridNode(EnumFacing dir) {
            return null;
        }
    }

    private static final class EmptyCrankable implements ICrankable {
        @Override
        public boolean canTurn() {
            return false;
        }

        @Override
        public void applyTurn() {
        }
    }
}
