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

package ae2.crafting;

import ae2.api.crafting.IPatternDetails;
import ae2.api.networking.crafting.ICraftingPlan;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.crafting.graph.CraftingGraphDisplaySnapshot;
import ae2.integration.data.LiteCraftTreeNode;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CraftingPlan implements ICraftingPlan {
    private final GenericStack finalOutput;
    private final long bytes;
    private final boolean simulation;
    private final boolean multiplePaths;
    private final KeyCounter usedItems;
    private final KeyCounter emittedItems;
    private final KeyCounter missingItems;
    private final long intermediateFinalOutputAmount;
    private final Object2LongMap<IPatternDetails> patternTimes;
    private final CraftingTreeNode tree;
    private final List<ICraftingProvider> temporaryProviders;
    private final CraftingGraphDisplaySnapshot graphDisplaySnapshot;
    private final CraftingPlanDisplay display;

    public CraftingPlan(GenericStack finalOutput, long bytes, boolean simulation, boolean multiplePaths,
                        KeyCounter usedItems, KeyCounter emittedItems, KeyCounter missingItems,
                        long intermediateFinalOutputAmount, Object2LongMap<IPatternDetails> patternTimes,
                        CraftingTreeNode tree, List<ICraftingProvider> temporaryProviders,
                        CraftingGraphDisplaySnapshot graphDisplaySnapshot) {
        this.finalOutput = finalOutput;
        this.bytes = bytes;
        this.simulation = simulation;
        this.multiplePaths = multiplePaths;
        this.usedItems = usedItems;
        this.emittedItems = emittedItems;
        this.missingItems = missingItems;
        this.intermediateFinalOutputAmount = intermediateFinalOutputAmount;
        this.patternTimes = patternTimes;
        this.tree = tree;
        this.temporaryProviders = temporaryProviders;
        this.graphDisplaySnapshot = graphDisplaySnapshot;
        this.display = new CraftingPlanDisplay(finalOutput, patternTimes, usedItems, missingItems,
            tree, graphDisplaySnapshot);
    }

    public CraftingPlan(GenericStack finalOutput, long bytes, boolean simulation, boolean multiplePaths,
                        KeyCounter usedItems, KeyCounter emittedItems, KeyCounter missingItems,
                        long intermediateFinalOutputAmount, Object2LongMap<IPatternDetails> patternTimes,
                        CraftingTreeNode tree, List<ICraftingProvider> temporaryProviders) {
        this(finalOutput, bytes, simulation, multiplePaths, usedItems, emittedItems, missingItems,
            intermediateFinalOutputAmount, patternTimes, tree, temporaryProviders, null);
    }

    public CraftingPlan(GenericStack finalOutput, long bytes, boolean simulation, boolean multiplePaths,
                        KeyCounter usedItems, KeyCounter emittedItems, KeyCounter missingItems,
                        long intermediateFinalOutputAmount, Object2LongMap<IPatternDetails> patternTimes,
                        CraftingTreeNode tree) {
        this(finalOutput, bytes, simulation, multiplePaths, usedItems, emittedItems, missingItems,
            intermediateFinalOutputAmount, patternTimes, tree, List.of(), null);
    }

    public GenericStack finalOutput() { return finalOutput; }
    public long bytes() { return bytes; }
    public boolean simulation() { return simulation; }
    public boolean multiplePaths() { return multiplePaths; }
    public KeyCounter usedItems() { return usedItems; }
    public KeyCounter emittedItems() { return emittedItems; }
    public KeyCounter missingItems() { return missingItems; }
    public long intermediateFinalOutputAmount() { return intermediateFinalOutputAmount; }
    public Object2LongMap<IPatternDetails> patternTimes() { return patternTimes; }
    public CraftingTreeNode tree() { return tree; }
    public List<ICraftingProvider> temporaryProviders() { return temporaryProviders; }
    public CraftingGraphDisplaySnapshot graphDisplaySnapshot() { return graphDisplaySnapshot; }
    public CompletableFuture<LiteCraftTreeNode> requestDisplayTree() { return display.requestTree(); }
}
