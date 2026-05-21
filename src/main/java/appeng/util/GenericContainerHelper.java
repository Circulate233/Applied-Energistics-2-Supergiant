package appeng.util;

import appeng.api.stacks.GenericStack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Allows generalized extraction from item-based containers such as buckets or tanks.
 */
public final class GenericContainerHelper {
    private GenericContainerHelper() {
    }

    @Nullable
    public static GenericStack getContainedFluidStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        FluidStack content = FluidUtil.getFluidContained(stack);
        return content != null ? GenericStack.fromFluidStack(content) : null;
    }
}
