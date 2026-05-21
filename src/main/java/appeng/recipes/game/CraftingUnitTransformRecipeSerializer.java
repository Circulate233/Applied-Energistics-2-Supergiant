package appeng.recipes.game;

import appeng.recipes.AERecipeTypes;
import appeng.recipes.IAERecipeFactory;
import appeng.recipes.serializers.JsonRecipeUtils;
import com.google.gson.JsonObject;
import net.minecraftforge.common.crafting.JsonContext;

public class CraftingUnitTransformRecipeSerializer implements IAERecipeFactory {
    @Override
    public void register(JsonObject json, JsonContext ctx) {
        AERecipeTypes.CRAFTING_UNIT_TRANSFORM.register(new CraftingUnitTransformRecipe(
            JsonRecipeUtils.readBlock(json, "upgraded_block"),
            JsonRecipeUtils.readItem(json, "upgrade_item")));
    }
}
