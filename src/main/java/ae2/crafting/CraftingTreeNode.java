/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

import ae2.api.config.Actionable;
import ae2.api.crafting.IPatternDetails;
import ae2.api.networking.crafting.ICraftingService;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.core.localization.PlayerMessages;
import ae2.crafting.execution.CraftingCpuHelper;
import ae2.crafting.execution.InputTemplate;
import ae2.crafting.inv.ChildCraftingSimulationState;
import ae2.crafting.inv.CraftingSimulationState;
import ae2.crafting.inv.ICraftingInventory;
import ae2.helpers.patternprovider.PseudoPatternDetails;
import com.google.common.math.LongMath;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A crafting tree node is what represents a single requested stack in the crafting process. It can either be the
 * top-level requested stack (slot is then -1, parent is null), or a stack used in a pattern (slot is then the position
 * of this stack in the pattern, parent is the parent node).
 */
public class CraftingTreeNode {

    /**
     * what input this node is for. Null for the top-level node.
     */
    @Nullable
    final IPatternDetails.IInput parentInput;
    private final CraftingCalculation job;
    // parent node.
    private final CraftingTreeProcess parent;
    private final World level;
    /**
     * "Template" of the item this node is making. For top-level node: the count is always 1. For child nodes: the count
     * is that of the template of the corresponding input.
     */
    private final AEKey what;
    private final long amount;
    private final boolean canEmit;
    /**
     * The patterns that can make this node. Null if they haven't been computed yet.
     */
    private ObjectArrayList<CraftingTreeProcess> nodes = null;
    private Boolean selfReturningRemainderInput;
    private boolean recursiveDisplayNodesInitialized;

    public CraftingTreeNode(ICraftingService cc, CraftingCalculation job, AEKey what, long amount,
                            CraftingTreeProcess par, int slot) {
        this.parent = par;
        this.parentInput = slot == -1 ? null : par.details.getInputs()[slot];
        this.level = job.getLevel();
        this.job = job;
        this.what = findCraftedStack(cc, what);
        this.amount = amount;

        this.canEmit = cc.canEmitFor(what);
    }

    private AEKey findCraftedStack(ICraftingService cc, AEKey wat) {
        if (cc.canEmitFor(wat)) {
            return wat; // if we can emit for something, use that.
        }

        var patterns = this.job.getCraftingFor(wat);

        if (patterns.isEmpty() && parentInput != null) {
            // No pattern for the exact encoded input. Try to find a pattern for a substitute ingredient. ;)
            long acceptableAmount = parentInput.possibleInputs()[0].amount();

            for (var possibleInput : parentInput.possibleInputs()) {
                if (possibleInput.amount() != acceptableAmount) {
                    // Skip if the amounts don't match (don't want to replace 1000 water by 1000 buckets for example).
                    continue;
                }

                var fuzzy = cc.getFuzzyCraftable(possibleInput.what(), fuzzyCandidate -> this.parentInput.isValid(fuzzyCandidate, level));

                if (fuzzy != null) {
                    return fuzzy;
                }
            }
        }

        return wat;
    }

    private void buildChildPatterns() {
        // Sanity check: this should never be called if this is emitable
        if (this.canEmit) {
            throw new IllegalStateException("Internal AE2 error: this node is emitable, it shouldn't use patterns!");
        }

        if (this.nodes == null) {
            boolean trackPerformance = this.job.isPerformanceTrackingEnabled();
            long start = trackPerformance ? System.nanoTime() : 0;
            this.nodes = new ObjectArrayList<>();

            var gridNode = this.job.simRequester.getGridNode();
            if (gridNode != null) {
                var craftingService = gridNode.grid().getCraftingService();
                var patterns = this.job.getCraftingFor(this.what);
                for (int i = 0, size = patterns.size(); i < size; i++) {
                    var details = patterns.get(i);
                    if (this.parent == null || this.parent.notRecursive(details)) {
                        this.nodes.add(new CraftingTreeProcess(craftingService, job, details, this));
                    }
                }
            }
            if (trackPerformance) {
                this.job.recordPerformanceCount("patterns-for-" + this.what, this.nodes.size());
                this.job.recordPerformanceStage("build-child-patterns " + this.what, System.nanoTime() - start);
            }
        }
    }

    /**
     * Return true if adding this pattern as a child would not cause recursion.
     */
    @SuppressWarnings("unused")
    boolean notRecursive(IPatternDetails details) {
        return true;
    }

    private static long divideCeil(long dividend, long divisor) {
        long quotient = dividend / divisor;
        if (dividend % divisor == 0) {
            return quotient;
        }
        return quotient + 1;
    }

    /**
     * Request items. Will always succeed or throw an exception.
     *
     * @param inv             Current simulated inventory.
     * @param requestedAmount How many items. The raw amount for top-level requests, or the number of inputs for
     *                        requests that have a parent.
     * @param containerItems  A list where produced container items are written if it's not null.
     * @throws CraftBranchFailure If the request failed.
     */
    void request(CraftingSimulationState inv, long requestedAmount,
                 @Nullable KeyCounter containerItems)
        throws CraftBranchFailure, InterruptedException {
        this.job.handlePausing();

        if (this.job.isRequesting(this.what)) {
            long requestedItems = getTotalRequestedItems(requestedAmount);
            if (this.job.resolveRecursiveRequest(this.what, inv, requestedItems)) {
                this.job.addRecursiveDisplayRequest(this, requestedItems);
                if (this.what.equals(this.job.getOutput())) {
                    var currentRequest = this.job.getCurrentRequestKey();
                    if (currentRequest != null) {
                        this.job.addRecursiveFinalOutputInput(currentRequest);
                    }
                    this.job.addRecursiveIntermediateFinalOutput(requestedItems);
                }
                return;
            }
            if (this.job.cycleHasNetOutput(this.what) && this.job.canUseMissingItems()) {
                job.addMissing(this.what, requestedItems);
                return;
            }
            if (this.job.canUseMissingItems()) {
                throw new CraftBranchFailure(this.what, requestedItems,
                    PlayerMessages.CraftingNoNetOutput);
            }
            throw new CraftBranchFailure(this.what, requestedItems);
        }

        this.job.pushRequest(this.what);
        try {
            requestInner(inv, requestedAmount, containerItems);
        } finally {
            this.job.popRequest();
        }
    }

