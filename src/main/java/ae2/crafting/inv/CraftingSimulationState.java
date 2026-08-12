/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package ae2.crafting.inv;

import ae2.api.config.Actionable;
import ae2.api.config.FuzzyMode;
import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.crafting.CraftingCalculation;
import ae2.crafting.CraftingPlan;
import ae2.crafting.graph.CraftingGraphDisplaySnapshot;
import com.google.common.collect.Iterables;
import com.google.common.math.LongMath;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.RandomAccess;

public abstract class CraftingSimulationState implements ICraftingSimulationState {
    record StateSnapshot(KeyCounter unmodifiedCache, KeyCounter modifiableCache,
                         ReferenceOpenHashSet<Object> loadedFuzzyGroups, KeyCounter emittedItems,
                         KeyCounter pseudoItems, KeyCounter boundaryItems, KeyCounter requiredExtract,
                         Object2LongOpenHashMap<IPatternDetails> crafts, double bytes) {
    }

    /**
     * Partial cache of the parent's items, never modified.
     */
    private final KeyCounter unmodifiedCache;
    /**
     * Partial cache of the parent's items, but modifiable. The different between this cache and the unmodified cache is
     * the items that were injected/extracted.
     */
    private final KeyCounter modifiableCache;
    /**
     * Primary keys whose fuzzy variants have been loaded into the local caches.
     */
    private final ReferenceOpenHashSet<Object> loadedFuzzyGroups;
    /**
     * List of items to emit.
     */
    private final KeyCounter emittedItems;
    /**
     * Virtual outputs produced by pseudo patterns during planning. They may satisfy later pseudo-pattern inputs, but do
     * not count as real simulated inventory or as network extraction requirements.
     */
    private final KeyCounter pseudoItems;
    /**
     * Inputs supplied by graph boundaries while previewing a local compatibility unit.
     */
    private final KeyCounter boundaryItems;
    private final Object2LongOpenHashMap<IPatternDetails> crafts = new Object2LongOpenHashMap<>();
    /**
     * Minimum amount of each item that needs to be extracted from the network. This is the maximum of (unmodified -
     * modifiable).
     */
    private final KeyCounter requiredExtract;
    /**
     * Byte count.
     */
    private double bytes = 0;

    protected CraftingSimulationState() {
        this.unmodifiedCache = new KeyCounter();
        this.modifiableCache = new KeyCounter();
        this.loadedFuzzyGroups = new ReferenceOpenHashSet<>();
        this.emittedItems = new KeyCounter();
        this.pseudoItems = new KeyCounter();
        this.boundaryItems = new KeyCounter();
        this.requiredExtract = new KeyCounter();
        this.crafts.defaultReturnValue(0);
    }

    public static CraftingPlan buildCraftingPlan(CraftingSimulationState state,
                                                 CraftingCalculation calculation, long calculatedAmount) {
        return buildCraftingPlan(state, calculation, calculatedAmount, null);
    }

    public static CraftingPlan buildCraftingPlan(CraftingSimulationState state,
                                                 CraftingCalculation calculation, long calculatedAmount,
                                                 @Nullable CraftingGraphDisplaySnapshot.Builder graphDisplayBuilder) {
        return new CraftingPlan(
            new GenericStack(calculation.getOutput(), calculatedAmount),
            (long) Math.ceil(state.bytes),
            calculation.isSimulation(),
            calculation.hasMultiplePaths(),
            state.requiredExtract,
            state.emittedItems,
            calculation.getMissingItems(),
            calculation.getIntermediateFinalOutputAmount(),
            state.crafts,
            calculation.getTree(),
            calculation.getTemporaryProviders(),
            graphDisplayBuilder,
            calculation.getAttemptMetrics(),
            calculation);
    }

    protected abstract long simulateExtractParent(AEKey what);

    protected abstract Iterable<AEKey> findFuzzyParent(AEKey input);

    private void cacheFuzzy(AEKey what) {
        if (!loadedFuzzyGroups.add(what.getPrimaryKey())) {
            return;
        }

        for (var keyToCache : findFuzzyParent(what)) {
            var extracted = simulateExtractParent(keyToCache);
            modifiableCache.add(keyToCache, extracted);
            unmodifiedCache.add(keyToCache, extracted);
        }
    }

