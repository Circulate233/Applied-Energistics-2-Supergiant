package appeng.recipes.serializers;

import appeng.recipes.handlers.InscriberRecipeSerializer;

public final class InscriberRecipeFactory extends AERecipeFactoryAdapter {
    public InscriberRecipeFactory() {
        super(new InscriberRecipeSerializer());
    }
}