    private void requestInner(CraftingSimulationState inv, long requestedAmount,
                              @Nullable KeyCounter containerItems)
        throws CraftBranchFailure, InterruptedException {
        buildChildPatterns();
        var primaryPattern = !this.nodes.isEmpty() ? this.nodes.getFirst().details : null;

        if (primaryPattern != null && requestedAmount > 0) {
            var bundleKey = new CraftingCalculation.BundleKey(this.what, primaryPattern);
            var bundle = this.job.getBundle(bundleKey);
            if (bundle != null) {
                long craftTimes = divideCeil(getTotalRequestedItems(requestedAmount),
                    primaryPattern.getPrimaryOutput().amount());
                var scaled = bundle.scale(craftTimes);
                if (scaled.isSatisfiable(inv)) {
                    applyBundle(scaled, inv, containerItems);
                    return;
                }
            }
        }

        var memoKey = CraftingCalculation.MemoKey.create(this.what, this.job);
        var cached = this.job.getMemoResult(memoKey);
        if (cached != null && cached.isApplicable(inv)) {
            applyMemoResult(cached, inv, containerItems, requestedAmount);
            return;
        }

        MemoSnapshot snapshot = MemoSnapshot.capture(inv, this.job);
        MemoRecorder recorder = new MemoRecorder();

        inv.addStackBytes(what, amount, requestedAmount);

        /*
         * 1) COLLECT ITEMS FROM THE INVENTORY
         */
        if (!isTopLevelRequestedOutput()) {
            // Templates: must copy before using!
            var templates = getValidItemTemplates(inv);
            for (int i = 0, size = templates.size(); i < size; i++) {
                var template = templates.get(i);
                long extracted = CraftingCpuHelper.extractTemplates(inv, template, requestedAmount);

                if (extracted > 0) {
                    recorder.recordExtraction(template.key(), LongMath.saturatedMultiply(extracted, template.amount()));
                    requestedAmount -= extracted;
                    addContainerItems(template.key(), extracted, containerItems);
                    if (this.parentInput != null) {
                        var containerItem = this.parentInput.getRemainingKey(template.key());
                        if (containerItem != null) {
                            recorder.recordContainerItem(containerItem, extracted);
                        }
                    }
                    this.job.addIntermediateFinalOutputInput(template.key(),
                        LongMath.saturatedMultiply(extracted, template.amount()));

                    if (requestedAmount == 0) {
                        var result = recorder.build(snapshot, inv, this.job, requestedAmount);
                        this.job.putMemoResult(memoKey, result);
                        return;
                    }
                }
            }

            if (requestedAmount > 0 && canUsePseudoInputs()) {
                templates = getValidItemTemplates(inv);
                for (int i = 0, size = templates.size(); i < size; i++) {
                    var template = templates.get(i);
                    long extracted = extractPseudoTemplates(inv, template, requestedAmount);
                    if (extracted <= 0) {
                        continue;
                    }

                    recorder.recordExtraction(template.key(), LongMath.saturatedMultiply(extracted, template.amount()));
                    requestedAmount -= extracted;
                    addContainerItems(template.key(), extracted, containerItems);
                    if (this.parentInput != null) {
                        var containerItem = this.parentInput.getRemainingKey(template.key());
                        if (containerItem != null) {
                            recorder.recordContainerItem(containerItem, extracted);
                        }
                    }

                    if (requestedAmount == 0) {
                        var result = recorder.build(snapshot, inv, this.job, requestedAmount);
                        this.job.putMemoResult(memoKey, result);
                        return;
                    }
                }
            }
        }

        addContainerItems(what, requestedAmount, containerItems);
        if (this.parentInput != null) {
            var containerItem = this.parentInput.getRemainingKey(what);
            if (containerItem != null) {
                recorder.recordContainerItem(containerItem, requestedAmount);
            }
        }

        /*
         * 2) EMITABLE ITEMS
         */
        if (this.canEmit) {
            inv.emitItems(this.what, getTotalRequestedItems(requestedAmount));
            var result = recorder.build(snapshot, inv, this.job, requestedAmount);
            this.job.putMemoResult(memoKey, result);
            return;
        }

        /*
         * 3) USE PATTERNS
         */
        buildChildPatterns();
        long totalRequestedItems = getTotalRequestedItems(requestedAmount);
        if (!this.nodes.isEmpty()) {
            for (int i = 0, size = this.nodes.size(); i < size; i++) {
                var pro = this.nodes.get(i);
                totalRequestedItems = requestCraftingBranch(inv, pro, totalRequestedItems);
                if (totalRequestedItems <= 0) {
                    break;
                }
            }
            if (totalRequestedItems <= 0) {
                var result = recorder.build(snapshot, inv, this.job, requestedAmount);
                this.job.putMemoResult(memoKey, result);
                return;
            }
        }

        if (totalRequestedItems > 0 && this.job.canUseMissingItems() && !this.nodes.isEmpty()) {
            requestMissingBranches(inv, totalRequestedItems);
            var result = recorder.build(snapshot, inv, this.job, requestedAmount);
            this.job.putMemoResult(memoKey, result);
            return;
        }

        if (totalRequestedItems > 0) {
            if (this.job.canUseMissingItems()) {
                job.addMissing(this.what, totalRequestedItems);
            } else {
                throw new CraftBranchFailure(this.what, totalRequestedItems);
            }
        }

        var result = recorder.build(snapshot, inv, this.job, requestedAmount);
        this.job.putMemoResult(memoKey, result);

        if (primaryPattern != null && requestedAmount > 0) {
            long craftTimes = divideCeil(getTotalRequestedItems(requestedAmount),
                primaryPattern.getPrimaryOutput().amount());
            var bundle = result.toBundle(craftTimes);
            var bundleKey = new CraftingCalculation.BundleKey(this.what, primaryPattern);
            this.job.putBundle(bundleKey, bundle);
        }
    }

