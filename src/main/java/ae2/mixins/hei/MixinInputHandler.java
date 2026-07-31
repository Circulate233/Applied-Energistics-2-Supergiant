package ae2.mixins.hei;

import ae2.integration.modules.hei.PatternIngredientLookup;
import mezz.jei.input.InputHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = InputHandler.class, remap = false)
public class MixinInputHandler {

    @ModifyArg(
        method = "handleMouseClickedFocus(ILmezz/jei/input/IClickedIngredient;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lmezz/jei/gui/Focus;<init>(Lmezz/jei/api/recipe/IFocus$Mode;Ljava/lang/Object;)V"),
        index = 1)
    private Object ae2$redirectMouseLookupToPatternOutput(Object ingredient) {
        return PatternIngredientLookup.redirectToPrimaryOutput(ingredient, Minecraft.getMinecraft().world);
    }

    @ModifyArg(
        method = "showRecipeOrUses(Lmezz/jei/api/recipe/IFocus$Mode;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lmezz/jei/gui/Focus;<init>(Lmezz/jei/api/recipe/IFocus$Mode;Ljava/lang/Object;)V"),
        index = 1)
    private Object ae2$redirectKeyLookupToPatternOutput(Object ingredient) {
        return PatternIngredientLookup.redirectToPrimaryOutput(ingredient, Minecraft.getMinecraft().world);
    }
}
