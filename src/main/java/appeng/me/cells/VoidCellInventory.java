package appeng.me.cells;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.StorageCell;
import appeng.items.storage.VoidCellItem;
import appeng.text.TextComponentItemStack;
import appeng.util.CellWorkbenchFilter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

public class VoidCellInventory implements StorageCell {
    private final ItemStack stack;

    public VoidCellInventory(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (amount <= 0 || !matches(what)) {
            return 0;
        }
        return amount;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        return StorageCell.super.extract(what, amount, mode, source);
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        StorageCell.super.getAvailableStacks(out);
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        return StorageCell.super.isPreferredStorageFor(what, source);
    }

    @Override
    public CellState getStatus() {
        return CellState.EMPTY;
    }

    @Override
    public double getIdleDrain() {
        return 0.0d;
    }

    @Override
    public boolean canFitInsideCell() {
        return false;
    }

    @Override
    public ITextComponent getDescription() {
        return TextComponentItemStack.of(stack);
    }

    @Override
    public void persist() {
    }

    private boolean matches(AEKey key) {
        if (!(stack.getItem() instanceof VoidCellItem voidCellItem)) {
            return false;
        }

        return CellWorkbenchFilter.matches(
            stack,
            voidCellItem,
            key,
            CellWorkbenchFilter.isInverted(stack, voidCellItem),
            CellWorkbenchFilter.isFuzzy(stack, voidCellItem));
    }
}