    @Override
    public void insert(AEKey what, long amount, Actionable mode) {
        cacheFuzzy(what);

        if (mode == Actionable.MODULATE) {
            modifiableCache.add(what, amount);
        }
    }

    private void updateRequiredExtract(AEKey key, long delta) {
        if (delta > 0) {
            long max = Math.max(delta, this.requiredExtract.get(key));
            this.requiredExtract.set(key, max);
        }
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode) {
        long boundaryExtracted = Math.min(this.boundaryItems.get(what), amount);
        if (mode == Actionable.MODULATE && boundaryExtracted > 0) {
            this.boundaryItems.remove(what, boundaryExtracted);
        }
        if (boundaryExtracted == amount) {
            return boundaryExtracted;
        }

        cacheFuzzy(what);

        var cachedAmount = modifiableCache.get(what);
        if (cachedAmount == 0)
            return boundaryExtracted;

        long extracted = Math.min(cachedAmount, amount - boundaryExtracted);
        if (mode == Actionable.MODULATE) {
            modifiableCache.remove(what, extracted);
        }

        updateRequiredExtract(what, unmodifiedCache.get(what) - modifiableCache.get(what));

        return boundaryExtracted + extracted;
    }

    @Nullable
    @Override
    public Iterable<AEKey> findFuzzyTemplates(AEKey input) {
        if (input == null)
            return Collections.emptyList();
        cacheFuzzy(input);

        return Iterables.transform(
            Iterables.filter(modifiableCache.findFuzzy(input, FuzzyMode.IGNORE_ALL), entry -> entry.getLongValue() > 0),
            Object2LongMap.Entry::getKey);
    }

    @Override
    public void emitItems(AEKey what, long amount) {
        this.emittedItems.add(what, amount);
    }

    public void insertPseudo(AEKey what, long amount, Actionable mode) {
        if (mode == Actionable.MODULATE) {
            this.pseudoItems.add(what, amount);
        }
    }

    public void insertBoundary(AEKey what, long amount) {
        if (amount > 0) {
            this.boundaryItems.add(what, amount);
        }
    }

    /**
     * Extracts virtual pseudo outputs. This intentionally does not update requiredExtract because pseudo outputs are
     * never pulled from network storage.
     */
    public long extractPseudo(AEKey what, long amount, Actionable mode) {
        var available = this.pseudoItems.get(what);
        if (available == 0) {
            return 0;
        }

        long extracted = Math.min(available, amount);
        if (mode == Actionable.MODULATE) {
            this.pseudoItems.remove(what, extracted);
        }
        return extracted;
    }

    public double getBytes() {
        return bytes;
    }

    @Override
    public void addBytes(double bytes) {
        this.bytes += bytes;
    }

    @Override
    public void addCrafting(IPatternDetails details, long crafts) {
        this.crafts.addTo(details, crafts);
    }

    public long getOriginalAmount(AEKey what) {
        cacheFuzzy(what);
        return this.unmodifiedCache.get(what);
    }

    public long getRequiredExtractAmount(AEKey what) {
        return this.requiredExtract.get(what);
    }

    public long getAvailableAmount(AEKey what) {
        cacheFuzzy(what);
        return LongMath.saturatedAdd(this.modifiableCache.get(what), this.boundaryItems.get(what));
    }

    public long getAvailableNonProducedAmount(AEKey what) {
        cacheFuzzy(what);
        return Math.max(0, this.unmodifiedCache.get(what) - this.requiredExtract.get(what));
    }

    public long returnExtractedForReserve(AEKey what, long amount) {
        cacheFuzzy(what);
        long returned = Math.min(amount, this.requiredExtract.get(what));
        if (returned <= 0) {
            return 0;
        }

        this.requiredExtract.remove(what, returned);
        this.modifiableCache.add(what, returned);
        return returned;
    }

    public long getCraftedAmount(AEKey what) {
        long amount = 0;
        for (Object2LongMap.Entry<IPatternDetails> entry : this.crafts.object2LongEntrySet()) {
            var outputs = entry.getKey().getOutputs();
            if (outputs instanceof RandomAccess) {
                for (int i = 0, size = outputs.size(); i < size; i++) {
                    var output = outputs.get(i);
                    if (what.matches(output)) {
                        amount += output.amount() * entry.getLongValue();
                    }
                }
            } else {
                for (var output : outputs) {
                    if (what.matches(output)) {
                        amount += output.amount() * entry.getLongValue();
                    }
                }
            }
        }
        return amount;
    }

