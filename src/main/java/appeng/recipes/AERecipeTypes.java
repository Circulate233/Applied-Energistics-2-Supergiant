package appeng.recipes;

import appeng.core.AppEng;
import appeng.recipes.entropy.EntropyRecipe;
import appeng.recipes.game.CraftingUnitTransformRecipe;
import appeng.recipes.game.StorageCellDisassemblyRecipe;
import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;
import appeng.recipes.mattercannon.MatterCannonAmmo;
import appeng.recipes.transform.TransformLogic;
import appeng.recipes.transform.TransformRecipe;
import appeng.recipes.types.AERecipeType;

public final class AERecipeTypes {
    public static final AERecipeType<ChargerRecipe> CHARGER = new AERecipeType<>(AppEng.makeId("charger"));
    public static final AERecipeType<InscriberRecipe> INSCRIBER = new AERecipeType<>(AppEng.makeId("inscriber"));
    public static final AERecipeType<MatterCannonAmmo> MATTER_CANNON_AMMO = new AERecipeType<>(
        AppEng.makeId("matter_cannon"));
    public static final AERecipeType<TransformRecipe> TRANSFORM = new AERecipeType<>(AppEng.makeId("transform"));
    public static final AERecipeType<CraftingUnitTransformRecipe> CRAFTING_UNIT_TRANSFORM = new AERecipeType<>(
        AppEng.makeId("crafting_unit_transform"));
    public static final AERecipeType<EntropyRecipe> ENTROPY = new AERecipeType<>(AppEng.makeId("entropy"));

    private AERecipeTypes() {
    }

    public static void clear() {
        CHARGER.clear();
        INSCRIBER.clear();
        MATTER_CANNON_AMMO.clear();
        TRANSFORM.clear();
        TransformLogic.clearCache();
        CRAFTING_UNIT_TRANSFORM.clear();
        ENTROPY.clear();
        StorageCellDisassemblyRecipe.clear();
    }
}
