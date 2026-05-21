package appeng.integration.modules.hei;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.container.me.items.ContainerPatternEncodingTerm;
import appeng.container.slot.FakeSlot;
import appeng.core.network.InitNetwork;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.parts.encoding.EncodingMode;
import appeng.util.GenericContainerHelper;
import com.google.common.math.LongMath;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PatternEncodingRecipeTransferHandler implements IRecipeTransferHandler<ContainerPatternEncodingTerm> {
    @SuppressWarnings("unused")
    private static final int RECIPE_OUTPUT_SLOT = 0;
    private static final int CRAFTING_GRID_SIZE = 9;

    @Override
    public Class<ContainerPatternEncodingTerm> getContainerClass() {
        return ContainerPatternEncodingTerm.class;
    }

    @Override
    public IRecipeTransferError transferRecipe(@Nonnull ContainerPatternEncodingTerm container,
                                               @Nonnull IRecipeLayout recipeLayout,
                                               @Nonnull EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        if (recipeLayout.getRecipeCategory().getUid().equals(VanillaRecipeCategoryUid.INFORMATION)
            || recipeLayout.getRecipeCategory().getUid().equals(VanillaRecipeCategoryUid.FUEL)) {
            return null;
        }

        if (!doTransfer) {
            return null;
        }

        if (recipeLayout.getRecipeCategory().getUid().equals(VanillaRecipeCategoryUid.CRAFTING)) {
            encodeCraftingRecipe(container, recipeLayout);
        } else {
            encodeProcessingRecipe(container, recipeLayout);
        }
        return null;
    }

    private static void encodeCraftingRecipe(ContainerPatternEncodingTerm container, IRecipeLayout recipeLayout) {
        container.setMode(EncodingMode.CRAFTING);
        List<GenericStack> bookmarkedIngredients = HeiBookmarkHelper.getBookmarkedStacks();
        List<List<GenericStack>> inputs = getCraftingInputs(recipeLayout);

        FakeSlot[] slots = container.getCraftingGridSlots();
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = ItemStack.EMPTY;
            if (i < inputs.size() && !inputs.get(i).isEmpty()) {
                GenericStack genericStack = selectIngredient(inputs.get(i), bookmarkedIngredients, true);
                if (genericStack.what() instanceof AEItemKey itemKey) {
                    stack = itemKey.toStack();
                } else {
                    stack = GenericStack.wrapInItemStack(genericStack.what(), 1);
                }
            }
            setFilter(container, slots[i], stack);
        }

        for (FakeSlot slot : container.getProcessingOutputSlots()) {
            setFilter(container, slot, ItemStack.EMPTY);
        }
    }

    private static void encodeProcessingRecipe(ContainerPatternEncodingTerm container, IRecipeLayout recipeLayout) {
        container.setMode(EncodingMode.PROCESSING);
        List<GenericStack> bookmarkedIngredients = HeiBookmarkHelper.getBookmarkedStacks();

        encodeSelectedStacksIntoSlots(container, getGenericInputs(recipeLayout), bookmarkedIngredients,
            container.getProcessingInputSlots());
        encodeSelectedStacksIntoSlots(container, getGenericOutputs(recipeLayout), Collections.emptyList(),
            container.getProcessingOutputSlots());
    }

    private static List<List<GenericStack>> getGenericInputs(IRecipeLayout recipeLayout) {
        return GenericIngredientHelper.getIngredients(recipeLayout, true, false, CRAFTING_GRID_SIZE);
    }

    private static List<List<GenericStack>> getGenericOutputs(IRecipeLayout recipeLayout) {
        return GenericIngredientHelper.getIngredients(recipeLayout, false, false, CRAFTING_GRID_SIZE);
    }

    private static List<List<GenericStack>> getCraftingInputs(IRecipeLayout recipeLayout) {
        return GenericIngredientHelper.getIngredients(recipeLayout, true, true, CRAFTING_GRID_SIZE);
    }

    private static void encodeSelectedStacksIntoSlots(ContainerPatternEncodingTerm container,
                                                      List<List<GenericStack>> possibleInputsBySlot,
                                                      List<GenericStack> bookmarkedIngredients,
                                                      FakeSlot[] slots) {
        List<GenericStack> encodedInputs = new ObjectArrayList<>();
        for (List<GenericStack> genericIngredient : possibleInputsBySlot) {
            if (!genericIngredient.isEmpty()) {
                addOrMerge(encodedInputs, selectIngredient(genericIngredient, bookmarkedIngredients, false));
            }
        }

        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = i < encodedInputs.size() ? GenericStack.wrapInItemStack(encodedInputs.get(i))
                : ItemStack.EMPTY;
            setFilter(container, slots[i], stack);
        }
    }

    private static GenericStack selectIngredient(List<GenericStack> possibleIngredients,
                                                 List<GenericStack> bookmarkedIngredients,
                                                 boolean preferFilledBucket) {
        if (preferFilledBucket) {
            for (GenericStack possibleIngredient : possibleIngredients) {
                if (isFilledBucketIngredient(possibleIngredient)) {
                    return possibleIngredient;
                }
            }
        }

        for (GenericStack bookmarkedIngredient : bookmarkedIngredients) {
            for (GenericStack possibleIngredient : possibleIngredients) {
                if (Objects.equals(possibleIngredient.what(), bookmarkedIngredient.what())) {
                    return possibleIngredient;
                }
            }
        }

        return possibleIngredients.stream().findFirst().orElseThrow(IllegalStateException::new);
    }

    private static void addOrMerge(List<GenericStack> stacks, GenericStack newStack) {
        for (int i = 0; i < stacks.size(); i++) {
            GenericStack existingStack = stacks.get(i);
            if (Objects.equals(existingStack.what(), newStack.what())) {
                long newAmount = LongMath.saturatedAdd(existingStack.amount(), newStack.amount());
                stacks.set(i, new GenericStack(newStack.what(), newAmount));

                long overflow = newStack.amount() - (newAmount - existingStack.amount());
                if (overflow > 0) {
                    stacks.add(new GenericStack(newStack.what(), overflow));
                }
                return;
            }
        }

        stacks.add(newStack);
    }

    private static boolean isFilledBucketIngredient(GenericStack stack) {
        if (!(stack.what() instanceof AEItemKey itemKey)) {
            return false;
        }

        ItemStack itemStack = itemKey.toStack();
        return GenericContainerHelper.getContainedFluidStack(itemStack) != null
            && (itemStack.getItem() instanceof ItemBucket || itemStack.getItem() == Items.MILK_BUCKET);
    }

    private static void setFilter(ContainerPatternEncodingTerm container, FakeSlot slot, ItemStack stack) {
        InitNetwork.sendToServer(new InventoryActionPacket(
            container.windowId,
            InventoryAction.SET_FILTER,
            slot.slotNumber,
            stack));
    }
}
