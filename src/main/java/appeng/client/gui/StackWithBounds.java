package appeng.client.gui;

import appeng.api.stacks.GenericStack;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record StackWithBounds(GenericStack stack, Rect2i bounds) {
    @Nullable
    public static StackWithBounds fromSlot(AEBaseGui<?> screen, Slot slot) {
        ItemStack item = slot.getStack();
        GenericStack stack = GenericStack.unwrapItemStack(item);
        if (stack != null) {
            return new StackWithBounds(
                stack,
                new Rect2i(screen.getGuiLeft() + slot.xPos, screen.getGuiTop() + slot.yPos, 16, 16));
        }
        return null;
    }
}
