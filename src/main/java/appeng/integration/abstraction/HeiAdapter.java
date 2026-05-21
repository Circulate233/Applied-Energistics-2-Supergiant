package appeng.integration.abstraction;

import appeng.api.stacks.GenericStack;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface HeiAdapter {

    static HeiAdapter none() {
        return () -> false;
    }

    boolean isEnabled();

    @Nullable
    default Object getCurrentGhostIngredient() {
        return null;
    }

    @Nullable
    default GenericStack ingredientToStack(Object ingredient) {
        return null;
    }

    default ItemStack getDisplayStack(Object ingredient) {
        return ItemStack.EMPTY;
    }
}
