package appeng.recipes.handlers;

import appeng.recipes.AERecipeTypes;
import appeng.recipes.IAERecipeFactory;
import appeng.recipes.serializers.JsonRecipeUtils;
import com.google.gson.JsonObject;
import net.minecraftforge.common.crafting.JsonContext;

public class ChargerRecipeSerializer implements IAERecipeFactory {
    @Override
    public void register(JsonObject json, JsonContext ctx) {
        AERecipeTypes.CHARGER.register(new ChargerRecipe(
            JsonRecipeUtils.readIngredient(json, "ingredient", ctx),
            JsonRecipeUtils.readItemStack(json, "result", ctx)));
    }
}
