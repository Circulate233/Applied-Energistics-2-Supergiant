package appeng.recipes.game;

import appeng.recipes.IAERecipeFactory;
import appeng.recipes.serializers.JsonRecipeUtils;
import com.google.gson.JsonObject;
import net.minecraftforge.common.crafting.JsonContext;

public class StorageCellDisassemblyRecipeSerializer implements IAERecipeFactory {
    @Override
    public void register(JsonObject json, JsonContext ctx) {
        StorageCellDisassemblyRecipe.register(new StorageCellDisassemblyRecipe(
            JsonRecipeUtils.readItem(json, "cell"),
            JsonRecipeUtils.readItemStacks(json, "cell_disassembly_items", ctx)));
    }
}
