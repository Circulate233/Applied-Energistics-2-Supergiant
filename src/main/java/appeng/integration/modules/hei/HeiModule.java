package appeng.integration.modules.hei;

import appeng.api.stacks.GenericStack;
import appeng.integration.abstraction.HeiAdapter;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class HeiModule implements HeiAdapter {
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public @Nullable Object getCurrentGhostIngredient() {
        return HeiPlugin.GUI_HANDLER.getCurrentGhostIngredient();
    }

    @Override
    public @Nullable GenericStack ingredientToStack(Object ingredient) {
        return GenericIngredientHelper.ingredientToStack(ingredient);
    }

    @Override
    public ItemStack getDisplayStack(Object ingredient) {
        return AEGuiHandler.toGhostDisplayStack(ingredient);
    }
}