    private void requestMissingBranches(CraftingSimulationState inv, long totalRequestedItems)
        throws CraftBranchFailure, InterruptedException {
        boolean requestedAnyBranch = false;
        for (int i = 0, size = this.nodes.size(); i < size; i++) {
            var pro = this.nodes.get(i);
            if (pro.getInputCount(this.what) >= pro.getOutputCount(this.what)) {
                continue;
            }
            requestMissingBranch(inv, pro, totalRequestedItems);
            requestedAnyBranch = true;
            break;
        }

        if (!requestedAnyBranch) {
            job.addMissing(this.what, totalRequestedItems);
        }
    }

    private void requestMissingBranch(CraftingSimulationState inv, CraftingTreeProcess pro, long totalRequestedItems)
        throws CraftBranchFailure, InterruptedException {
        var craftedPerPattern = getEffectiveOutputCount(pro);
        var recursiveBatch = this.job.getRecursivePatternBatch(pro.details, this.what);

        while (totalRequestedItems > 0) {
            long times = getRequestedPatternTimes(pro, totalRequestedItems, craftedPerPattern, recursiveBatch);
            if (this.job.isPerformanceTrackingEnabled()) {
                this.job.runTimedCrafting("request-missing-branch " + this.what, () -> pro.request(inv, times));
            } else {
                pro.request(inv, times);
            }
            pro.addTreeRequestTimes(times);

            // by now we have succeeded, as request throws an exception in case of failure
            // check how much was actually produced
            var available = extractCraftedBranchOutput(inv, totalRequestedItems);
            if (available != 0) {
                totalRequestedItems -= available;

                if (totalRequestedItems <= 0) {
                    return;
                }
            } else {
                return;
            }
        }
    }

    long extractAvailableForCrafting(CraftingSimulationState inv, long maxAmount)
        throws InterruptedException {
        this.job.handlePausing();

        if (this.job.isCheckingAvailability(this.what)) {
            return 0;
        }

        this.job.pushAvailabilityCheck(this.what);
        try {
            return extractAvailableForCraftingInner(inv, maxAmount);
        } finally {
            this.job.popAvailabilityCheck();
        }
    }

    private long requestCraftingBranch(CraftingSimulationState inv, CraftingTreeProcess pro, long totalRequestedItems)
        throws InterruptedException {
        if (!pro.possible || totalRequestedItems <= 0) {
            return totalRequestedItems;
        }

        var craftedPerPattern = getEffectiveOutputCount(pro);
        while (totalRequestedItems > 0) {
            var recursiveBatch = this.job.getRecursivePatternBatch(pro.details, this.what);
            long requestedTimes = getRequestedPatternTimes(pro, totalRequestedItems, craftedPerPattern, recursiveBatch);
            long times;
            if (this.job.isPerformanceTrackingEnabled()) {
                times = this.job.timed("max-craftable " + this.what,
                    () -> pro.getMaximumCraftableTimes(inv, requestedTimes));
            } else {
                times = pro.getMaximumCraftableTimes(inv, requestedTimes);
            }
            if (times <= 0) {
                pro.possible = false;
                return totalRequestedItems;
            }

            long available;
            long missingSeedAmount = pro.getReusablePreviewRecursiveMissingSeedAmount(this.what);
            if (missingSeedAmount > 0) {
                long usedMissingSeed = Math.min(totalRequestedItems, missingSeedAmount);
                pro.applyReusablePreviewRecursiveMissingSeedsOnly();
                totalRequestedItems -= usedMissingSeed;
                if (totalRequestedItems <= 0) {
                    return 0;
                }
                continue;
            } else if (pro.applyReusablePreview(inv, times)) {
                pro.addTreeRequestTimes(times);
                available = extractCraftedBranchOutput(inv, totalRequestedItems);
            } else {
                final ChildCraftingSimulationState child = new ChildCraftingSimulationState(inv);
                long intermediateFinalOutputMarker = this.job.getIntermediateFinalOutputMarker();
                try {
                    this.job.pushMissingSuppression();
                    if (this.job.isPerformanceTrackingEnabled()) {
                        this.job.runTimedCrafting("request-branch " + this.what, () -> pro.request(child, times));
                    } else {
                        pro.request(child, times);
                    }
                    pro.addTreeRequestTimes(times);
                } catch (CraftBranchFailure failure) {
                    this.job.restoreIntermediateFinalOutputMarker(intermediateFinalOutputMarker);
                    pro.possible = false;
                    return totalRequestedItems;
                } finally {
                    this.job.popMissingSuppression();
                }

                available = extractCraftedBranchOutput(child, totalRequestedItems);
                if (craftedPerPattern > 0) {
                    available = Math.min(available, LongMath.saturatedMultiply(craftedPerPattern, times));
                }
                if (available > 0) {
                    child.applyDiff(inv);
                } else {
                    this.job.restoreIntermediateFinalOutputMarker(intermediateFinalOutputMarker);
                }
            }

            if (available <= 0) {
                pro.possible = false;
                return totalRequestedItems;
            }

            totalRequestedItems -= available;

            if (!pro.limitsQuantity()) {
                return totalRequestedItems;
            }
        }
        return totalRequestedItems;
    }

