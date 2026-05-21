package appeng.recipes.serializers;

import appeng.recipes.handlers.ChargerRecipeSerializer;

public final class ChargerRecipeFactory extends AERecipeFactoryAdapter {
    public ChargerRecipeFactory() {
        super(new ChargerRecipeSerializer());
    }
}
