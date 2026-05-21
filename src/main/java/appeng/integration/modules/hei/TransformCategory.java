package appeng.integration.modules.hei;

import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
class TransformCategory implements IRecipeCategory<TransformRecipeWrapper> {
    static final String UID = "ae2.transform";
    private static final String KEY_CATEGORY = "ae2.rei_jei_integration.transform_category";
    private static final String KEY_EXPLOSION = "ae2.rei_jei_integration.explosion";
    private static final String KEY_SUBMERGE_IN = "ae2.rei_jei_integration.submerge_in";
    private static final int WIDTH = 144;
    private static final int HEIGHT = 72;

    private final IDrawable background;
    private final IDrawable slotDrawable;
    private final IDrawable arrow;

    TransformCategory(IGuiHelper guiHelper) {
        ResourceLocation location = new ResourceLocation(AppEng.MOD_ID, "textures/guis/jei.png");
        this.background = new HeiBackgroundDrawable(WIDTH, HEIGHT);
        this.slotDrawable = guiHelper.createDrawable(location, 0, 34, 18, 18);
        this.arrow = guiHelper.createDrawable(location, 0, 0, 24, 17);
    }

    private static Minecraft minecraft() {
        return Minecraft.getMinecraft();
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return I18n.format(KEY_CATEGORY);
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
    public void drawExtras(Minecraft minecraft) {
        Layout layout = Layout.create();
        this.arrow.draw(minecraft, layout.arrow1X(), layout.yOffset());
        this.arrow.draw(minecraft, layout.arrow2X(), layout.yOffset());
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, TransformRecipeWrapper recipeWrapper,
                          mezz.jei.api.ingredients.IIngredients ingredients) {
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        List<List<ItemStack>> inputs = recipeWrapper.getInputs();
        Layout layout = Layout.create();
        int x = layout.inputX();
        int y = 10;
        int inputCount = inputs.size();
        if (inputCount < 3) {
            y += 9 * (3 - inputCount);
        }
        for (int i = 0; i < inputs.size(); i++) {
            itemStacks.init(i, true, x, y);
            itemStacks.setBackground(i, this.slotDrawable);
            itemStacks.set(i, inputs.get(i));
            y += 18;
            if (y >= 64) {
                y -= 54;
                x += 18;
            }
        }

        String circumstanceText =
            I18n.format(recipeWrapper.getCircumstance().isExplosion() ? KEY_EXPLOSION : KEY_SUBMERGE_IN);
        int textWidth = minecraft().fontRenderer.getStringWidth(circumstanceText);
        recipeWrapper.setTitleDrawData((WIDTH - textWidth) / 2, circumstanceText);
        recipeWrapper.setFluidDrawData(0, 0);

        int catalystSlot = 100;
        if (recipeWrapper.getCircumstance().isFluid()) {
            Fluid fluid = recipeWrapper.getCircumstance().getFluidForRendering();
            if (fluid != null) {
                recipeWrapper.setFluidDrawData(layout.catalystX(), layout.yOffset());
            }
        } else if (recipeWrapper.getCircumstance().isExplosion()) {
            itemStacks.init(catalystSlot, true, layout.catalystX(), layout.yOffset());
            itemStacks.setBackground(catalystSlot, this.slotDrawable);
            itemStacks.set(catalystSlot,
                Arrays.asList(AEBlocks.TINY_TNT.stack(), new ItemStack(net.minecraft.init.Blocks.TNT)));
        }

        itemStacks.init(200, false, layout.outputX(), layout.yOffset());
        itemStacks.setBackground(200, this.slotDrawable);
        itemStacks.set(200, recipeWrapper.getOutputs());
    }

    private record Layout(int inputX, int arrow1X, int catalystX, int arrow2X, int outputX, int yOffset) {
        private static Layout create() {
            int inputX = 10;
            int yOffset = 28;
            int arrow1X = inputX + 25;
            int catalystX = arrow1X + 24 + 6;
            int arrow2X = catalystX + 16 + 5;
            int outputX = arrow2X + 24 + 10;
            return new Layout(inputX, arrow1X, catalystX, arrow2X, outputX, yOffset);
        }
    }
}
