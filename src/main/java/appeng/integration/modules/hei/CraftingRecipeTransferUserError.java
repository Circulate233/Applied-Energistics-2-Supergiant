package appeng.integration.modules.hei;

import appeng.core.localization.ItemModText;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntCollections;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.gui.recipes.RecipeLayout;
import mezz.jei.gui.recipes.RecipeTransferButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;

import java.awt.Color;
import java.util.List;
import java.util.Map;

final class CraftingRecipeTransferUserError implements IRecipeTransferError {
    private static final Color MISSING_SLOT_COLOR = new Color(1.0f, 0.0f, 0.0f, 0.4f);
    private static final Color CRAFTABLE_SLOT_COLOR = new Color(0.2f, 0.45f, 1.0f, 0.4f);

    private final List<String> tooltip;
    private final IntCollection missingGuiSlots;
    private final IntCollection craftableGuiSlots;

    private CraftingRecipeTransferUserError(List<String> tooltip,
                                            IntCollection missingGuiSlots,
                                            IntCollection craftableGuiSlots) {
        this.tooltip = List.copyOf(tooltip);
        this.missingGuiSlots = IntCollections.unmodifiable(missingGuiSlots);
        this.craftableGuiSlots = IntCollections.unmodifiable(craftableGuiSlots);
    }

    static IRecipeTransferError create(CraftingRecipeTransferAnalysis analysis) {
        List<String> tooltip = new ObjectArrayList<>();
        tooltip.add(I18n.format("jei.tooltip.transfer"));

        if (analysis.outcome() == CraftingRecipeTransferAnalysis.Outcome.READY) {
            throw new IllegalArgumentException("Ready transfers do not need a user-facing error");
        }

        if (analysis.hasImmediatelyAvailableIngredients()) {
            tooltip.add(TextFormatting.RED + ItemModText.RecipeTransferImportsAvailable.getLocal());
        }
        if (analysis.hasCraftableMissingIngredients()) {
            tooltip.add(TextFormatting.RED + ItemModText.RecipeTransferRequestsCraftableMissing.getLocal());
        }
        if (analysis.hasUncraftableMissingIngredients()) {
            if (analysis.hasCraftableMissingIngredients()) {
                tooltip.add(TextFormatting.RED + ItemModText.RecipeTransferLeavesMissing.getLocal());
            } else if (!analysis.hasImmediatelyAvailableIngredients()) {
                tooltip.add(TextFormatting.RED + ItemModText.NoItems.getLocal());
            } else {
                tooltip.add(TextFormatting.RED + ItemModText.RecipeTransferLeavesMissing.getLocal());
            }
        }

        return new CraftingRecipeTransferUserError(tooltip, analysis.getMissingGuiSlots(),
            analysis.getCraftableGuiSlots());
    }

    private static void drawHighlights(Minecraft minecraft, Map<Integer, ? extends IGuiIngredient<ItemStack>> ingredients,
                                       IntCollection slots, Color color, int recipeX, int recipeY) {
        if (ingredients instanceof Int2ObjectMap<? extends IGuiIngredient<ItemStack>> ingredientMap) {
            for (int slotIndex : slots) {
                IGuiIngredient<ItemStack> ingredient = ingredientMap.get(slotIndex);
                if (ingredient != null) {
                    ingredient.drawHighlight(minecraft, color, recipeX, recipeY);
                }
            }
        } else {
            for (int slotIndex : slots) {
                IGuiIngredient<ItemStack> ingredient = ingredients.get(slotIndex);
                if (ingredient != null) {
                    ingredient.drawHighlight(minecraft, color, recipeX, recipeY);
                }
            }
        }
    }

    @Override
    public Type getType() {
        return Type.USER_FACING;
    }

    @Override
    public void showError(Minecraft minecraft, int mouseX, int mouseY, IRecipeLayout recipeLayout, int recipeX,
                          int recipeY) {
        IGuiItemStackGroup itemStackGroup = recipeLayout.getItemStacks();
        Map<Integer, ? extends IGuiIngredient<ItemStack>> ingredients = itemStackGroup.getGuiIngredients();
        int screenWidth = minecraft.currentScreen != null ? minecraft.currentScreen.width : minecraft.displayWidth;
        int screenHeight = minecraft.currentScreen != null ? minecraft.currentScreen.height : minecraft.displayHeight;

        enableTransferButton(recipeLayout);
        drawHighlights(minecraft, ingredients, missingGuiSlots, MISSING_SLOT_COLOR, recipeX, recipeY);
        drawHighlights(minecraft, ingredients, craftableGuiSlots, CRAFTABLE_SLOT_COLOR, recipeX, recipeY);

        GuiUtils.drawHoveringText(tooltip, mouseX, mouseY, screenWidth, screenHeight, 150,
            minecraft.fontRenderer);
    }

    private void enableTransferButton(IRecipeLayout recipeLayout) {
        if (!(recipeLayout instanceof RecipeLayout concreteLayout)) {
            return;
        }

        RecipeTransferButton button = concreteLayout.getRecipeTransferButton();
        if (button != null) {
            button.enabled = true;
            button.visible = true;
        }
    }
}
