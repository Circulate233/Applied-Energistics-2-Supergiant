package appeng.recipes.serializers;

import appeng.recipes.game.CraftingUnitTransformRecipeSerializer;

public final class CraftingUnitTransformRecipeFactory extends AERecipeFactoryAdapter {
    public CraftingUnitTransformRecipeFactory() {
        super(new CraftingUnitTransformRecipeSerializer());
    }
}