    // Only item stacks are supported.
    private void addContainerItems(AEKey template, long multiplier,
                                   @Nullable KeyCounter outputList) {
        if (outputList != null && this.parentInput != null) {
            var containerItem = parentInput.getRemainingKey(template);
            if (containerItem != null) {
                outputList.add(containerItem, multiplier);
            }
        }
    }

    private long extractAvailableForCraftingInner(CraftingSimulationState inv, long maxAmount)
        throws InterruptedException {
        long available = 0;

        if (!isTopLevelRequestedOutput()) {
            var intermediateFinalOutputMarker = this.job.getIntermediateFinalOutputMarker();
            var templates = getValidItemTemplates(inv);
            for (int i = 0, size = templates.size(); i < size; i++) {
                var template = templates.get(i);
                long extracted = CraftingCpuHelper.extractTemplates(inv, template, maxAmount - available);
                available = LongMath.saturatedAdd(available, extracted);
                this.job.addIntermediateFinalOutputInput(template.key(),
                    LongMath.saturatedMultiply(extracted, template.amount()));
                if (available >= maxAmount) {
                    return maxAmount;
                }
            }
            if (available < maxAmount && canUsePseudoInputs()) {
                templates = getValidItemTemplates(inv);
                for (int i = 0, size = templates.size(); i < size; i++) {
                    var template = templates.get(i);
                    long extracted = extractPseudoTemplates(inv, template, maxAmount - available);
                    available = LongMath.saturatedAdd(available, extracted);
                    if (available >= maxAmount) {
                        return maxAmount;
                    }
                }
            }
            if (available == 0) {
                this.job.restoreIntermediateFinalOutputMarker(intermediateFinalOutputMarker);
            }
        }

        if (this.job.isRequesting(this.what)) {
            if (this.job.canResolveRecursiveRequest(this.what, inv, getTotalRequestedItems(available))) {
                long remainingAmount = maxAmount - available;
                if (remainingAmount > 0) {
                    this.job.addRecursiveDisplayRequest(this, getTotalRequestedItems(maxAmount));
                    inv.insert(this.what, getTotalRequestedItems(remainingAmount), Actionable.MODULATE);
                    if (this.what.equals(this.job.getOutput())) {
                        var currentRequest = this.job.getCurrentRequestKey();
                        if (currentRequest != null) {
                            this.job.addRecursiveFinalOutputInput(currentRequest);
                        }
                        this.job.addRecursiveIntermediateFinalOutput(getTotalRequestedItems(remainingAmount));
                    }
                }
                return maxAmount;
            }
            return available;
        }

        if (this.canEmit) {
            return maxAmount;
        }

        buildChildPatterns();
        long totalRequestedItems = getTotalRequestedItems(maxAmount - available);
        for (int i = 0, size = this.nodes.size(); i < size; i++) {
            var pro = this.nodes.get(i);
            if (!pro.possible || totalRequestedItems <= 0) {
                continue;
            }
            long craftedPerPattern = getEffectiveOutputCount(pro);
            var recursiveBatch = this.job.getRecursivePatternBatch(pro.details, this.what);
            long requestedTimes = getRequestedPatternTimes(pro, totalRequestedItems, craftedPerPattern, recursiveBatch);
            long times;
            if (this.job.isPerformanceTrackingEnabled()) {
                times = this.job.timed("max-craftable-for-input " + this.what,
                    () -> pro.getMaximumCraftableTimes(inv, requestedTimes));
            } else {
                times = pro.getMaximumCraftableTimes(inv, requestedTimes);
            }
            if (times <= 0) {
                continue;
            }
            long missingSeedAmount = pro.getReusablePreviewRecursiveMissingSeedAmount(this.what);
            if (missingSeedAmount > 0) {
                long usedMissingSeed = Math.min(totalRequestedItems, missingSeedAmount);
                pro.applyReusablePreviewRecursiveMissingSeedsOnly();
                totalRequestedItems -= usedMissingSeed;
                available = LongMath.saturatedAdd(available, usedMissingSeed / this.amount);
                if (totalRequestedItems <= 0) {
                    break;
                }
                continue;
            }
            if (pro.applyReusablePreview(inv, times)) {
                pro.addTreeRequestTimes(times);
            } else {
                try {
                    this.job.pushMissingSuppression();
                    if (this.job.isPerformanceTrackingEnabled()) {
                        this.job.runTimedCrafting("request-input-branch " + this.what,
                            () -> pro.request(inv, times));
                    } else {
                        pro.request(inv, times);
                    }
                    pro.addTreeRequestTimes(times);
                } catch (CraftBranchFailure ignored) {
                    continue;
                } finally {
                    this.job.popMissingSuppression();
                }
            }
            long produced = extractCraftedBranchOutput(inv, totalRequestedItems);
            if (produced > 0) {
                totalRequestedItems -= produced;
                available = LongMath.saturatedAdd(available, produced / this.amount);
            }
        }

        return Math.min(maxAmount, available);
    }

    private boolean canUsePseudoInputs() {
        return this.parent != null && PseudoPatternDetails.isPseudo(this.parent.details);
    }

    /**
     * Get all stack templates that can be used for this node.
     *
     * @param inv Crafting inventory, used for fuzzy matching.
     */
    private List<InputTemplate> getValidItemTemplates(ICraftingInventory inv) {
        if (this.parentInput == null)
            return List.of(new InputTemplate(what, 1));
        if (this.job.isPerformanceTrackingEnabled()) {
            long start = System.nanoTime();
            var templates = this.job.collectValidTemplates(
                CraftingCpuHelper.getValidItemTemplates(inv, this.parentInput, level));
            this.job.recordPerformanceStage("fuzzy-templates " + this.what, System.nanoTime() - start);
            return templates;
        }
        return this.job.collectValidTemplates(CraftingCpuHelper.getValidItemTemplates(inv, this.parentInput, level));
    }

