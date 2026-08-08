package ae2.container.pattern;

import ae2.api.behaviors.GenericStackDisplayInventory;
import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.inventories.BaseInternalInventory;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2.container.slot.AppEngSlot;
import ae2.crafting.pattern.EncodedPatternItem;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public abstract class ContainerPattern extends AEBaseContainer {

    protected final ItemStack stack;
    protected final IPatternDetails details;
    protected final List<GenericStack[]> inputs = new ArrayList<>();
    protected final List<GenericStack[]> outputs = new ArrayList<>();
    private boolean valid = true;
    private int cycle;

    protected ContainerPattern(InventoryPlayer playerInventory, ItemStack stack) {
        super(playerInventory, null);
        this.stack = stack.copy();
        if (this.stack.getItem() instanceof EncodedPatternItem<?>) {
            this.details = PatternDetailsHelper.decodePattern(this.stack, playerInventory.player.world);
            this.analyse();
        } else {
            throw new IllegalArgumentException(this.stack.getItem() + " isn't an encoded pattern!");
        }
    }

    protected abstract void analyse();

    protected final void addDisplaySlot(List<GenericStack[]> stacks, int index, int x, int y) {
        this.addSlot(new DisplayOnlySlot(this, stacks, index, x, y), SlotSemantics.STORAGE);
    }

    protected final void invalidate() {
        this.valid = false;
    }

    public final void setCycleItem(int index) {
        this.cycle = index;
    }

    @Override
    public @NotNull ItemStack transferStackInSlot(@NotNull EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canInteractWith(@NotNull EntityPlayer player) {
        return this.details != null && this.valid && super.canInteractWith(player);
    }

    protected GenericStack[] clean(GenericStack[] input) {
        Set<GenericStack> unique = new ObjectOpenHashSet<>();
        unique.addAll(Arrays.asList(input));
        return unique.toArray(new GenericStack[0]);
    }

    public static final class DisplayOnlySlot extends AppEngSlot {

        private final int displayIndex;

        private DisplayOnlySlot(ContainerPattern container, List<GenericStack[]> stacks, int index, int x, int y) {
            super(new PatternDisplayInventory(container, stacks, index), 0, x, y);
            this.displayIndex = index;
            this.setNotDraggable();
        }

        @Override
        public boolean isItemValid(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeStack(@NotNull EntityPlayer player) {
            return false;
        }

        @Override
        public @NotNull ItemStack decrStackSize(int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public void putStack(@NotNull ItemStack stack) {
        }

        @Override
        public int getSlotIndex() {
            return this.displayIndex;
        }
    }

    private static final class PatternDisplayInventory extends BaseInternalInventory
        implements GenericStackDisplayInventory {

        private final ContainerPattern container;
        private final List<GenericStack[]> stacks;
        private final int displayIndex;

        private PatternDisplayInventory(ContainerPattern container, List<GenericStack[]> stacks, int displayIndex) {
            this.container = container;
            this.stacks = stacks;
            this.displayIndex = displayIndex;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slotIndex) {
            return GenericStack.wrapInItemStack(this.getDisplayedStack());
        }

        @Override
        public void setItemDirect(int slotIndex, @NotNull ItemStack stack) {
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        public boolean hasGenericDisplayStack(int slot) {
            return this.getDisplayedStack() != null;
        }

        @Override
        public @Nullable AEKey getDisplayKey(int slot) {
            GenericStack stack = this.getDisplayedStack();
            return stack == null ? null : stack.what();
        }

        @Override
        public long getDisplayAmount(int slot) {
            GenericStack stack = this.getDisplayedStack();
            return stack == null ? 0 : stack.amount();
        }

        @Nullable
        private GenericStack getDisplayedStack() {
            if (this.displayIndex >= this.stacks.size()) {
                return null;
            }
            GenericStack[] alternatives = this.stacks.get(this.displayIndex);
            if (alternatives.length == 0) {
                return null;
            }
            return alternatives[Math.floorMod(this.container.cycle, alternatives.length)];
        }
    }
}
