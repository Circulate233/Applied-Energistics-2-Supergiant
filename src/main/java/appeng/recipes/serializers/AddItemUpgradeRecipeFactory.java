package appeng.recipes.serializers;

import appeng.recipes.game.AddItemUpgradeRecipe;
import com.google.gson.JsonObject;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

public class AddItemUpgradeRecipeFactory implements IRecipeFactory {
    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {
        return new AddItemUpgradeRecipe();
    }
}
