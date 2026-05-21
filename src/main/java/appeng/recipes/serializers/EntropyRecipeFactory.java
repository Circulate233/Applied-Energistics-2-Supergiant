package appeng.recipes.serializers;

import appeng.recipes.entropy.EntropyRecipeSerializer;

public final class EntropyRecipeFactory extends AERecipeFactoryAdapter {
    public EntropyRecipeFactory() {
        super(new EntropyRecipeSerializer());
    }
}
