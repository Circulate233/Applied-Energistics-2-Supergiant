package appeng.integration.modules.hei;

import appeng.recipes.handlers.ChargerRecipe;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;

class ChargerRecipeWrapper implements IRecipeWrapper {
    private final ChargerRecipe recipe;

    ChargerRecipeWrapper(ChargerRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM,
            java.util.Collections.singletonList(java.util.Arrays.asList(this.recipe.getIngredient().getMatchingStacks())));
        ingredients.setOutput(VanillaTypes.ITEM, this.recipe.getResultItem());
    }
}
