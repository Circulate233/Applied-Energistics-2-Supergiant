package appeng.recipes.serializers;

import appeng.recipes.transform.TransformRecipeSerializer;

public final class TransformRecipeFactory extends AERecipeFactoryAdapter {
    public TransformRecipeFactory() {
        super(new TransformRecipeSerializer());
    }
}
