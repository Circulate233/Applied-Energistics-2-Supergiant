package appeng.recipes.serializers;

import appeng.recipes.game.StorageCellDisassemblyRecipeSerializer;

public final class StorageCellDisassemblyRecipeFactory extends AERecipeFactoryAdapter {
    public StorageCellDisassemblyRecipeFactory() {
        super(new StorageCellDisassemblyRecipeSerializer());
    }
}