    private long extractCraftedBranchOutput(CraftingSimulationState inv, long amount) {
        long extracted = inv.extract(this.what, amount, Actionable.MODULATE);
        if (extracted >= amount) {
            return extracted;
        }
        return LongMath.saturatedAdd(extracted, inv.extractPseudo(this.what, amount - extracted, Actionable.MODULATE));
    }

    private long getEffectiveOutputCount(CraftingTreeProcess pro) {
        if (usesFinalOutputAsIntermediateInput(pro)) {
            return pro.getOutputCount(this.what);
        }
        long recursiveNetOutput = this.job.getCycleNetOutput(this.what);
        if (recursiveNetOutput > 0) {
            return recursiveNetOutput;
        }
        var recursiveBatch = this.job.getRecursivePatternBatch(pro.details, this.what);
        if (recursiveBatch.netOutput() > 0) {
            return recursiveBatch.netOutput();
        }
        long expandedPatternNetOutput = this.job.getExpandedPatternNetOutput(pro.details, this.what);
        if (expandedPatternNetOutput > 0) {
            return expandedPatternNetOutput;
        }
        return pro.getEffectiveOutputCount(this.what);
    }

    private boolean usesFinalOutputAsIntermediateInput(CraftingTreeProcess pro) {
        return !this.what.equals(this.job.getOutput()) && pro.getInputCount(this.job.getOutput()) > 0;
    }

    private long extractPseudoTemplates(CraftingSimulationState inv, InputTemplate template, long multiplier) {
        long maxTotal = LongMath.saturatedMultiply(template.amount(), multiplier);
        long extracted = inv.extractPseudo(template.key(), maxTotal, Actionable.SIMULATE);
        if (extracted == 0) {
            return 0;
        }

        multiplier = extracted / template.amount();
        maxTotal = LongMath.saturatedMultiply(template.amount(), multiplier);
        if (maxTotal == 0) {
            return 0;
        }

        extracted = inv.extractPseudo(template.key(), maxTotal, Actionable.MODULATE);
        if (extracted == 0 || extracted != maxTotal) {
            throw new IllegalStateException("Failed to correctly extract pseudo templates. Invalid simulation!");
        }
        return multiplier;
    }

    private long getRequestedPatternTimes(CraftingTreeProcess pro, long totalRequestedItems, long craftedPerPattern,
                                          CraftingCalculation.RecursivePatternBatch recursiveBatch) {
        if (pro.limitsQuantity()) {
            return 1;
        }
        if (usesFinalOutputAsIntermediateInput(pro)) {
            return divideCeil(totalRequestedItems, craftedPerPattern);
        }
        long netOutput = recursiveBatch.netOutput() > 0 ? recursiveBatch.netOutput() : craftedPerPattern;
        long rootTimes = Math.max(1, recursiveBatch.rootTimes());
        return LongMath.saturatedMultiply(divideCeil(totalRequestedItems, netOutput), rootTimes);
    }

    int getDepth() {
        int depth = 1;
        if (this.nodes != null) {
            for (int i = 0, size = this.nodes.size(); i < size; i++) {
                var pro = this.nodes.get(i);
                depth = Math.max(depth, 1 + pro.getDepth());
            }
        }
        return depth;
    }

    long getNodeCount() {
        long tot = 1;
        if (this.nodes != null) {
            for (int i = 0, size = this.nodes.size(); i < size; i++) {
                var pro = this.nodes.get(i);
                tot = LongMath.saturatedAdd(tot, pro.getNodeCount());
            }
        }
        return tot;
    }

    boolean hasMultiplePaths() {
        if (this.nodes == null) {
            return false;
        }
        if (this.nodes.size() > 1) {
            return true;
        }
        for (int i = 0, size = this.nodes.size(); i < size; i++) {
            var pro = this.nodes.get(i);
            if (pro.hasMultiplePaths()) {
                return true;
            }
        }
        return false;
    }

    void resetPossible() {
        this.recursiveDisplayNodesInitialized = false;
        if (this.nodes != null) {
            for (int i = 0, size = this.nodes.size(); i < size; i++) {
                var pro = this.nodes.get(i);
                pro.resetPossible();
            }
        }
    }

    public List<CraftingTreeProcess> getNodes() {
        return this.nodes;
    }

    long getPatternNodeCount() {
        long total = this.nodes == null ? 0 : this.nodes.size();
        if (this.nodes != null) {
            for (int i = 0, size = this.nodes.size(); i < size; i++) {
                var pro = this.nodes.get(i);
                total = LongMath.saturatedAdd(total, pro.getPatternNodeCount());
            }
        }
        return total;
    }

    CraftingTreeNode findDisplayNodeFor(AEKey key) {
        return findDisplayNodeFor(key, true);
    }

