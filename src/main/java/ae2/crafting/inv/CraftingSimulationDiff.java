package ae2.crafting.inv;

import ae2.api.crafting.IPatternDetails;
import ae2.api.config.Actionable;
import ae2.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

/**
 * Frozen, single-use changes produced by an isolated crafting simulation.
 */
public final class CraftingSimulationDiff {
    final KeyCounter requiredExtract;
    final KeyCounter inventoryDelta;
    final KeyCounter emittedItems;
    final KeyCounter pseudoItems;
    final Object2LongMap<IPatternDetails> crafts;
    final double bytes;
    private boolean applied;

    CraftingSimulationDiff(KeyCounter requiredExtract, KeyCounter inventoryDelta,
                           KeyCounter emittedItems, KeyCounter pseudoItems,
                           Object2LongMap<IPatternDetails> crafts, double bytes) {
        this.requiredExtract = copy(requiredExtract);
        this.inventoryDelta = copy(inventoryDelta);
        this.emittedItems = copy(emittedItems);
        this.pseudoItems = copy(pseudoItems);
        this.crafts = new Object2LongOpenHashMap<>(crafts);
        this.bytes = bytes;
    }

    public void applyTo(CraftingSimulationState parent) {
        if (this.applied) {
            throw new IllegalStateException("Crafting simulation diff was already applied");
        }
        var snapshot = parent.snapshot();
        try {
            if (!canApplyTo(parent)) {
                throw new IllegalStateException("Crafting simulation diff cannot be applied to parent");
            }
            parent.applyDiff(this);
            this.applied = true;
        } catch (RuntimeException | Error failure) {
            parent.restore(snapshot);
            throw failure;
        }
    }

    public boolean canApplyTo(CraftingSimulationState parent) {
        var test = new ChildCraftingSimulationState(parent);
        for (var entry : inventoryDelta) {
            if (entry.getLongValue() < 0
                && test.extract(entry.getKey(), -entry.getLongValue(), Actionable.MODULATE)
                != -entry.getLongValue()) {
                return false;
            }
        }
        return true;
    }

    public double getBytes() {
        return this.bytes;
    }

    private static KeyCounter copy(KeyCounter source) {
        var result = new KeyCounter();
        result.addAll(source);
        return result;
    }
}
