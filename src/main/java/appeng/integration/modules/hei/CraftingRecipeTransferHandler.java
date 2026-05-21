package appeng.integration.modules.hei;

import appeng.container.me.items.ContainerCraftingTerm;
import appeng.core.localization.ItemModText;
import appeng.integration.modules.itemlists.CraftingHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public class CraftingRecipeTransferHandler implements IRecipeTransferHandler<ContainerCraftingTerm> {
    private static final int RECIPE_OUTPUT_SLOT = 0;
    private static final int CRAFTING_GRID_SIZE = 9;

    private final IRecipeTransferHandlerHelper handlerHelper;

    public CraftingRecipeTransferHandler(IRecipeTransferHandlerHelper handlerHelper) {
        this.handlerHelper = handlerHelper;
    }

    private static List<List<ItemStack>> createEmptyTemplates() {
        List<List<ItemStack>> templates = new ObjectArrayList<>(CRAFTING_GRID_SIZE);
        for (int i = 0; i < CRAFTING_GRID_SIZE; i++) {
            templates.add(new ObjectArrayList<>());
        }
        return templates;
    }

    @Override
    public Class<ContainerCraftingTerm> getContainerClass() {
        return ContainerCraftingTerm.class;
    }

    @Override
    public IRecipeTransferError transferRecipe(@Nonnull ContainerCraftingTerm container,
                                               @Nonnull IRecipeLayout recipeLayout,
                                               @Nonnull EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        ExtractedRecipe extractedRecipe = extractRecipe(recipeLayout);
        if (extractedRecipe == null) {
            return this.handlerHelper.createInternalError();
        }

        if (extractedRecipe.tooLarge) {
            return this.handlerHelper.createUserErrorWithTooltip(ItemModText.RecipeTooLarge.getLocal());
        }

        Int2ObjectMap<Ingredient> slotToIngredientMap = extractedRecipe.ingredients;
        ContainerCraftingTerm.MissingIngredientSlots missingSlots = container.findMissingIngredients(slotToIngredientMap);
        if (missingSlots.missingSlots().size() == slotToIngredientMap.size()) {
            return this.handlerHelper.createUserErrorForSlots(ItemModText.NoItems.getLocal(),
                missingSlots.missingSlots());
        }

        if (doTransfer) {
            CraftingHelper.performTransfer(container, null, extractedRecipe.templates, GuiScreen.isCtrlKeyDown());
        }

        return null;
    }

    private @Nullable ExtractedRecipe extractRecipe(IRecipeLayout recipeLayout) {
        Map<Integer, ? extends IGuiIngredient<ItemStack>> guiIngredients = recipeLayout.getIngredientsGroup(
            ItemStack.class).getGuiIngredients();
        Int2ObjectMap<Ingredient> ingredients = new Int2ObjectOpenHashMap<>();
        List<List<ItemStack>> templates = createEmptyTemplates();
        boolean tooLarge = false;

        for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> entry : guiIngredients.entrySet()) {
            IGuiIngredient<ItemStack> guiIngredient = entry.getValue();
            if (guiIngredient == null || !guiIngredient.isInput()) {
                continue;
            }

            int guiSlot = entry.getKey();
            int gridSlot = guiSlot - 1;
            if (guiSlot == RECIPE_OUTPUT_SLOT || gridSlot < 0) {
                continue;
            }
            if (gridSlot >= CRAFTING_GRID_SIZE) {
                tooLarge = true;
                continue;
            }

            List<ItemStack> allIngredients = guiIngredient.getAllIngredients();
            if (allIngredients == null || allIngredients.isEmpty()) {
                continue;
            }

            List<ItemStack> stacks = new ObjectArrayList<>(allIngredients.size());
            for (ItemStack stack : allIngredients) {
                if (stack != null && !stack.isEmpty()) {
                    stacks.add(stack.copy());
                }
            }

            if (stacks.isEmpty()) {
                continue;
            }

            ItemStack[] matchingStacks = stacks.toArray(new ItemStack[0]);
            ingredients.put(gridSlot, Ingredient.fromStacks(matchingStacks));
            templates.set(gridSlot, stacks);
        }

        return ingredients.isEmpty() && !tooLarge ? null : new ExtractedRecipe(ingredients, templates, tooLarge);
    }

    private record ExtractedRecipe(Int2ObjectMap<Ingredient> ingredients, List<List<ItemStack>> templates,
                                   boolean tooLarge) {
    }
}
