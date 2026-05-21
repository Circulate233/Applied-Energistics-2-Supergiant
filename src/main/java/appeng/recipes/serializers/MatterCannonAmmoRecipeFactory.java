package appeng.recipes.serializers;

import appeng.recipes.mattercannon.MatterCannonAmmoSerializer;

public final class MatterCannonAmmoRecipeFactory extends AERecipeFactoryAdapter {
    public MatterCannonAmmoRecipeFactory() {
        super(new MatterCannonAmmoSerializer());
    }
}