    public void ignore(AEKey stack) {
        cacheFuzzy(stack);
        unmodifiedCache.set(stack, 0);
        modifiableCache.set(stack, 0);
    }

    public void applyDiff(CraftingSimulationState parent) {
        freezeDiff().applyTo(parent);
    }

    public CraftingSimulationDiff freezeDiff() {
        var inventoryDelta = new KeyCounter();
        for (var entry : modifiableCache) {
            long delta = entry.getLongValue() - unmodifiedCache.get(entry.getKey());
            if (delta != 0) {
                inventoryDelta.add(entry.getKey(), delta);
            }
        }
        return new CraftingSimulationDiff(requiredExtract, inventoryDelta, emittedItems, pseudoItems, crafts, bytes);
    }

    StateSnapshot snapshot() {
        return new StateSnapshot(
            copy(unmodifiedCache),
            copy(modifiableCache),
            new ReferenceOpenHashSet<>(loadedFuzzyGroups),
            copy(emittedItems),
            copy(pseudoItems),
            copy(boundaryItems),
            copy(requiredExtract),
            new Object2LongOpenHashMap<>(crafts),
            bytes);
    }

    void restore(StateSnapshot snapshot) {
        restore(unmodifiedCache, snapshot.unmodifiedCache());
        restore(modifiableCache, snapshot.modifiableCache());
        loadedFuzzyGroups.clear();
        loadedFuzzyGroups.addAll(snapshot.loadedFuzzyGroups());
        restore(emittedItems, snapshot.emittedItems());
        restore(pseudoItems, snapshot.pseudoItems());
        restore(boundaryItems, snapshot.boundaryItems());
        restore(requiredExtract, snapshot.requiredExtract());
        crafts.clear();
        crafts.putAll(snapshot.crafts());
        bytes = snapshot.bytes();
    }

    private static KeyCounter copy(KeyCounter source) {
        var result = new KeyCounter();
        result.addAll(source);
        return result;
    }

    private static void restore(KeyCounter target, KeyCounter snapshot) {
        target.clear();
        target.removeEmptySubmaps();
        target.addAll(snapshot);
    }

    void applyDiff(CraftingSimulationDiff diff) {
        // It's important to apply this here to ensure that the extract below doesn't make us count some stacks twice.
        for (var entry : diff.requiredExtract) {
            var key = entry.getKey();
            // To compute the new parent max difference during the processing of the child's queries:
            // Take current parent difference, and add required extract (= max difference observed in the child).
            long delta = this.unmodifiedCache.get(key) - this.modifiableCache.get(key) + entry.getLongValue();
            this.updateRequiredExtract(key, delta);
        }

        for (var entry : diff.inventoryDelta) {
            long sizeDelta = entry.getLongValue();

            if (sizeDelta > 0) {
                this.insert(entry.getKey(), sizeDelta, Actionable.MODULATE);
            } else if (sizeDelta < 0) {
                long newStackSize = -sizeDelta;
                var reallyExtracted = this.extract(entry.getKey(), newStackSize, Actionable.MODULATE);

                if (reallyExtracted != -sizeDelta) {
                    throw new IllegalStateException("Failed to extract from parent. This is a bug!");
                }
            }
        }

        for (var toEmit : diff.emittedItems) {
            this.emitItems(toEmit.getKey(), toEmit.getLongValue());
        }

        for (var pseudoItem : diff.pseudoItems) {
            this.insertPseudo(pseudoItem.getKey(), pseudoItem.getLongValue(), Actionable.MODULATE);
        }

        this.addBytes(diff.bytes);

        for (Object2LongMap.Entry<IPatternDetails> entry : diff.crafts.object2LongEntrySet()) {
            this.addCrafting(entry.getKey(), entry.getLongValue());
        }
    }

    public Object2LongMap<IPatternDetails> getCrafts() {
        return crafts;
    }

    public KeyCounter getExtractedItems() {
        return requiredExtract;
    }

    // 新增 getter：供 MemoRecorder 录制副作用
    public KeyCounter getEmittedItems() {
        return emittedItems;
    }

    public KeyCounter getPseudoItems() {
        return pseudoItems;
    }

    public KeyCounter getModifiableCache() {
        return modifiableCache;
    }
}
