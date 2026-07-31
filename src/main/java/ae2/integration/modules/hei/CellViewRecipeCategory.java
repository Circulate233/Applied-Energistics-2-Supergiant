package ae2.integration.modules.hei;

import ae2.core.AppEng;
import ae2.core.definitions.AEItems;
import ae2.core.localization.GuiText;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.util.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
final class CellViewRecipeCategory implements IRecipeCategory<CellViewRecipe> {
    static final String UID = "ae2.cell_view";

    private static final ResourceLocation JEI_TEXTURE = AppEng.makeId("textures/guis/jei.png");
    static final int WIDTH = 168;
    static final int HEIGHT = 154;
    static final int GRID_X = 3;
    static final int GRID_Y = 25;
    static final int COLUMNS = 9;
    static final int ROWS = 7;
    static final int SLOT_SIZE = 18;
    static final int HEADER_HEIGHT = 24;

    private final IDrawable background;
    private final IDrawable slot;
    private final IDrawable icon;

    CellViewRecipeCategory(IGuiHelper guiHelper) {
        this.background = new HeiBackgroundDrawable(WIDTH, HEIGHT);
        this.slot = guiHelper.createDrawable(JEI_TEXTURE, 0, 34, SLOT_SIZE, SLOT_SIZE);
        this.icon = guiHelper.createDrawableIngredient(AEItems.ITEM_CELL_64K.stack());
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return GuiText.CellViewTitle.getLocal();
    }

    @Override
    public String getModName() {
        return AppEng.MOD_NAME;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, CellViewRecipe recipe, IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        for (int i = 0; i < recipe.slotCount(); i++) {
            int x = GRID_X + i % COLUMNS * SLOT_SIZE;
            int y = GRID_Y + i / COLUMNS * SLOT_SIZE;
            var stack = i < recipe.contents().size() ? recipe.contents().get(i) : null;
            itemStacks.init(i, true, new CellViewIngredientRenderer(stack),
                x, y, SLOT_SIZE, SLOT_SIZE, 1, 1);
            itemStacks.setBackground(i, this.slot);
        }
        itemStacks.set(ingredients);
        itemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            tooltip.clear();
            if (slotIndex >= 0 && slotIndex < recipe.contents().size()) {
                tooltip.addAll(CellViewIngredientRenderer.buildTooltip(recipe.contents().get(slotIndex)));
            }
        });
    }
}
