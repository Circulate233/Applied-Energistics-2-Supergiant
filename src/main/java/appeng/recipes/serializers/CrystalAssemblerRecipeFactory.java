package appeng.recipes.serializers;

import appeng.recipes.handlers.CrystalAssemblerRecipeSerializer;

public final class CrystalAssemblerRecipeFactory extends AERecipeFactoryAdapter {
    public CrystalAssemblerRecipeFactory() {
        super(new CrystalAssemblerRecipeSerializer());
    }
}
