package ae2.mixins.hei;

import ae2.integration.modules.hei.GenericIngredientHelper;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mezz.jei.gui.recipes.RecipeLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RecipeLayout.class, remap = false)
public class MixinRecipeLayout {

    @ModifyExpressionValue(
        method = "getIngredientUnderMouse(II)Ljava/lang/Object;",
        at = @At(value = "INVOKE", target = "Lmezz/jei/gui/ingredients/GuiIngredient;getDisplayedIngredient()Ljava/lang/Object;"))
    private Object ae2$unwrapCellViewIngredient(Object ingredient) {
        return GenericIngredientHelper.unwrapWrappedIngredient(ingredient);
    }
}
