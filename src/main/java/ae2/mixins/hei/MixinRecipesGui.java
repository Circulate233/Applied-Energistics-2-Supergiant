package ae2.mixins.hei;

import ae2.integration.modules.hei.GenericIngredientHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mezz.jei.gui.recipes.RecipesGui;
import mezz.jei.input.ClickedIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.awt.Rectangle;

@Mixin(value = RecipesGui.class, remap = false)
public class MixinRecipesGui {

    @WrapOperation(
        method = "getIngredientUnderMouse(II)Lmezz/jei/input/IClickedIngredient;",
        at = @At(
            value = "INVOKE",
            target = "Lmezz/jei/input/ClickedIngredient;create(Ljava/lang/Object;Ljava/awt/Rectangle;)Lmezz/jei/input/ClickedIngredient;"))
    private ClickedIngredient<Object> ae2$unwrapCellViewIngredient(Object value, Rectangle area,
                                                                   Operation<ClickedIngredient<Object>> original) {
        return original.call(GenericIngredientHelper.unwrapWrappedIngredient(value), area);
    }
}
