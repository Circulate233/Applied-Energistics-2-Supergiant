package appeng.integration.modules.hei;

import appeng.container.me.items.ContainerCraftingTerm;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;

record CraftingRecipeTransferAnalysis(
    Outcome outcome,
    IntSet missingGridSlots,
    IntSet craftableGridSlots,
    int ingredientCount) {

    CraftingRecipeTransferAnalysis {
        missingGridSlots = IntSets.unmodifiable(new IntRBTreeSet(missingGridSlots));
        craftableGridSlots = IntSets.unmodifiable(new IntRBTreeSet(craftableGridSlots));
    }

    static CraftingRecipeTransferAnalysis analyze(ContainerCraftingTerm.MissingIngredientSlots missingSlots,
                                                  int ingredientCount) {
        return of(missingSlots.missingSlots(), missingSlots.craftableSlots(), ingredientCount);
    }

    static CraftingRecipeTransferAnalysis of(IntSet missingGridSlots, IntSet craftableGridSlots,
                                             int ingredientCount) {
        int unavailableCount = missingGridSlots.size() + craftableGridSlots.size();

        Outcome outcome;
        if (unavailableCount == 0) {
            outcome = Outcome.READY;
        } else if (unavailableCount >= ingredientCount) {
            outcome = Outcome.BLOCK_ALL_MISSING;
        } else if (!missingGridSlots.isEmpty() && !craftableGridSlots.isEmpty()) {
            outcome = Outcome.PARTIAL_MIXED;
        } else if (!craftableGridSlots.isEmpty()) {
            outcome = Outcome.PARTIAL_CRAFTABLE;
        } else {
            outcome = Outcome.PARTIAL_UNCRAFTABLE;
        }

        return new CraftingRecipeTransferAnalysis(outcome, missingGridSlots, craftableGridSlots, ingredientCount);
    }

    private static IntCollection toGuiSlots(IntSet gridSlots) {
        IntList result = new IntArrayList(gridSlots.size());
        for (int slot : gridSlots) {
            result.add(slot + 1);
        }
        return result;
    }

    IntSet getUnavailableGridSlots() {
        IntRBTreeSet result = new IntRBTreeSet(missingGridSlots);
        result.addAll(craftableGridSlots);
        return result;
    }

    boolean hasImmediatelyAvailableIngredients() {
        return getUnavailableCount() < ingredientCount;
    }

    boolean hasCraftableMissingIngredients() {
        return !craftableGridSlots.isEmpty();
    }

    boolean hasUncraftableMissingIngredients() {
        return !missingGridSlots.isEmpty();
    }

    IntCollection getMissingGuiSlots() {
        return toGuiSlots(missingGridSlots);
    }

    IntCollection getCraftableGuiSlots() {
        return toGuiSlots(craftableGridSlots);
    }

    private int getUnavailableCount() {
        return missingGridSlots.size() + craftableGridSlots.size();
    }

    enum Outcome {
        READY,
        BLOCK_ALL_MISSING,
        PARTIAL_UNCRAFTABLE,
        PARTIAL_CRAFTABLE,
        PARTIAL_MIXED
    }
}
