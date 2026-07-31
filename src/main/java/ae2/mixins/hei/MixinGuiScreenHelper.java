package ae2.mixins.hei;

import ae2.integration.modules.hei.GenericIngredientHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mezz.jei.gui.GuiScreenHelper;
import mezz.jei.input.IClickedIngredient;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GuiScreenHelper.class, remap = false)
public class MixinGuiScreenHelper {

    @WrapOperation(method = "getPluginsIngredientUnderMouse", at = @At(value = "INVOKE", target = "Lmezz/jei/gui/GuiScreenHelper;createClickedIngredient(Ljava/lang/Object;Lnet/minecraft/client/gui/inventory/GuiContainer;)Lmezz/jei/input/IClickedIngredient;"))
    private IClickedIngredient<Object> wrapFluidPacket(final GuiScreenHelper instance, final Object ingredient, final GuiContainer guiContainer, final Operation<IClickedIngredient<Object>> original) {
        return original.call(instance, GenericIngredientHelper.unwrapWrappedIngredient(ingredient), guiContainer);
    }
}