    private CraftingTreeNode findDisplayNodeFor(AEKey key, boolean skipSelf) {
        if (!skipSelf && this.what.equals(key)) {
            return this;
        }
        if (this.nodes == null) {
            return null;
        }
        for (int i = 0, size = this.nodes.size(); i < size; i++) {
            var process = this.nodes.get(i);
            for (CraftingTreeNode node : process.getNodes().keySet()) {
                var found = node.findDisplayNodeFor(key, false);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public long getRecursiveDisplayAmount() {
        return this.job.getRecursiveDisplayRequest(this);
    }

    public AEKey getWhat() {
        return this.what;
    }

    public long getAmount() {
        return this.amount;
    }

    public long getMissing() {
        return job.getMissingItems().get(what);
    }

    public boolean hasSelfReturningRemainderInput() {
        if (this.parentInput == null) {
            return false;
        }

        if (selfReturningRemainderInput != null) {
            return selfReturningRemainderInput;
        }

        for (var possibleInput : this.parentInput.possibleInputs()) {
            if (possibleInput.what().equals(this.parentInput.getRemainingKey(possibleInput.what()))) {
                return selfReturningRemainderInput = true;
            }
        }
        return selfReturningRemainderInput = this.what.equals(this.parentInput.getRemainingKey(this.what));
    }

    long getTemplateAmount() {
        return this.amount;
    }

    public List<CraftingTreeProcess> getDisplayNodes() {
        long recursiveDisplayAmount = getRecursiveDisplayAmount();
        if (this.nodes == null && recursiveDisplayAmount > 0) {
            buildChildPatterns();
        }
        if (this.nodes != null && recursiveDisplayAmount > 0 && !this.recursiveDisplayNodesInitialized) {
            this.recursiveDisplayNodesInitialized = true;
            for (int i = 0, size = this.nodes.size(); i < size; i++) {
                var process = this.nodes.get(i);
                long outputCount = process.getOutputCount(this.what);
                if (outputCount <= 0) {
                    continue;
                }
                process.addTreeRequestTimes(divideCeil(recursiveDisplayAmount, outputCount));
                break;
            }
        }
        return this.nodes;
    }

    private long getTotalRequestedItems(long requestedAmount) {
        return LongMath.saturatedMultiply(requestedAmount, this.amount);
    }

    private boolean isTopLevelRequestedOutput() {
        return this.parent == null && this.parentInput == null && this.what.equals(this.job.getOutput());
    }

    private void applyMemoResult(CraftingCalculation.MemoResult cached, CraftingSimulationState inv,
                                 @Nullable KeyCounter containerItems, long requestedAmount) throws CraftBranchFailure {
        long multiplier = 1;
        if (cached.baseTimes > 0) {
            multiplier = divideCeil(requestedAmount, cached.baseTimes);
        }

        for (var entry : cached.insertedItems) {
            inv.insert(entry.getKey(), entry.getLongValue() * multiplier, Actionable.MODULATE);
        }

        for (var entry : cached.emittedItems) {
            inv.emitItems(entry.getKey(), entry.getLongValue() * multiplier);
        }

        for (var entry : cached.pseudoItems) {
            inv.insertPseudo(entry.getKey(), entry.getLongValue() * multiplier, Actionable.MODULATE);
        }

        for (var entry : cached.missingItems) {
            this.job.addMissing(entry.getKey(), entry.getLongValue() * multiplier);
        }

        if (cached.intermediateFinalOutputAmount > 0) {
            this.job.addIntermediateFinalOutput(cached.intermediateFinalOutputAmount * multiplier);
        }

        var scaledRecursiveMissingSeeds = new KeyCounter();
        for (var entry : cached.recursiveMissingSeeds) {
            scaledRecursiveMissingSeeds.add(entry.getKey(), entry.getLongValue() * multiplier);
        }
        var scaledClearedSeeds = new KeyCounter();
        for (var entry : cached.clearedRecursiveMissingSeeds) {
            scaledClearedSeeds.add(entry.getKey(), entry.getLongValue() * multiplier);
        }
        this.job.applyRecursiveMissingSeedPreview(scaledClearedSeeds, scaledRecursiveMissingSeeds);

        this.job.addRealSeededRecursiveRequests(cached.realSeededRecursiveRequests);
        this.job.addRealRecursiveSeeds(cached.realRecursiveSeeds);
        this.job.addRealSeededRecursiveKeys(cached.realSeededRecursiveKeys);

        var scaledDisplayRequests = new Reference2LongOpenHashMap<CraftingTreeNode>();
        for (var entry : cached.recursiveDisplayRequestsDelta.reference2LongEntrySet()) {
            scaledDisplayRequests.put(entry.getKey(), entry.getLongValue() * multiplier);
        }
        this.job.addRecursiveDisplayRequests(scaledDisplayRequests);

        for (var entry : cached.extracted) {
            long amount = entry.getLongValue() * multiplier;
            long extracted = inv.extract(entry.getKey(), amount, Actionable.MODULATE);
            if (extracted < amount) {
                throw new CraftBranchFailure(entry.getKey(), amount - extracted);
            }
        }

        for (var entry : cached.patternTimes.reference2LongEntrySet()) {
            inv.addCrafting(entry.getKey(), entry.getLongValue() * multiplier);
        }

        inv.addBytes(cached.bytes * multiplier);

        if (containerItems != null) {
            for (var entry : cached.containerItems) {
                containerItems.add(entry.getKey(), entry.getLongValue() * multiplier);
            }
        }
    }

    private void applyBundle(CraftingCalculation.Bundle bundle, CraftingSimulationState inv,
                             @Nullable KeyCounter containerItems) throws CraftBranchFailure {
        for (var entry : bundle.insertedItems) {
            inv.insert(entry.getKey(), entry.getLongValue(), Actionable.MODULATE);
        }

        for (var entry : bundle.emittedItems) {
            inv.emitItems(entry.getKey(), entry.getLongValue());
        }

        for (var entry : bundle.pseudoItems) {
            inv.insertPseudo(entry.getKey(), entry.getLongValue(), Actionable.MODULATE);
        }

        for (var entry : bundle.missingItems) {
            this.job.addMissing(entry.getKey(), entry.getLongValue());
        }

        if (bundle.intermediateFinalOutputAmount > 0) {
            this.job.addIntermediateFinalOutput(bundle.intermediateFinalOutputAmount);
        }
        this.job.applyRecursiveMissingSeedPreview(
            bundle.clearedRecursiveMissingSeeds, bundle.recursiveMissingSeeds);
        this.job.addRealSeededRecursiveRequests(bundle.realSeededRecursiveRequests);
        this.job.addRealRecursiveSeeds(bundle.realRecursiveSeeds);
        this.job.addRealSeededRecursiveKeys(bundle.realSeededRecursiveKeys);
        this.job.addRecursiveDisplayRequests(bundle.recursiveDisplayRequestsDelta);

        for (var entry : bundle.extracted) {
            long amount = entry.getLongValue();
            long extracted = inv.extract(entry.getKey(), amount, Actionable.MODULATE);
            if (extracted < amount) {
                throw new CraftBranchFailure(entry.getKey(), amount - extracted);
            }
        }

        for (var entry : bundle.patternTimes.reference2LongEntrySet()) {
            inv.addCrafting(entry.getKey(), entry.getLongValue());
        }

        inv.addBytes(bundle.bytes);

        if (containerItems != null) {
            for (var entry : bundle.containerItems) {
                containerItems.add(entry.getKey(), entry.getLongValue());
            }
        }
    }

    private static final class MemoRecorder {
        private final KeyCounter extracted = new KeyCounter();
        private final KeyCounter containerItems = new KeyCounter();

        void recordExtraction(AEKey key, long amount) {
            extracted.add(key, amount);
        }

        void recordContainerItem(AEKey key, long amount) {
            containerItems.add(key, amount);
        }

        CraftingCalculation.MemoResult build(MemoSnapshot snapshot,
                                             CraftingSimulationState inv,
                                             CraftingCalculation job,
                                             long requestedAmount) {
            var result = new CraftingCalculation.MemoResult();
            result.baseTimes = requestedAmount;

            for (var entry : extracted) {
                result.recordExtraction(entry.getKey(), entry.getLongValue());
            }

            for (var entry : containerItems) {
                result.recordContainerItem(entry.getKey(), entry.getLongValue());
            }

            for (var entry : inv.getExtractedItems()) {
                long before = snapshot.snapshotExtracted.get(entry.getKey());
                long delta = entry.getLongValue() - before;
                if (delta > 0) {
                    result.recordExtraction(entry.getKey(), delta);
                }
            }

            for (var entry : inv.getCrafts().object2LongEntrySet()) {
                long before = snapshot.snapshotCrafts.getLong(entry.getKey());
                long delta = entry.getLongValue() - before;
                if (delta > 0) {
                    result.recordPattern(entry.getKey(), delta);
                }
            }

            double deltaBytes = inv.getBytes() - snapshot.snapshotBytes;
            if (deltaBytes > 0) {
                result.bytes = deltaBytes;
            }

            for (var entry : inv.getEmittedItems()) {
                long before = snapshot.snapshotEmitted.get(entry.getKey());
                long delta = entry.getLongValue() - before;
                if (delta > 0) {
                    result.recordEmitted(entry.getKey(), delta);
                }
            }

            for (var entry : inv.getPseudoItems()) {
                long before = snapshot.snapshotPseudo.get(entry.getKey());
                long delta = entry.getLongValue() - before;
                if (delta > 0) {
                    result.recordPseudo(entry.getKey(), delta);
                }
            }

            for (var entry : inv.getModifiableCache()) {
                long before = snapshot.snapshotModifiable.get(entry.getKey());
                long delta = entry.getLongValue() - before;
                if (delta > 0) {
                    result.recordInserted(entry.getKey(), delta);
                }
            }

            for (var entry : job.getMissingItems()) {
                long before = snapshot.snapshotMissing.get(entry.getKey());
                long delta = entry.getLongValue() - before;
                if (delta > 0) {
                    result.recordMissing(entry.getKey(), delta);
                }
            }

            long intermediateDelta = job.getIntermediateFinalOutputMarker() - snapshot.snapshotIntermediateFinalOutput;
            if (intermediateDelta > 0) {
                result.recordIntermediateFinalOutput(intermediateDelta);
            }

            var recursiveMissingSeedsDelta = new KeyCounter();
            for (var entry : job.getRecursiveMissingSeedsMarker()) {
                long before = snapshot.snapshotRecursiveMissingSeeds.get(entry.getKey());
                long delta = entry.getLongValue() - before;
                if (delta > 0) {
                    recursiveMissingSeedsDelta.add(entry.getKey(), delta);
                }
            }
            result.recordRecursiveMissingSeeds(recursiveMissingSeedsDelta);

            var clearedDelta = new KeyCounter();
            for (var entry : snapshot.snapshotRecursiveMissingSeeds) {
                long current = job.getRecursiveMissingSeedsMarker().get(entry.getKey());
                long cleared = entry.getLongValue() - current;
                if (cleared > 0) {
                    clearedDelta.add(entry.getKey(), cleared);
                }
            }
            result.recordClearedRecursiveMissingSeeds(clearedDelta);

            var newRealSeededRequests = new ObjectOpenHashSet<AEKey>();
            for (var key : job.getRealSeededRecursiveRequestsMarker()) {
                if (!snapshot.snapshotRealSeededRecursiveRequests.contains(key)) {
                    newRealSeededRequests.add(key);
                }
            }
            result.recordRealSeededRecursiveRequests(newRealSeededRequests);

            var newRealRecursiveSeeds = new ObjectOpenHashSet<AEKey>();
            for (var key : job.getRealRecursiveSeedsMarker()) {
                if (!snapshot.snapshotRealRecursiveSeeds.contains(key)) {
                    newRealRecursiveSeeds.add(key);
                }
            }
            result.recordRealRecursiveSeeds(newRealRecursiveSeeds);

            var newRealSeededKeys = new ObjectOpenHashSet<AEKey>();
            for (var key : job.getRealSeededRecursiveKeysMarker()) {
                if (!snapshot.snapshotRealSeededRecursiveKeys.contains(key)) {
                    newRealSeededKeys.add(key);
                }
            }
            result.recordRealSeededRecursiveKeys(newRealSeededKeys);

            var recursiveDisplayDelta = new Reference2LongOpenHashMap<CraftingTreeNode>();
            for (var entry : job.getRecursiveDisplayRequestsMarker().reference2LongEntrySet()) {
                long before = snapshot.snapshotRecursiveDisplayRequests.getLong(entry.getKey());
                long delta = entry.getLongValue() - before;
                if (delta > 0) {
                    recursiveDisplayDelta.put(entry.getKey(), delta);
                }
            }
            result.recordRecursiveDisplayRequests(recursiveDisplayDelta);

            return result;
        }
    }

    private static final class MemoSnapshot {
        final KeyCounter snapshotExtracted;
        final Reference2LongOpenHashMap<IPatternDetails> snapshotCrafts;
        final double snapshotBytes;
        final KeyCounter snapshotEmitted;
        final KeyCounter snapshotPseudo;
        final KeyCounter snapshotModifiable;
        final KeyCounter snapshotMissing;
        final long snapshotIntermediateFinalOutput;
        final KeyCounter snapshotRecursiveMissingSeeds;
        final ObjectOpenHashSet<AEKey> snapshotRealSeededRecursiveRequests;
        final ObjectOpenHashSet<AEKey> snapshotRealRecursiveSeeds;
        final ObjectOpenHashSet<AEKey> snapshotRealSeededRecursiveKeys;
        final Reference2LongOpenHashMap<CraftingTreeNode> snapshotRecursiveDisplayRequests;

        private MemoSnapshot(KeyCounter snapshotExtracted,
                            Reference2LongOpenHashMap<IPatternDetails> snapshotCrafts,
                            double snapshotBytes,
                            KeyCounter snapshotEmitted,
                            KeyCounter snapshotPseudo,
                            KeyCounter snapshotModifiable,
                            KeyCounter snapshotMissing,
                            long snapshotIntermediateFinalOutput,
                            KeyCounter snapshotRecursiveMissingSeeds,
                            ObjectOpenHashSet<AEKey> snapshotRealSeededRecursiveRequests,
                            ObjectOpenHashSet<AEKey> snapshotRealRecursiveSeeds,
                            ObjectOpenHashSet<AEKey> snapshotRealSeededRecursiveKeys,
                            Reference2LongOpenHashMap<CraftingTreeNode> snapshotRecursiveDisplayRequests) {
            this.snapshotExtracted = snapshotExtracted;
            this.snapshotCrafts = snapshotCrafts;
            this.snapshotBytes = snapshotBytes;
            this.snapshotEmitted = snapshotEmitted;
            this.snapshotPseudo = snapshotPseudo;
            this.snapshotModifiable = snapshotModifiable;
            this.snapshotMissing = snapshotMissing;
            this.snapshotIntermediateFinalOutput = snapshotIntermediateFinalOutput;
            this.snapshotRecursiveMissingSeeds = snapshotRecursiveMissingSeeds;
            this.snapshotRealSeededRecursiveRequests = snapshotRealSeededRecursiveRequests;
            this.snapshotRealRecursiveSeeds = snapshotRealRecursiveSeeds;
            this.snapshotRealSeededRecursiveKeys = snapshotRealSeededRecursiveKeys;
            this.snapshotRecursiveDisplayRequests = snapshotRecursiveDisplayRequests;
        }

        static MemoSnapshot capture(CraftingSimulationState inv, CraftingCalculation job) {
            var snapshotExtracted = new KeyCounter();
            snapshotExtracted.addAll(inv.getExtractedItems());
            var snapshotCrafts = new Reference2LongOpenHashMap<IPatternDetails>();
            snapshotCrafts.putAll(inv.getCrafts());
            double snapshotBytes = inv.getBytes();

            var snapshotEmitted = new KeyCounter();
            snapshotEmitted.addAll(inv.getEmittedItems());
            var snapshotPseudo = new KeyCounter();
            snapshotPseudo.addAll(inv.getPseudoItems());
            var snapshotModifiable = new KeyCounter();
            snapshotModifiable.addAll(inv.getModifiableCache());
            var snapshotMissing = new KeyCounter();
            snapshotMissing.addAll(job.getMissingItems());

            long snapshotIntermediateFinalOutput = job.getIntermediateFinalOutputMarker();
            var snapshotRecursiveMissingSeeds = new KeyCounter();
            snapshotRecursiveMissingSeeds.addAll(job.getRecursiveMissingSeedsMarker());
            var snapshotRealSeededRecursiveRequests = new ObjectOpenHashSet<>(job.getRealSeededRecursiveRequestsMarker());
            var snapshotRealRecursiveSeeds = new ObjectOpenHashSet<>(job.getRealRecursiveSeedsMarker());
            var snapshotRealSeededRecursiveKeys = new ObjectOpenHashSet<>(job.getRealSeededRecursiveKeysMarker());
            var snapshotRecursiveDisplayRequests = new Reference2LongOpenHashMap<CraftingTreeNode>();
            snapshotRecursiveDisplayRequests.putAll(job.getRecursiveDisplayRequestsMarker());

            return new MemoSnapshot(snapshotExtracted, snapshotCrafts, snapshotBytes,
                snapshotEmitted, snapshotPseudo, snapshotModifiable, snapshotMissing,
                snapshotIntermediateFinalOutput, snapshotRecursiveMissingSeeds,
                snapshotRealSeededRecursiveRequests, snapshotRealRecursiveSeeds,
                snapshotRealSeededRecursiveKeys, snapshotRecursiveDisplayRequests);
        }
    }
}
