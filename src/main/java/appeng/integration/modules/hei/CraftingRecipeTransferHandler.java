package appeng.integration.modules.hei;

import appeng.container.me.items.ContainerCraftingTerm;
import appeng.core.localization.ItemModText;
import appeng.integration.modules.itemlists.CraftingHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CraftingRecipeTransferHandler<T extends ContainerCraftingTerm> implements IRecipeTransferHandler<T> {
    private static final int RECIPE_OUTPUT_SLOT = 0;
    private static final int CRAFTING_GRID_SIZE = 9;

    private final Class<T> containerClass;
    private final IRecipeTransferHandlerHelper handlerHelper;

    public CraftingRecipeTransferHandler(Class<T> containerClass, IRecipeTransferHandlerHelper handlerHelper) {
        this.containerClass = containerClass;
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
    public Class<T> getContainerClass() {
        return containerClass;
    }

    @Override
    public IRecipeTransferError transferRecipe(@Nonnull T container,
                                               @Nonnull IRecipeLayout recipeLayout,
                                               @Nonnull EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        ExtractedRecipe extractedRecipe = extractRecipe(recipeLayout);
        if (extractedRecipe == null) {
            return this.handlerHelper.createInternalError();
        }

        if (extractedRecipe.tooLarge) {
            return this.handlerHelper.createUserErrorWithTooltip(ItemModText.RecipeTooLarge.getLocal());
        }

        if (!doTransfer) {
            ContainerCraftingTerm.MissingIngredientSlots missingSlots =
                container.findMissingIngredients(extractedRecipe.ingredients);
            CraftingRecipeTransferAnalysis analysis = CraftingRecipeTransferAnalysis.analyze(missingSlots,
                extractedRecipe.ingredients.size());
            if (analysis.outcome() != CraftingRecipeTransferAnalysis.Outcome.READY) {
                return CraftingRecipeTransferUserError.create(recipeLayout, analysis);
            }
            return null;
        }

        CraftingHelper.performTransfer(container, null, extractedRecipe.templates, GuiScreen.isCtrlKeyDown());

        return null;
    }

    @SuppressWarnings("unchecked")
    private @Nullable ExtractedRecipe extractRecipe(IRecipeLayout recipeLayout) {
        var guiIngredients = recipeLayout.getIngredientsGroup(VanillaTypes.ITEM).getGuiIngredients();
        Int2ObjectMap<Ingredient> ingredients = new Int2ObjectOpenHashMap<>();
        List<List<ItemStack>> templates = createEmptyTemplates();
        boolean craftingLayout = VanillaRecipeCategoryUid.CRAFTING.equals(recipeLayout.getRecipeCategory().getUid());
        boolean tooLarge = false;
        int nextProcessingSlot = 0;

        Map.Entry<Integer, IGuiIngredient<ItemStack>>[] entries;

        if (guiIngredients instanceof Int2ObjectMap<? extends IGuiIngredient<ItemStack>> map) {
            var a = map.int2ObjectEntrySet().toArray(Int2ObjectMap.Entry[]::new);
            Arrays.sort(a, Comparator.comparingInt(Int2ObjectMap.Entry::getIntKey));
            entries = a;
        } else {
            entries = guiIngredients.entrySet().toArray(Map.Entry[]::new);
            Arrays.sort(entries, Comparator.comparingInt(Map.Entry::getKey));
        }

        for (var entry : entries) {
            IGuiIngredient<ItemStack> guiIngredient = entry.getValue();
            if (guiIngredient == null || !guiIngredient.isInput()) {
                continue;
            }

            int gridSlot;
            if (craftingLayout) {
                int guiSlot;
                if (entry instanceof Int2ObjectMap.Entry<IGuiIngredient<ItemStack>> e) {
                    guiSlot = e.getIntKey();
                } else {
                    guiSlot = entry.getKey();
                }
                gridSlot = guiSlot - 1;
                if (guiSlot == RECIPE_OUTPUT_SLOT || gridSlot < 0) {
                    continue;
                }
                if (gridSlot >= CRAFTING_GRID_SIZE) {
                    tooLarge = true;
                    continue;
                }
            } else {
                if (nextProcessingSlot >= CRAFTING_GRID_SIZE) {
                    continue;
                }
                gridSlot = nextProcessingSlot++;
            }

            List<ItemStack> allIngredients = guiIngredient.getAllIngredients();
            if (allIngredients == null || allIngredients.isEmpty()) {
                continue;
            }

            List<ItemStack> stacks = new ObjectArrayList<>(allIngredients.size());
            addTemplateStack(stacks, guiIngredient.getDisplayedIngredient());
            for (ItemStack stack : allIngredients) {
                addTemplateStack(stacks, stack);
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

    private static void addTemplateStack(List<ItemStack> stacks, @Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        ItemStack template = stack.copy();
        template.setCount(Math.clamp(template.getCount(), 1, 64));
        for (ItemStack existing : stacks) {
            if (ItemStack.areItemStacksEqual(existing, template)) {
                return;
            }
        }
        stacks.add(template);
    }

    private record ExtractedRecipe(Int2ObjectMap<Ingredient> ingredients, List<List<ItemStack>> templates,
                                   boolean tooLarge) {
    }
}
