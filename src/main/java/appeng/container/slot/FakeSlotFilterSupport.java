package appeng.container.slot;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.EmptyingAction;
import appeng.api.stacks.GenericStack;
import appeng.util.ConfigGuiInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class FakeSlotFilterSupport {
    private FakeSlotFilterSupport() {
    }

    @Nullable
    public static EmptyingAction getEmptyingAction(@Nullable Slot slot, ItemStack carried) {
        if (!(slot instanceof AppEngSlot appEngSlot) || carried.isEmpty()) {
            return null;
        }

        if (!(appEngSlot.getInventory() instanceof ConfigGuiInventory configInv)) {
            return null;
        }

        EmptyingAction emptyingAction = ContainerItemStrategies.getEmptyingAction(carried);
        if (emptyingAction == null) {
            return null;
        }

        ItemStack wrappedStack = GenericStack.wrapInItemStack(new GenericStack(
            emptyingAction.what(),
            emptyingAction.maxAmount()));
        return configInv.isItemValid(appEngSlot.getSlotIndex(), wrappedStack) ? emptyingAction : null;
    }

    @Nullable
    public static GenericStack getEmptyingFilter(@Nullable Slot slot, ItemStack carried) {
        EmptyingAction action = getEmptyingAction(slot, carried);
        if (action == null) {
            return null;
        }
        return new GenericStack(action.what(), action.maxAmount());
    }

    public static ItemStack getPreferredFilterStack(FakeSlot slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (slot.canSetFilterTo(stack)) {
            return stack;
        }

        GenericStack emptyingFilter = getEmptyingFilter(slot, stack);
        if (emptyingFilter != null) {
            return GenericStack.wrapInItemStack(emptyingFilter);
        }

        return ItemStack.EMPTY;
    }
}
