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
import ae2.api.networking.IGrid;
import ae2.api.networking.crafting.CalculationStrategy;
import ae2.api.networking.crafting.ICraftingPlan;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.networking.crafting.ICraftingService;
import ae2.api.networking.crafting.ICraftingSimulationRequester;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.AEKey2LongMap;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.core.AEConfig;
import ae2.core.AELog;
import ae2.crafting.execution.CraftingSupplierLocation;
import ae2.crafting.execution.CraftingSupplierLocator;
import ae2.crafting.execution.InputTemplate;
import ae2.crafting.graph.DemandPropagation;
import ae2.crafting.graph.GraphBuilder;
import ae2.crafting.graph.GraphExecutor;
import ae2.crafting.inv.ChildCraftingSimulationState;
import ae2.crafting.inv.CraftingSimulationState;
import ae2.crafting.inv.NetworkCraftingSimulationState;
import ae2.debug.TileCraftingTreeTest;
import ae2.hooks.ticking.TickHandler;
import ae2.me.service.CraftingService;
import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.math.LongMath;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CraftingCalculation {

    final ICraftingSimulationRequester simRequester;
    private final ICraftingService craftingService;
    private final NetworkCraftingSimulationState networkInv;
    private final World level;
    private final KeyCounter missing = new KeyCounter();
    private final KeyCounter recursiveMissingSeeds = new KeyCounter();
    private final Object monitor = new Object();
    private final CraftingPerformanceListener performanceListener;
    private final Stopwatch watch = Stopwatch.createUnstarted();
    private final CraftingTreeNode tree;
    private final AEKey output;
    private final ObjectArrayList<AEKey> requestStack = new ObjectArrayList<>();
    private final ObjectArrayList<AEKey> availabilityStack = new ObjectArrayList<>();
    private final ObjectArrayList<CraftingTreeProcess> processStack = new ObjectArrayList<>();
    private final IntArrayList processHashPrefixes = new IntArrayList();
    private final IntArrayList processHashPowers = new IntArrayList();
    private final List<TimingFrame> timingStack = new ObjectArrayList<>();
    private final Map<AEKey, ObjectList<IPatternDetails>> patternCache = new Object2ObjectOpenHashMap<>();
    private @Nullable Map<AEKey, List<IPatternDetails>> additionalPatternsByOutput;
    private final Map<IPatternDetails, AEKey2LongMap> expandedPatternNetOutputCache = new Reference2ObjectOpenHashMap<>();
    private final Map<IPatternDetails, Map<AEKey, RecursivePatternBatch>> recursivePatternBatchCache =
        new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<IPatternDetails, CraftingTreeProcess.MachineInfo> machineInfoCache =
        new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<ICraftingProvider, CraftingSupplierLocation> machineLocationCache =
        new Reference2ObjectOpenHashMap<>();
    private @Nullable IGrid machineLocationCacheGrid;
    private final Map<RecursiveNetKey, RecursiveNet> recursiveNetCache = new Object2ObjectOpenHashMap<>();
    private final RecursiveNetKey recursiveNetLookup = RecursiveNetKey.mutableProbe();
    private final ObjectPool<RecursiveNetKey> recursiveNetKeyPool = new ObjectPool<>(
        RecursiveNetKey::new,
        100
    );
    private final Set<AEKey> realSeededRecursiveRequests = new ObjectOpenHashSet<>();
    private final Set<AEKey> realRecursiveSeeds = new ObjectOpenHashSet<>();
    private final Set<AEKey> realSeededRecursiveKeys = new ObjectOpenHashSet<>();
    private final Cache<Object, Object> memoCache;
    private final Set<AEKey> recursiveFinalOutputInputs = new ObjectOpenHashSet<>();
    private final KeyCounter recursiveReserveCandidates = new KeyCounter();
    private final Reference2LongOpenHashMap<CraftingTreeNode> recursiveDisplayRequests = new Reference2LongOpenHashMap<>();
    private final List<ICraftingProvider> temporaryProviders;
    private final long recursiveIngredientReserveAmount;
    // The initially requested amount of "output", may be reduced depending on the strategy used
    private final long requestedAmount;
    private final CalculationStrategy strategy;
    private final List<CraftAttempt> attempts = AELog.isCraftingLogEnabled() ? new ObjectArrayList<>() : null;
    private boolean simulate = false;
    private boolean allowMissing = false;
    private int missingSuppression = 0;
    private boolean running = false;
    private boolean done = false;
    private int time = 5;
    private int incTime = Integer.MAX_VALUE;
    private int maxRequestDepth = 0;
    private long intermediateFinalOutputAmount = 0;
    private int recursiveMissingSeedSuppression = 0;
    @Nullable
    private KeyCounter reserveProtectedMissingSeeds = null;
    private boolean applyingRecursiveIngredientReserve = false;
    private boolean recursiveReserveBlockedByMissingSeed = false;
    private boolean usedCraftingTreeTestPattern;

    public CraftingCalculation(World level, IGrid grid, ICraftingSimulationRequester simRequester,
                               GenericStack output, CalculationStrategy strategy,
                               Object sharedMemoCache) {
        this(level, grid, simRequester, output, strategy, createPerformanceListener(), sharedMemoCache);
    }

    private CraftingCalculation(World level, IGrid grid, ICraftingSimulationRequester simRequester,
                                GenericStack output, CalculationStrategy strategy,
                                CraftingPerformanceListener performanceListener,
                                @Nullable Object sharedMemoCache) {
        this.level = level;
        this.output = output.what();
        this.requestedAmount = output.amount();
        this.strategy = strategy;
        this.simRequester = simRequester;
        this.performanceListener = performanceListener;
        this.temporaryProviders = List.copyOf(simRequester.getAdditionalProviders());

        if (sharedMemoCache instanceof Cache) {
            @SuppressWarnings("unchecked")
            Cache<Object, Object> cache = (Cache<Object, Object>) sharedMemoCache;
            this.memoCache = cache;
        } else {
            this.memoCache = CacheBuilder.newBuilder().maximumSize(0).build();
        }

        var storage = grid.getStorageService();
        var craftingService = grid.getCraftingService();
        this.craftingService = craftingService;
        this.networkInv = new NetworkCraftingSimulationState(storage, simRequester.getActionSource());
        this.recursiveIngredientReserveAmount = Math.max(0, craftingService.getRecursiveIngredientReserveAmount());
        this.processHashPrefixes.add(0);
        this.processHashPowers.add(1);

        if (isPerformanceTrackingEnabled()) {
            long treeStart = System.nanoTime();
            this.tree = new CraftingTreeNode(craftingService, this, this.output, 1, null, -1);
            recordPerformanceStage("construct-tree", System.nanoTime() - treeStart);

            long preloadStart = System.nanoTime();
            preloadPatternCache(this.output);
            recordPerformanceStage("preload-pattern-cache", System.nanoTime() - preloadStart);
        } else {
            this.tree = new CraftingTreeNode(craftingService, this, this.output, 1, null, -1);
            preloadPatternCache(this.output);
        }
    }

    private void preloadPatternCache(AEKey what) {
        var patterns = getCraftingFor(what);
        for (int i = 0, size = patterns.size(); i < size; i++) {
            var pattern = patterns.get(i);
            var inputs = pattern.getInputs();
            for (var input : inputs) {
                var possibleInputs = input.possibleInputs();
                for (var possible : possibleInputs) {
                    getCraftingFor(possible.what());
                }
            }
        }
    }

    private static Map<AEKey, List<IPatternDetails>> indexAdditionalPatterns(List<IPatternDetails> additionalPatterns) {
        var patternsByOutput = new Object2ObjectOpenHashMap<AEKey, List<IPatternDetails>>();
        if (additionalPatterns instanceof RandomAccess) {
            for (int i = 0, size = additionalPatterns.size(); i < size; i++) {
                var pattern = additionalPatterns.get(i);
                var output = pattern.getPrimaryOutput();
                patternsByOutput.computeIfAbsent(output.what(), ignored -> new ObjectArrayList<>()).add(pattern);
            }
        } else {
            for (var pattern : additionalPatterns) {
                var output = pattern.getPrimaryOutput();
                patternsByOutput.computeIfAbsent(output.what(), ignored -> new ObjectArrayList<>()).add(pattern);
            }
        }
        return patternsByOutput;
    }

    private Map<AEKey, List<IPatternDetails>> getAdditionalPatternsByOutput() {
        var result = this.additionalPatternsByOutput;
        if (result == null) {
            result = indexAdditionalPatterns(this.simRequester.getAdditionalPatterns());
            this.additionalPatternsByOutput = result;
        }
        return result;
    }

    private static CraftingPerformanceListener createPerformanceListener() {
        try {
            if (AEConfig.instance().isCraftingPerformanceLogEnabled()) {
                return new LoggingCraftingPerformanceListener();
            }
        } catch (IllegalStateException ignored) {
            // Tests can construct calculations before the mod configuration exists.
        }
        return CraftingPerformanceListener.NOOP;
    }

    private static long getPatternOutputCount(IPatternDetails pattern, AEKey what) {
        long total = 0;
        var outputs = pattern.getOutputs();
        if (outputs instanceof RandomAccess) {
            for (int i = 0, size = outputs.size(); i < size; i++) {
                var output = outputs.get(i);
                if (what.matches(output)) {
                    total = LongMath.saturatedAdd(total, output.amount());
                }
            }
        } else {
            for (var output : outputs) {
                if (what.matches(output)) {
                    total = LongMath.saturatedAdd(total, output.amount());
                }
            }
        }
        return total;
    }

    private static long getPatternInputCount(IPatternDetails pattern, AEKey what) {
        long total = 0;
        for (var input : pattern.getInputs()) {
            var possibleInputs = input.possibleInputs();
            for (var possibleInput : possibleInputs) {
                if (what.matches(possibleInput)) {
                    total = LongMath.saturatedAdd(total,
                        LongMath.saturatedMultiply(possibleInput.amount(), input.getMultiplier()));
                    break;
                }
            }
        }
        return total;
    }

    private static void accumulatePatternNet(KeyCounter netByKey, IPatternDetails pattern) {
        accumulatePatternNet(netByKey, pattern, 1);
    }

    private static void accumulatePatternNet(KeyCounter netByKey, IPatternDetails pattern, long times) {
        accumulatePatternNet(netByKey, null, pattern, times);
    }

    private static void accumulatePatternNet(KeyCounter netByKey, @Nullable Collection<AEKey> inputKeys,
                                             IPatternDetails pattern, long times) {
        var outputs = pattern.getOutputs();
        if (outputs instanceof RandomAccess) {
            for (int i = 0, size = outputs.size(); i < size; i++) {
                var output = outputs.get(i);
                netByKey.add(output.what(), LongMath.saturatedMultiply(output.amount(), times));
            }
        } else {
            for (var output : outputs) {
                netByKey.add(output.what(), LongMath.saturatedMultiply(output.amount(), times));
            }
        }

        for (var input : pattern.getInputs()) {
            var possibleInputs = input.possibleInputs();
            if (possibleInputs.length == 0) {
                continue;
            }
            var primaryInput = possibleInputs[0];
            var inputKey = primaryInput.what();
            long inputAmount = LongMath.saturatedMultiply(primaryInput.amount(),
                LongMath.saturatedMultiply(input.getMultiplier(), times));
            netByKey.add(inputKey, LongMath.saturatedSubtract(0, inputAmount));
            if (inputKeys != null) {
                inputKeys.add(inputKey);
            }
        }
    }

    private static long divideCeil(long dividend, long divisor) {
        long quotient = dividend / divisor;
        if (dividend % divisor == 0) {
            return quotient;
        }
        return quotient + 1;
    }

    public void addMissing(AEKey what, long amount) {
        if (this.realSeededRecursiveKeys.contains(what) || this.realRecursiveSeeds.contains(what)
            || hasRealSeededRecursiveRequestFor(what)) {
            return;
        }
        missing.add(what, amount);
    }

    public ObjectList<IPatternDetails> getCraftingFor(AEKey what) {
        return this.patternCache.computeIfAbsent(what, key -> {
            var gridNode = this.simRequester.getGridNode();
            if (gridNode == null) {
                return ObjectLists.emptyList();
            }
            var patterns = new ObjectArrayList<IPatternDetails>();
            var networkPatterns = gridNode.grid().getCraftingService().getCraftingFor(key);
            if (networkPatterns instanceof List<?> networkPatternList && networkPatternList instanceof RandomAccess) {
                for (int i = 0, size = networkPatternList.size(); i < size; i++) {
                    var pattern = (IPatternDetails) networkPatternList.get(i);
                    patterns.add(pattern);
                    if (TileCraftingTreeTest.isCraftingTreeTestPattern(pattern)) {
                        this.usedCraftingTreeTestPattern = true;
                    }
                }
            } else {
                for (var pattern : networkPatterns) {
                    patterns.add(pattern);
                    if (TileCraftingTreeTest.isCraftingTreeTestPattern(pattern)) {
                        this.usedCraftingTreeTestPattern = true;
                    }
                }
            }
            var additionalPatterns = this.getAdditionalPatternsByOutput().get(key);
            if (additionalPatterns != null) {
                for (int i = 0; i < additionalPatterns.size(); i++) {
                    var pattern = additionalPatterns.get(i);
                    patterns.add(pattern);
                    if (TileCraftingTreeTest.isCraftingTreeTestPattern(pattern)) {
                        this.usedCraftingTreeTestPattern = true;
                    }
                }
            }
            return patterns;
        });
    }

    CraftingTreeProcess.MachineInfo getMachineInfo(ICraftingService craftingService, IPatternDetails pattern) {
        var grid = craftingService instanceof CraftingService service ? service.getGrid() : null;
        updateMachineLocationCacheGrid(grid);

        var machineInfo = this.machineInfoCache.get(pattern);
        if (machineInfo == null) {
            machineInfo = CraftingTreeProcess.collectMachineInfo(this, craftingService, pattern);
            this.machineInfoCache.put(pattern, machineInfo);
        }
        return machineInfo;
    }

    @Nullable
    CraftingSupplierLocation resolveMachineLocation(IGrid grid, ICraftingProvider provider) {
        updateMachineLocationCacheGrid(grid);

        var location = this.machineLocationCache.get(provider);
        if (location != null || this.machineLocationCache.containsKey(provider)) {
            return location;
        }

        location = CraftingSupplierLocator.resolveLocation(grid, provider);
        this.machineLocationCache.put(provider, location);
        return location;
    }

    private void updateMachineLocationCacheGrid(@Nullable IGrid grid) {
        if (this.machineLocationCacheGrid != grid) {
            invalidateMachineLocationCaches();
            this.machineLocationCacheGrid = grid;
        }
    }

    private void invalidateMachineLocationCaches() {
        this.machineInfoCache.clear();
        this.machineLocationCache.clear();
    }

    List<InputTemplate> collectValidTemplates(Iterable<InputTemplate> templates) {
        var collected = new ObjectArrayList<InputTemplate>();
        for (var template : templates) {
            collected.add(template);
        }
        return collected;
    }

    public ICraftingPlan run() {
        long calculationStart = System.nanoTime();
        try {
            startPerformanceListener();
            TickHandler.instance().registerCraftingSimulation(this.level, this);
            this.handlePausing();

            ICraftingPlan plan;
            if (isPerformanceTrackingEnabled()) {
                plan = timed("compute-plan", this::computePlan);
            } else {
                plan = computePlan();
            }
            this.logCraftingJob(plan);
            if (this.usedCraftingTreeTestPattern) {
                long elapsedNanos = System.nanoTime() - calculationStart;
                AELog.info(
                    "Crafting tree test completed output=%s requested=%d time=%d ms (%d us) "
                        + "treeDepth=%d treeNodeCount=%d patternNodeCount=%d maxRequestDepth=%d",
                    this.output,
                    this.requestedAmount,
                    TimeUnit.NANOSECONDS.toMillis(elapsedNanos),
                    TimeUnit.NANOSECONDS.toMicros(elapsedNanos),
                    this.getTreeDepth(),
                    this.getTreeNodeCount(),
                    this.getPatternNodeCount(),
                    this.getMaxRequestDepth());
            }
            return plan;
        } catch (Exception ex) {
            AELog.info(ex, "Exception during crafting calculation.");
            throw new RuntimeException(ex);
        } finally {
            this.finish();
        }
    }

    private void startPerformanceListener() {
        if (this.performanceListener.isEnabled()) {
            this.performanceListener.start(this.output, this.requestedAmount);
        }
    }

    private ICraftingPlan computePlan() throws InterruptedException {
        if (!isPerformanceTrackingEnabled()) {
            return computePlanUnmeasured();
        }

        long calculationStart = System.nanoTime();
        try {
            return computePlanUnmeasured();
        } finally {
            finishPerformanceListener(System.nanoTime() - calculationStart);
        }
    }

    private ICraftingPlan computePlanUnmeasured() throws InterruptedException {
        if (strategy == CalculationStrategy.CRAFT_LESS) {
            var craftLessPlan = runCraftLessAttempt(requestedAmount);
            if (craftLessPlan != null) {
                return craftLessPlan;
            }

            return runCraftAttempt(requestedAmount, requestedAmount);
        }

        try {
            return runGraphBasedAttempt(requestedAmount);
        } catch (Exception e) {
            AELog.warn("Graph-based calculation failed, fallback to legacy: " + e.getMessage());
            return runCraftAttempt(requestedAmount, requestedAmount);
        }
    }

    private CraftingPlan runCraftAttempt(long productionAmount, long finalAmount)
        throws InterruptedException {
        this.simulate = false;
        this.allowMissing = true;
        this.missing.clear();
        this.recursiveMissingSeeds.clear();
        for (var key : this.recursiveNetCache.keySet()) {
            this.recursiveNetKeyPool.release(key);
        }
        this.recursiveNetCache.clear();
        this.intermediateFinalOutputAmount = 0;
        this.recursiveMissingSeedSuppression = 0;
        this.realSeededRecursiveRequests.clear();
        this.realRecursiveSeeds.clear();
        this.realSeededRecursiveKeys.clear();
        this.recursiveFinalOutputInputs.clear();
        this.recursiveReserveCandidates.clear();
        this.recursiveDisplayRequests.clear();
        this.tree.resetPossible();

        final boolean logCrafting = AELog.isCraftingLogEnabled();
        final Stopwatch timer = logCrafting ? Stopwatch.createStarted() : null;
        final boolean trackPerformance = isPerformanceTrackingEnabled();
        final String attemptName = trackPerformance
            ? "attempt amount=%d final=%d".formatted(productionAmount, finalAmount)
            : null;
        final long attemptStart = trackPerformance ? System.nanoTime() : 0;

        ChildCraftingSimulationState craftingInventory = new ChildCraftingSimulationState(networkInv);

        // Do the crafting. Throws in case of failure.
        try {
            if (trackPerformance) {
                runTimedCrafting("tree-request " + attemptName,
                    () -> this.tree.request(craftingInventory, productionAmount, null));
            } else {
                this.tree.request(craftingInventory, productionAmount, null);
            }
        } catch (CraftBranchFailure failure) {
            if (failure.hasExplicitMessageKey()) {
                throw new CraftingCalculationFailure(failure.getLocalizedMessageKey());
            }
            if (logCrafting) {
                this.attempts.add(new CraftAttempt(productionAmount + " failed", timer));
            }
            if (trackPerformance) {
                recordPerformanceStage(attemptName + " failed", System.nanoTime() - attemptStart);
            }
            return null;
        }
        applyRecursiveIngredientReserve(craftingInventory);
        clearResolvedRecursiveMissingItems(craftingInventory);
        addRecursiveMissingSeedsToPlan();
        // Add bytes for the tree size.
        craftingInventory.addBytes((double) this.tree.getNodeCount() * 8);

        CraftingPlan plan;
        if (trackPerformance) {
            plan = timed("build-plan " + attemptName,
                () -> CraftingSimulationState.buildCraftingPlan(craftingInventory, this, finalAmount));
        } else {
            plan = CraftingSimulationState.buildCraftingPlan(craftingInventory, this, finalAmount);
        }
        if (logCrafting) {
            this.attempts.add(new CraftAttempt("%d succeeded (%d bytes)".formatted(productionAmount, plan.bytes()),
                timer));
        }
        if (trackPerformance) {
            recordPerformanceStage(attemptName + " completed", System.nanoTime() - attemptStart);
        }
        return plan;
    }

    private CraftingPlan runGraphBasedAttempt(long productionAmount) throws InterruptedException {
        var graphBuilder = new GraphBuilder(this);
        var graph = timed("buildGraph", () -> graphBuilder.buildGraph(this.output, productionAmount));

        recordPerformanceCount("graphNodes", graph.getNodeCount());
        recordPerformanceCount("graphEdges", graph.getEdgeCount());

        var propagation = new DemandPropagation();
        timed("propagateDemand", () -> {
            propagation.propagate(graph);
            return null;
        });

        var craftingInventory = new ChildCraftingSimulationState(this.networkInv);
        var executor = new GraphExecutor(this, graph);
        timed("applyGraph", () -> {
            executor.applyGraph(craftingInventory);
            return null;
        });

        applyRecursiveIngredientReserve(craftingInventory);
        clearResolvedRecursiveMissingItems(craftingInventory);
        addRecursiveMissingSeedsToPlan();

        craftingInventory.addBytes((double) graph.getNodeCount() * 8);

        return CraftingSimulationState.buildCraftingPlan(craftingInventory, this, productionAmount);
    }

    @Nullable
    private CraftingPlan runCraftLessAttempt(long amount) throws InterruptedException {
        this.simulate = false;
        this.allowMissing = false;
        this.missing.clear();
        this.recursiveMissingSeeds.clear();
        for (var key : this.recursiveNetCache.keySet()) {
            this.recursiveNetKeyPool.release(key);
        }
        this.recursiveNetCache.clear();
        this.intermediateFinalOutputAmount = 0;
        this.realSeededRecursiveRequests.clear();
        this.realRecursiveSeeds.clear();
        this.realSeededRecursiveKeys.clear();
        this.recursiveFinalOutputInputs.clear();
        this.recursiveReserveCandidates.clear();
        this.recursiveDisplayRequests.clear();
        this.tree.resetPossible();

        final boolean logCrafting = AELog.isCraftingLogEnabled();
        final Stopwatch timer = logCrafting ? Stopwatch.createStarted() : null;
        final boolean trackPerformance = isPerformanceTrackingEnabled();
        final String attemptName = trackPerformance ? "craft-less amount=%d".formatted(amount) : null;
        final long attemptStart = trackPerformance ? System.nanoTime() : 0;

        ChildCraftingSimulationState craftingInventory = new ChildCraftingSimulationState(networkInv);

        long craftableAmount;
        if (trackPerformance) {
            craftableAmount = timed("craft-less-available " + attemptName,
                () -> this.tree.extractAvailableForCrafting(craftingInventory, amount));
        } else {
            craftableAmount = this.tree.extractAvailableForCrafting(craftingInventory, amount);
        }
        if (craftableAmount <= 0) {
            if (logCrafting) {
                this.attempts.add(new CraftAttempt(amount + " craft-less failed", timer));
            }
            if (trackPerformance) {
                recordPerformanceStage(attemptName + " failed", System.nanoTime() - attemptStart);
            }
            return null;
        }

        craftingInventory.addBytes((double) this.tree.getNodeCount() * 8);

        CraftingPlan plan;
        if (trackPerformance) {
            plan = timed("build-plan " + attemptName,
                () -> CraftingSimulationState.buildCraftingPlan(craftingInventory, this, craftableAmount));
        } else {
            plan = CraftingSimulationState.buildCraftingPlan(craftingInventory, this, craftableAmount);
        }
        if (logCrafting) {
            this.attempts.add(new CraftAttempt("%d craft-less (%d bytes)".formatted(craftableAmount, plan.bytes()),
                timer));
        }
        if (trackPerformance) {
            recordPerformanceStage(attemptName + " completed", System.nanoTime() - attemptStart);
        }
        return plan;
    }

    public void handlePausing() throws InterruptedException {
        if (this.incTime > AEConfig.CRAFTING.craftingCalculationPausingInterval) {
            this.incTime = 0;

            synchronized (this.monitor) {
                if (this.watch.elapsed(TimeUnit.MICROSECONDS) > this.time) {
                    this.running = false;
                    this.watch.stop();
                    this.monitor.notify();
                }

                if (!this.running) {
                    AELog.craftingDebug("crafting job will now sleep");

                    while (!this.running) {
                        this.monitor.wait();
                    }

                    AELog.craftingDebug("crafting job now active");
                }
            }

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
        this.incTime++;
    }

    private void finish() {
        synchronized (this.monitor) {
            this.running = false;
            this.done = true;
            this.monitor.notify();
        }
    }

    public boolean isSimulation() {
        return this.simulate;
    }

    boolean canUseMissingItems() {
        return (this.simulate || this.allowMissing) && this.missingSuppression == 0;
    }

    void pushMissingSuppression() {
        this.missingSuppression++;
    }

    void popMissingSuppression() {
        this.missingSuppression--;
    }

    boolean isRequesting(AEKey what) {
        for (int i = 0; i < this.requestStack.size(); i++) {
            if (this.requestStack.get(i).equals(what)) {
                return true;
            }
        }
        return false;
    }

    boolean isCheckingAvailability(AEKey what) {
        for (int i = 0; i < this.availabilityStack.size(); i++) {
            if (this.availabilityStack.get(i).equals(what)) {
                return true;
            }
        }
        return false;
    }

    void pushRequest(AEKey what) {
        this.requestStack.add(what);
        this.maxRequestDepth = Math.max(this.maxRequestDepth, this.requestStack.size());
    }

    void popRequest() {
        this.requestStack.removeLast();
    }

    @Nullable
    AEKey getCurrentRequestKey() {
        return this.requestStack.isEmpty() ? null : this.requestStack.getLast();
    }

    void pushAvailabilityCheck(AEKey what) {
        this.availabilityStack.add(what);
    }

    void popAvailabilityCheck() {
        this.availabilityStack.removeLast();
    }

    void pushProcess(CraftingTreeProcess process) {
        this.processStack.add(process);
        int previousHash = this.processHashPrefixes.getInt(this.processHashPrefixes.size() - 1);
        this.processHashPrefixes.add(31 * previousHash + System.identityHashCode(process));
        int previousPower = this.processHashPowers.getInt(this.processHashPowers.size() - 1);
        this.processHashPowers.add(31 * previousPower);
    }

    void popProcess() {
        this.processStack.removeLast();
        this.processHashPrefixes.removeInt(this.processHashPrefixes.size() - 1);
        this.processHashPowers.removeInt(this.processHashPowers.size() - 1);
    }

    public boolean cycleHasNetOutput(AEKey what) {
        return getCycleNetOutput(what) > 0;
    }

    long getCycleNetOutput(AEKey what) {
        int requestIndex = -1;
        for (int i = this.requestStack.size() - 1; i >= 0; i--) {
            if (this.requestStack.get(i).equals(what)) {
                requestIndex = i;
                break;
            }
        }
        if (requestIndex < 0) {
            return 0;
        }

        long netOutput = 0;
        for (int i = requestIndex; i < this.processStack.size(); i++) {
            var process = this.processStack.get(i);
            netOutput = LongMath.saturatedAdd(netOutput, process.getOutputCount(what));
            netOutput = LongMath.saturatedSubtract(netOutput, process.getInputCount(what));
        }
        return netOutput;
    }

    boolean resolveRecursiveRequest(AEKey what, CraftingSimulationState inv, long amount) {
        var resolution = getRecursiveResolution(what, inv);
        if (resolution == null) {
            return false;
        }

        if (resolution.seed() != null) {
            if (resolution.missingSeed() != null) {
                clearRecursiveMissingSeed(resolution.missingSeed().what());
            }
            clearRecursiveMissingSeed(resolution.seed().what());
            inv.extract(resolution.seed().what(), resolution.seed().amount(), Actionable.MODULATE);
            inv.insert(what, amount, Actionable.MODULATE);
            return true;
        }

        if (resolution.missingSeeds()) {
            this.recursiveMissingSeedSuppression++;
            try {
                inv.insert(what, amount, Actionable.MODULATE);
                return true;
            } finally {
                this.recursiveMissingSeedSuppression--;
            }
        }

        return false;
    }

    boolean canResolveRecursiveRequest(AEKey what, CraftingSimulationState inv, long locallyExtractedAmount) {
        return getRecursiveResolution(what, inv, locallyExtractedAmount) != null;
    }

    private RecursiveResolution getRecursiveResolution(AEKey what, CraftingSimulationState inv) {
        return getRecursiveResolution(what, inv, 0);
    }

    private RecursiveResolution getRecursiveResolution(AEKey what, CraftingSimulationState inv,
                                                       long locallyExtractedAmount) {
        var recursiveNet = getRecursiveNet(what);
        if (recursiveNet == null || !recursiveNet.canResolve()) {
            return null;
        }

        var seed = getRecursiveSeed(inv, recursiveNet, what, locallyExtractedAmount);
        if (seed != null) {
            this.realSeededRecursiveRequests.add(getRecursiveRootKey(recursiveNet.requestIndex()));
            this.realRecursiveSeeds.add(seed.what());
            addRecursiveReserveCandidates(recursiveNet);
            addRealSeededRecursiveKeys(recursiveNet.netByKey());
            var missingSeed = getMissingRecursiveSeed(recursiveNet, what);
            if (missingSeed != null && recursiveNet.netByKey().get(missingSeed.what()) >= 0) {
                clearRecursiveMissingSeed(missingSeed.what());
            }
            return new RecursiveResolution(seed, false, missingSeed);
        }

        AEKey recursiveRoot = getRecursiveRootKey(recursiveNet.requestIndex());
        if (canUseMissingItems() && this.recursiveMissingSeedSuppression == 0) {
            var missingSeed = getMissingRecursiveSeed(recursiveNet, what);
            if (missingSeed == null) {
                return null;
            }
            if (this.applyingRecursiveIngredientReserve && isReserveProtectedMissingSeed(missingSeed.what())) {
                this.recursiveReserveBlockedByMissingSeed = true;
                return null;
            }
            if (!this.realSeededRecursiveRequests.contains(recursiveRoot)) {
                addRecursiveMissingSeed(missingSeed.what(), missingSeed.amount());
            }
            addRecursiveReserveCandidates(recursiveNet);
            return new RecursiveResolution(null, true, missingSeed);
        }

        return null;
    }

    private AEKey getRecursiveRootKey(int requestIndex) {
        return this.requestStack.get(requestIndex);
    }

    private RecursiveNet getRecursiveNet(AEKey what) {
        int requestIndex = findRecursiveRequestIndex(what);
        if (requestIndex < 0) {
            return null;
        }

        int processCount = this.processStack.size() - requestIndex;
        int processHash = getProcessSuffixHash(requestIndex, processCount);
        this.recursiveNetLookup.reset(what, requestIndex, this.processStack, requestIndex, processCount, processHash);

        var cached = this.recursiveNetCache.get(this.recursiveNetLookup);
        if (cached != null) {
            return cached;
        }

        var frozenKey = this.recursiveNetLookup.freeze(this.recursiveNetKeyPool);
        var computed = computeRecursiveNet(frozenKey);
        this.recursiveNetCache.put(frozenKey, computed);
        return computed;
    }

    private int getProcessSuffixHash(int offset, int length) {
        int endHash = this.processHashPrefixes.getInt(offset + length);
        int startHash = this.processHashPrefixes.getInt(offset);
        return endHash - startHash * this.processHashPowers.getInt(length);
    }

    private int findRecursiveRequestIndex(AEKey what) {
        for (int i = this.requestStack.size() - 1; i >= 0; i--) {
            if (this.requestStack.get(i).equals(what)) {
                return i;
            }
        }
        return -1;
    }

    private RecursiveNet computeRecursiveNet(RecursiveNetKey key) {
        recordPerformanceCount("recursive-net-analysis", 1);
        var netByKey = new KeyCounter();
        var inputKeys = new ObjectOpenHashSet<AEKey>();
        var includedPatterns = new ObjectOpenHashSet<IPatternDetails>();
        for (int i = 0; i < key.processCount(); i++) {
            var process = key.processAt(i);
            process.accumulateNet(netByKey);
            process.accumulateInputKeys(inputKeys);
            includedPatterns.add(process.details);
        }

        expandRecursiveNetClosure(netByKey, inputKeys, includedPatterns);

        boolean hasPositiveNet = false;
        for (var entry : netByKey) {
            if (entry.getLongValue() > 0) {
                hasPositiveNet = true;
                break;
            }
        }

        return new RecursiveNet(key.requestIndex(), netByKey, inputKeys,
            hasPositiveNet && netByKey.get(key.what()) >= 0);
    }

    long getExpandedPatternNetOutput(IPatternDetails pattern, AEKey what) {
        var outputsByKey = this.expandedPatternNetOutputCache.computeIfAbsent(pattern,
            ignored -> new AEKey2LongMap.OpenHashMap());
        return outputsByKey.computeIfAbsent(what, key -> computeExpandedPatternNetOutput(pattern, (AEKey) key));
    }

    private long computeExpandedPatternNetOutput(IPatternDetails pattern, AEKey what) {
        long directOutput = getPatternOutputCount(pattern, what);
        if (directOutput <= 0) {
            return 0;
        }

        var netByKey = new KeyCounter();
        var includedPatterns = new ObjectOpenHashSet<IPatternDetails>();
        includedPatterns.add(pattern);
        accumulatePatternNet(netByKey, pattern);
        expandRecursiveNetClosure(netByKey, null, includedPatterns);

        long netOutput = netByKey.get(what);
        if (netOutput > 0 && netOutput < directOutput) {
            return netOutput;
        }
        return directOutput;
    }

    RecursivePatternBatch getRecursivePatternBatch(IPatternDetails pattern, AEKey what) {
        var batchesByKey = this.recursivePatternBatchCache.computeIfAbsent(pattern,
            ignored -> new Object2ObjectOpenHashMap<>());
        return batchesByKey.computeIfAbsent(what, key -> computeRecursivePatternBatch(pattern, key));
    }

    private RecursivePatternBatch computeRecursivePatternBatch(IPatternDetails pattern, AEKey what) {
        long directOutput = getPatternOutputCount(pattern, what);
        if (directOutput <= 0) {
            return new RecursivePatternBatch(1, 0);
        }

        boolean rootConsumesTarget = getPatternInputCount(pattern, what) > 0;
        for (long rootTimes = 1; rootTimes <= 64; rootTimes++) {
            var netByKey = new KeyCounter();
            var recursiveUse = new RecursiveUse(rootConsumesTarget);
            accumulatePatternNet(netByKey, pattern, rootTimes);
            if (!expandRecursiveBatchNet(netByKey, what, recursiveUse)) {
                return new RecursivePatternBatch(1, directOutput);
            }

            if (!recursiveUse.get()) {
                return new RecursivePatternBatch(1, directOutput);
            }

            long netOutput = netByKey.get(what);
            if (netOutput > 0) {
                return new RecursivePatternBatch(rootTimes, netOutput);
            }
        }

        return new RecursivePatternBatch(1, directOutput);
    }

    private RecursiveSeed getRecursiveSeed(CraftingSimulationState inv, RecursiveNet recursiveNet, AEKey what,
                                           long locallyExtractedAmount) {
        for (AEKey seed : recursiveNet.inputKeys()) {
            if (recursiveNet.netByKey().get(seed) < 0 || isReserveProtectedMissingSeed(seed)) {
                continue;
            }
            long amount = getRecursiveSeedAmount(seed, recursiveNet.requestIndex());
            long available = inv.getAvailableNonProducedAmount(seed);
            if (seed.equals(what)) {
                available += locallyExtractedAmount;
            }
            if (available >= amount) {
                return new RecursiveSeed(seed, amount);
            }
        }
        return null;
    }

    private long getRecursiveSeedAmount(AEKey seed, int requestIndex) {
        if (requestIndex >= 0 && requestIndex < this.processStack.size()) {
            for (int i = requestIndex; i < this.processStack.size(); i++) {
                var process = this.processStack.get(i);
                long amount = process.getInputCount(seed);
                if (amount > 0) {
                    return amount;
                }
            }
        }
        return 1;
    }

    private RecursiveSeed getMissingRecursiveSeed(RecursiveNet recursiveNet, AEKey what) {
        int requestIndex = recursiveNet.requestIndex();
        if (requestIndex >= 0 && requestIndex < this.processStack.size()) {
            for (AEKey seed : recursiveNet.inputKeys()) {
                if (seed.equals(what) || recursiveNet.netByKey().get(seed) < 0 || isReserveProtectedMissingSeed(seed)) {
                    continue;
                }
                return new RecursiveSeed(seed, getRecursiveSeedAmount(seed, requestIndex));
            }
        }
        return null;
    }

    private void addRecursiveMissingSeed(AEKey what, long amount) {
        if (hasRealSeededRecursiveRequestFor(what)) {
            clearRecursiveMissingSeed(what);
            return;
        }
        if (this.realRecursiveSeeds.contains(what)) {
            clearRecursiveMissingSeed(what);
            return;
        }
        long existing = this.recursiveMissingSeeds.get(what);
        if (existing >= amount) {
            return;
        }

        long delta = amount - existing;
        this.recursiveMissingSeeds.add(what, delta);
    }

    private void clearRecursiveMissingSeed(AEKey what) {
        this.recursiveMissingSeeds.remove(what);
        this.missing.remove(what);
    }

    private void applyRecursiveIngredientReserve(CraftingSimulationState inv) throws InterruptedException {
        if (this.recursiveIngredientReserveAmount <= 0 || this.recursiveReserveCandidates.isEmpty()) {
            return;
        }

        var protectedMissingSeeds = getRecursiveMissingSeedsMarker();
        this.reserveProtectedMissingSeeds = protectedMissingSeeds;
        this.applyingRecursiveIngredientReserve = true;
        try {
            var reserveCandidates = new ObjectArrayList<AEKey>();
            for (var entry : this.recursiveReserveCandidates) {
                reserveCandidates.add(entry.getKey());
            }
            for (int i = 0; i < reserveCandidates.size(); i++) {
                var what = reserveCandidates.get(i);
                if (protectedMissingSeeds.get(what) > 0) {
                    continue;
                }

                long reservePerBatch = this.recursiveReserveCandidates.get(what);
                if (reservePerBatch <= 0) {
                    continue;
                }

                long targetReserve = Math.min(
                    LongMath.saturatedMultiply(this.recursiveIngredientReserveAmount, reservePerBatch),
                    inv.getOriginalAmount(what));
                if (targetReserve <= 0) {
                    continue;
                }

                long available = inv.getAvailableAmount(what);
                if (available >= targetReserve) {
                    continue;
                }

                long deficit = targetReserve - available;
                if (getCraftingFor(what).isEmpty()) {
                    long returned = inv.returnExtractedForReserve(what, deficit);
                    if (returned > 0) {
                        this.missing.add(what, returned);
                    }
                    continue;
                }

                var reserveNode = new CraftingTreeNode(this.craftingService, this, what, 1, null, -1);
                var branchInv = new ChildCraftingSimulationState(inv);
                var branchMarker = createRecursiveReserveBranchMarker();
                this.recursiveReserveBlockedByMissingSeed = false;
                try {
                    if (isPerformanceTrackingEnabled()) {
                        runTimedCrafting("recursive-reserve " + what,
                            () -> reserveNode.request(branchInv, deficit, null));
                    } else {
                        reserveNode.request(branchInv, deficit, null);
                    }
                    if (this.recursiveReserveBlockedByMissingSeed) {
                        restoreRecursiveReserveBranchMarker(branchMarker);
                        continue;
                    }
                    branchInv.applyDiff(inv);
                    addRecursiveReserveDisplayRequest(what, deficit);
                } catch (CraftBranchFailure failure) {
                    if (this.recursiveReserveBlockedByMissingSeed) {
                        restoreRecursiveReserveBranchMarker(branchMarker);
                        continue;
                    }
                    if (failure.hasExplicitMessageKey()) {
                        throw new CraftingCalculationFailure(failure.getLocalizedMessageKey());
                    }
                    restoreRecursiveReserveBranchMarker(branchMarker);
                    this.missing.add(what, deficit);
                } finally {
                    this.recursiveReserveBlockedByMissingSeed = false;
                }
            }
        } finally {
            restoreProtectedRecursiveMissingSeeds(protectedMissingSeeds);
            this.applyingRecursiveIngredientReserve = false;
            this.reserveProtectedMissingSeeds = null;
            this.recursiveReserveBlockedByMissingSeed = false;
        }
    }

    private RecursiveReserveBranchMarker createRecursiveReserveBranchMarker() {
        return new RecursiveReserveBranchMarker(
            getMissingItemsMarker(),
            getRecursiveMissingSeedsMarker(),
            getRealSeededRecursiveRequestsMarker(),
            getRealRecursiveSeedsMarker(),
            getRealSeededRecursiveKeysMarker(),
            getRecursiveDisplayRequestsMarker(),
            getIntermediateFinalOutputMarker());
    }

    private void restoreRecursiveReserveBranchMarker(RecursiveReserveBranchMarker marker) {
        restoreMissingItemsMarker(marker.missingItems());
        restoreRecursiveMissingSeedsMarker(marker.recursiveMissingSeeds());
        restoreRealSeededRecursiveRequestsMarker(marker.realSeededRecursiveRequests());
        restoreRealRecursiveSeedsMarker(marker.realRecursiveSeeds());
        restoreRealSeededRecursiveKeysMarker(marker.realSeededRecursiveKeys());
        restoreRecursiveDisplayRequestsMarker(marker.recursiveDisplayRequests());
        restoreIntermediateFinalOutputMarker(marker.intermediateFinalOutputAmount());
    }

    private void restoreProtectedRecursiveMissingSeeds(KeyCounter protectedSeeds) {
        for (var entry : protectedSeeds) {
            long current = this.recursiveMissingSeeds.get(entry.getKey());
            if (current < entry.getLongValue()) {
                this.recursiveMissingSeeds.add(entry.getKey(), entry.getLongValue() - current);
            }
        }
    }

    private void addRecursiveReserveDisplayRequest(AEKey what, long amount) {
        var displayNode = this.tree.findDisplayNodeFor(what);
        if (displayNode != null) {
            addRecursiveDisplayRequest(displayNode, amount);
        }
    }

    private void addRecursiveMissingSeedsToPlan() {
        for (var entry : this.recursiveMissingSeeds) {
            if (hasRealSeededRecursiveRequestFor(entry.getKey()) || this.realRecursiveSeeds.contains(entry.getKey())
                || this.realSeededRecursiveKeys.contains(entry.getKey())
                || this.missing.get(entry.getKey()) >= entry.getLongValue()) {
                continue;
            }
            this.missing.add(entry.getKey(), entry.getLongValue());
        }
    }

    private void clearResolvedRecursiveMissingItems(CraftingSimulationState inv) {
        var keys = new ObjectArrayList<AEKey>();
        for (var entry : this.missing) {
            var key = entry.getKey();
            if (this.realSeededRecursiveKeys.contains(key) || this.realRecursiveSeeds.contains(key)
                || hasRealSeededRecursiveRequestFor(key)
                || (this.recursiveMissingSeeds.get(key) <= 0
                && inv.getCraftedAmount(key) >= entry.getLongValue())) {
                keys.add(key);
            }
        }
        for (int i = 0; i < keys.size(); i++) {
            this.missing.remove(keys.get(i));
        }
    }

    private boolean hasRealSeededRecursiveRequestFor(AEKey seed) {
        if (isReserveProtectedMissingSeed(seed)) {
            return false;
        }
        for (AEKey recursiveRoot : this.realSeededRecursiveRequests) {
            var recursiveNet = getRecursiveNet(recursiveRoot);
            if (recursiveNet != null && recursiveNet.netByKey().get(seed) >= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean isReserveProtectedMissingSeed(AEKey what) {
        return this.reserveProtectedMissingSeeds != null && this.reserveProtectedMissingSeeds.get(what) > 0;
    }

    private void addRealSeededRecursiveKeys(KeyCounter netByKey) {
        for (var entry : netByKey) {
            if (entry.getLongValue() >= 0) {
                var key = entry.getKey();
                if (isReserveProtectedMissingSeed(key)) {
                    continue;
                }
                this.realSeededRecursiveKeys.add(key);
                clearRecursiveMissingSeed(key);
            }
        }
    }

    private void addRecursiveReserveCandidates(RecursiveNet recursiveNet) {
        for (var entry : recursiveNet.netByKey()) {
            AEKey key = entry.getKey();
            if (entry.getLongValue() < 0 || isReserveProtectedMissingSeed(key)) {
                continue;
            }

            long reservePerBatch = entry.getLongValue();
            if (reservePerBatch <= 0 && recursiveNet.inputKeys().contains(key)) {
                reservePerBatch = getRecursiveSeedAmount(key, recursiveNet.requestIndex());
            }
            if (reservePerBatch <= 0) {
                continue;
            }

            long existing = this.recursiveReserveCandidates.get(key);
            if (existing < reservePerBatch) {
                this.recursiveReserveCandidates.add(key, reservePerBatch - existing);
            }
        }
    }

    private void expandRecursiveNetClosure(KeyCounter netByKey, @Nullable Collection<AEKey> inputKeys,
                                           Set<IPatternDetails> includedPatterns) {
        boolean changed;
        do {
            changed = false;
            var negativeKeys = new ObjectArrayList<AEKey>();
            for (var entry : netByKey) {
                if (entry.getLongValue() < 0) {
                    negativeKeys.add(entry.getKey());
                }
            }

            for (int i = 0; i < negativeKeys.size(); i++) {
                var key = negativeKeys.get(i);
                var patterns = this.getCraftingFor(key);
                for (int j = 0, size = patterns.size(); j < size; j++) {
                    var pattern = patterns.get(j);
                    if (includedPatterns.add(pattern)) {
                        accumulatePatternNet(netByKey, inputKeys, pattern, 1);
                        changed = true;
                        break;
                    }
                }
            }
        } while (changed);
    }

    private boolean expandRecursiveBatchNet(KeyCounter netByKey, AEKey target, RecursiveUse recursiveUse) {
        for (int guard = 0; guard < 128; guard++) {
            AEKey missingKey = null;
            long missingAmount = 0;
            for (var entry : netByKey) {
                if (entry.getLongValue() < 0) {
                    missingKey = entry.getKey();
                    missingAmount = LongMath.saturatedSubtract(0, entry.getLongValue());
                    break;
                }
            }

            if (missingKey == null) {
                return true;
            }

            IPatternDetails selectedPattern = null;
            long outputAmount = 0;
            var candidates = this.getCraftingFor(missingKey);
            for (int i = 0, size = candidates.size(); i < size; i++) {
                var candidate = candidates.get(i);
                outputAmount = getPatternOutputCount(candidate, missingKey);
                if (outputAmount > 0) {
                    selectedPattern = candidate;
                    break;
                }
            }

            if (selectedPattern == null) {
                return recursiveUse.get() && netByKey.get(target) > 0;
            }

            if (getPatternInputCount(selectedPattern, target) > 0) {
                recursiveUse.set();
            }
            long times = divideCeil(missingAmount, outputAmount);
            accumulatePatternNet(netByKey, selectedPattern, times);
        }

        return false;
    }

    public AEKey getOutput() {
        return output;
    }

    public KeyCounter getMissingItems() {
        return missing;
    }

    public CraftingTreeNode getTree() {
        return tree;
    }

    public List<ICraftingProvider> getTemporaryProviders() {
        return temporaryProviders;
    }

    public long getIntermediateFinalOutputAmount() {
        return intermediateFinalOutputAmount;
    }

    void addIntermediateFinalOutput(long amount) {
        this.intermediateFinalOutputAmount = LongMath.saturatedAdd(this.intermediateFinalOutputAmount, amount);
    }

    void addRecursiveIntermediateFinalOutput(long amount) {
        this.intermediateFinalOutputAmount = LongMath.saturatedAdd(this.intermediateFinalOutputAmount, amount);
    }

    void addRecursiveFinalOutputInput(AEKey what) {
        this.recursiveFinalOutputInputs.add(what);
    }

    void addRecursiveDisplayRequest(CraftingTreeNode node, long amount) {
        long oldValue = this.recursiveDisplayRequests.addTo(node, amount);
        long newValue = oldValue + amount;
        if (newValue < oldValue) {
            this.recursiveDisplayRequests.put(node, Long.MAX_VALUE);
        }
    }

    long getRecursiveDisplayRequest(CraftingTreeNode node) {
        return this.recursiveDisplayRequests.getOrDefault(node, 0L);
    }

    Reference2LongMap<CraftingTreeNode> getRecursiveDisplayRequestsMarker() {
        return new Reference2LongOpenHashMap<>(this.recursiveDisplayRequests);
    }

    void restoreRecursiveDisplayRequestsMarker(Reference2LongMap<CraftingTreeNode> marker) {
        this.recursiveDisplayRequests.clear();
        this.recursiveDisplayRequests.putAll(marker);
    }

    Reference2LongMap<CraftingTreeNode> getRecursiveDisplayRequestsDelta(
        Reference2LongMap<CraftingTreeNode> marker) {
        var delta = new Reference2LongOpenHashMap<CraftingTreeNode>();
        for (var entry : this.recursiveDisplayRequests.reference2LongEntrySet()) {
            long valueDelta = entry.getLongValue() - marker.getOrDefault(entry.getKey(), 0L);
            if (valueDelta > 0) {
                delta.put(entry.getKey(), valueDelta);
            }
        }
        return delta;
    }

    void addRecursiveDisplayRequests(Reference2LongMap<CraftingTreeNode> delta) {
        for (var entry : delta.reference2LongEntrySet()) {
            addRecursiveDisplayRequest(entry.getKey(), entry.getLongValue());
        }
    }

    boolean isRecursiveFinalOutputInput(AEKey what) {
        return this.recursiveFinalOutputInputs.contains(what);
    }

    void addIntermediateFinalOutputInput(AEKey what, long amount) {
        if (what.equals(this.output)) {
            addIntermediateFinalOutput(amount);
        }
    }

    long getIntermediateFinalOutputMarker() {
        return this.intermediateFinalOutputAmount;
    }

    void restoreIntermediateFinalOutputMarker(long marker) {
        this.intermediateFinalOutputAmount = marker;
    }

    KeyCounter getRecursiveMissingSeedsMarker() {
        var marker = new KeyCounter();
        marker.addAll(this.recursiveMissingSeeds);
        return marker;
    }

    void restoreRecursiveMissingSeedsMarker(KeyCounter marker) {
        this.recursiveMissingSeeds.clear();
        this.recursiveMissingSeeds.addAll(marker);
    }

    Set<AEKey> getRealSeededRecursiveRequestsMarker() {
        return new ObjectOpenHashSet<>(this.realSeededRecursiveRequests);
    }

    void restoreRealSeededRecursiveRequestsMarker(Set<AEKey> marker) {
        this.realSeededRecursiveRequests.clear();
        this.realSeededRecursiveRequests.addAll(marker);
    }

    void addRealSeededRecursiveRequests(Collection<AEKey> requests) {
        this.realSeededRecursiveRequests.addAll(requests);
    }

    Set<AEKey> getRealRecursiveSeedsMarker() {
        return new ObjectOpenHashSet<>(this.realRecursiveSeeds);
    }

    void restoreRealRecursiveSeedsMarker(Set<AEKey> marker) {
        this.realRecursiveSeeds.clear();
        this.realRecursiveSeeds.addAll(marker);
    }

    void addRealRecursiveSeeds(Collection<AEKey> seeds) {
        for (AEKey seed : seeds) {
            if (!isReserveProtectedMissingSeed(seed)) {
                this.realRecursiveSeeds.add(seed);
            }
        }
    }

    Set<AEKey> getRealSeededRecursiveKeysMarker() {
        return new ObjectOpenHashSet<>(this.realSeededRecursiveKeys);
    }

    void restoreRealSeededRecursiveKeysMarker(Set<AEKey> marker) {
        this.realSeededRecursiveKeys.clear();
        this.realSeededRecursiveKeys.addAll(marker);
    }

    void addRealSeededRecursiveKeys(Collection<AEKey> keys) {
        for (AEKey key : keys) {
            if (!isReserveProtectedMissingSeed(key)) {
                this.realSeededRecursiveKeys.add(key);
            }
        }
    }

    void applyRecursiveMissingSeedPreview(KeyCounter clearedSeeds, KeyCounter addedSeeds) {
        for (var entry : clearedSeeds) {
            if (!isReserveProtectedMissingSeed(entry.getKey())) {
                clearRecursiveMissingSeed(entry.getKey());
            }
        }
        this.recursiveMissingSeeds.addAll(addedSeeds);
    }

    KeyCounter getMissingItemsMarker() {
        var marker = new KeyCounter();
        marker.addAll(this.missing);
        return marker;
    }

    void restoreMissingItemsMarker(KeyCounter marker) {
        this.missing.clear();
        this.missing.addAll(marker);
    }

    World getLevel() {
        return this.level;
    }

    /**
     * returns true if this needs more simulation.
     *
     * @param micros microseconds of simulation
     * @return true if this needs more simulation
     */
    public boolean simulateFor(int micros) {
        this.time = micros;

        synchronized (this.monitor) {
            if (this.done) {
                return false;
            }

            this.watch.reset();
            this.watch.start();
            this.running = true;
            invalidateMachineLocationCaches();

            AELog.craftingDebug("main thread is now going to sleep");

            this.monitor.notify();

            while (this.running) {
                try {
                    this.monitor.wait();
                } catch (InterruptedException ignored) {
                }
            }

            AELog.craftingDebug("main thread is now active");
        }

        return true;
    }

    private void logCraftingJob(ICraftingPlan plan) {
        if (AELog.isCraftingLogEnabled()) {
            StringBuilder message = new StringBuilder();
            message.append("CraftingCalculation issued by %s requesting [%dx%s] breakdown:\n".formatted(
                getActionSourceName(), this.requestedAmount, this.output));
            for (int i = 0; i < this.attempts.size(); i++) {
                var attempt = this.attempts.get(i);
                message.append(" - %s in %d ms\n".formatted(
                    attempt.description(), attempt.stopwatch().elapsed(TimeUnit.MILLISECONDS)));
            }
            message.append(" - final plan: %d (%d bytes)".formatted(plan.finalOutput().amount(), plan.bytes()));

            AELog.crafting(message.toString());
        }
    }

    private String getActionSourceName() {
        var actionSource = this.simRequester.getActionSource();
        if (actionSource != null && actionSource.player().isPresent()) {
            var player = actionSource.player().get();
            return player.toString();
        }
        if (actionSource != null && actionSource.machine().isPresent()) {
            var machineSource = actionSource.machine().get();
            var actionableNode = machineSource.getActionableNode();
            return actionableNode != null ? actionableNode.toString() : machineSource.toString();
        }
        return "[unknown source]";
    }

    public boolean hasMultiplePaths() {
        return this.tree.hasMultiplePaths();
    }

    int getTreeDepth() {
        return this.tree.getDepth();
    }

    long getTreeNodeCount() {
        return this.tree.getNodeCount();
    }

    long getPatternNodeCount() {
        return this.tree.getPatternNodeCount();
    }

    int getMaxRequestDepth() {
        return this.maxRequestDepth;
    }

    boolean isPerformanceTrackingEnabled() {
        return this.performanceListener.isEnabled();
    }

    void recordPerformanceStage(String name, long nanos) {
        if (this.performanceListener.isEnabled()) {
            this.performanceListener.stage(name, nanos);
        }
    }

    void recordPerformanceSelfStage(String name, long nanos) {
        if (this.performanceListener.isEnabled()) {
            this.performanceListener.selfStage(name, nanos);
        }
    }

    void recordPerformanceCount(String name, long amount) {
        if (this.performanceListener.isEnabled()) {
            this.performanceListener.count(name, amount);
        }
    }

    private void finishPerformanceListener(long nanos) {
        if (this.performanceListener.isEnabled()) {
            this.performanceListener.finish(nanos, this);
        }
    }

    <T> T timed(String name, InterruptibleSupplier<T> supplier) throws InterruptedException {
        if (!this.performanceListener.isEnabled()) {
            return supplier.get();
        }
        long start = System.nanoTime();
        this.timingStack.add(new TimingFrame());
        try {
            return supplier.get();
        } finally {
            long total = System.nanoTime() - start;
            var frame = this.timingStack.removeLast();
            if (!this.timingStack.isEmpty()) {
                this.timingStack.getLast().childNanos += total;
            }
            recordPerformanceStage(name, total);
            recordPerformanceSelfStage(name, Math.max(0, total - frame.childNanos));
        }
    }

    void runTimedCrafting(String name, CraftingRunnable runnable)
        throws InterruptedException, CraftBranchFailure {
        if (!this.performanceListener.isEnabled()) {
            runnable.run();
            return;
        }
        long start = System.nanoTime();
        this.timingStack.add(new TimingFrame());
        try {
            runnable.run();
        } finally {
            long total = System.nanoTime() - start;
            var frame = this.timingStack.removeLast();
            if (!this.timingStack.isEmpty()) {
                this.timingStack.getLast().childNanos += total;
            }
            recordPerformanceStage(name, total);
            recordPerformanceSelfStage(name, Math.max(0, total - frame.childNanos));
        }
    }

    @FunctionalInterface
    interface InterruptibleSupplier<T> {
        T get() throws InterruptedException;
    }

    @FunctionalInterface
    interface CraftingRunnable {
        void run() throws InterruptedException, CraftBranchFailure;
    }

    private record CraftAttempt(String description, Stopwatch stopwatch) {
    }

    private record RecursiveSeed(AEKey what, long amount) {
    }

    private static final class RecursiveNetKey {
        private AEKey what;
        private int requestIndex;
        private ObjectArrayList<CraftingTreeProcess> liveProcesses;
        private CraftingTreeProcess[] frozenProcesses;
        private int processOffset;
        private int processCount;
        private int hashCode;

        private RecursiveNetKey() {
        }

        private static RecursiveNetKey mutableProbe() {
            return new RecursiveNetKey();
        }

        private void reset(AEKey what, int requestIndex, ObjectArrayList<CraftingTreeProcess> processes,
                           int processOffset, int processCount, int processHash) {
            this.what = what;
            this.requestIndex = requestIndex;
            this.liveProcesses = processes;
            this.processOffset = processOffset;
            this.processCount = processCount;
            this.hashCode = computeHashCode(what, requestIndex, processCount, processHash);
        }

        private RecursiveNetKey freeze(ObjectPool<RecursiveNetKey> pool) {
            var processes = new CraftingTreeProcess[this.processCount];
            for (int i = 0; i < this.processCount; i++) {
                processes[i] = processAt(i);
            }
            var key = pool.acquire();
            key.what = this.what;
            key.requestIndex = this.requestIndex;
            key.frozenProcesses = processes;
            key.liveProcesses = null;
            key.processOffset = 0;
            key.processCount = processes.length;
            key.hashCode = computeHashCode(this.what, this.requestIndex, this.processCount,
                getProcessIdentityHash(processes));
            return key;
        }

        private static int getProcessIdentityHash(CraftingTreeProcess[] processes) {
            int result = 0;
            for (CraftingTreeProcess process : processes) {
                result = 31 * result + System.identityHashCode(process);
            }
            return result;
        }

        private static int computeHashCode(AEKey what, int requestIndex, int processCount, int processHash) {
            int result = what.hashCode();
            result = 31 * result + requestIndex;
            result = 31 * result + processCount;
            return 31 * result + processHash;
        }

        private AEKey what() {
            return this.what;
        }

        private int requestIndex() {
            return this.requestIndex;
        }

        private int processCount() {
            return this.processCount;
        }

        private CraftingTreeProcess processAt(int index) {
            if (this.frozenProcesses != null) {
                return this.frozenProcesses[index];
            }
            return this.liveProcesses.get(this.processOffset + index);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RecursiveNetKey other)
                || this.hashCode != other.hashCode
                || this.requestIndex != other.requestIndex
                || this.processCount != other.processCount
                || !this.what.equals(other.what)) {
                return false;
            }
            for (int i = 0; i < this.processCount; i++) {
                if (this.processAt(i) != other.processAt(i)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }

    private record RecursiveNet(int requestIndex, KeyCounter netByKey, Collection<AEKey> inputKeys,
                                boolean canResolve) {
    }

    private record RecursiveResolution(RecursiveSeed seed, boolean missingSeeds, RecursiveSeed missingSeed) {
    }

    private record RecursiveReserveBranchMarker(KeyCounter missingItems,
                                                KeyCounter recursiveMissingSeeds,
                                                Set<AEKey> realSeededRecursiveRequests,
                                                Set<AEKey> realRecursiveSeeds,
                                                Set<AEKey> realSeededRecursiveKeys,
                                                Reference2LongMap<CraftingTreeNode> recursiveDisplayRequests,
                                                long intermediateFinalOutputAmount) {
    }

    record RecursivePatternBatch(long rootTimes, long netOutput) {
    }

    private static final class RecursiveUse {
        private boolean aBoolean;

        private RecursiveUse(boolean aBoolean) {
            this.aBoolean = aBoolean;
        }

        private boolean get() {
            return this.aBoolean;
        }

        private void set() {
            this.aBoolean = true;
        }
    }

    private static final class TimingFrame {
        private long childNanos;
    }

    static final class MemoKey {
        private final AEKey what;
        private final int fingerprint;
        private final int hashCode;

        private MemoKey(AEKey what, int fingerprint) {
            this.what = what;
            this.fingerprint = fingerprint;
            this.hashCode = Objects.hash(what, fingerprint);
        }

        static MemoKey create(AEKey what, CraftingCalculation job) {
            int fp = computeFingerprint(what, job);
            return new MemoKey(what, fp);
        }

        private static int computeFingerprint(AEKey what, CraftingCalculation job) {
            int fp = 0;

            var patterns = job.getCraftingFor(what);
            fp = hashCombine(fp, patterns.size());

            for (int i = 0, size = patterns.size(); i < size; i++) {
                var pattern = patterns.get(i);
                fp = hashCombine(fp, pattern.hashCode());

                var inputs = pattern.getInputs();
                fp = hashCombine(fp, inputs.length);
                for (var input : inputs) {
                    var possibleInputs = input.possibleInputs();
                    fp = hashCombine(fp, possibleInputs.length);
                    for (GenericStack possible : possibleInputs) {
                        AEKey depKey = possible.what();
                        fp = hashCombine(fp, depKey.hashCode());

                        var subPatterns = job.getCraftingFor(depKey);
                        fp = hashCombine(fp, subPatterns.isEmpty() ? 0 : 1);
                    }
                }
            }
            return fp;
        }

        private static int hashCombine(int hash, int value) {
            return 31 * hash + value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MemoKey other)) return false;
            return fingerprint == other.fingerprint && what.equals(other.what);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    static final class MemoResult {
        final KeyCounter extracted;
        final KeyCounter containerItems;
        final Reference2LongOpenHashMap<IPatternDetails> patternTimes;
        double bytes;
        long baseTimes;

        final KeyCounter emittedItems;
        final KeyCounter missingItems;
        final KeyCounter insertedItems;
        final KeyCounter pseudoItems;
        long intermediateFinalOutputAmount;

        final KeyCounter recursiveMissingSeeds;
        final KeyCounter clearedRecursiveMissingSeeds;
        final ObjectOpenHashSet<AEKey> realSeededRecursiveRequests;
        final ObjectOpenHashSet<AEKey> realRecursiveSeeds;
        final ObjectOpenHashSet<AEKey> realSeededRecursiveKeys;
        final Reference2LongOpenHashMap<CraftingTreeNode> recursiveDisplayRequestsDelta;

        MemoResult() {
            this.extracted = new KeyCounter();
            this.containerItems = new KeyCounter();
            this.patternTimes = new Reference2LongOpenHashMap<>();
            this.patternTimes.defaultReturnValue(0);
            this.bytes = 0;

            this.emittedItems = new KeyCounter();
            this.missingItems = new KeyCounter();
            this.insertedItems = new KeyCounter();
            this.pseudoItems = new KeyCounter();
            this.intermediateFinalOutputAmount = 0;

            this.recursiveMissingSeeds = new KeyCounter();
            this.clearedRecursiveMissingSeeds = new KeyCounter();
            this.realSeededRecursiveRequests = new ObjectOpenHashSet<>();
            this.realRecursiveSeeds = new ObjectOpenHashSet<>();
            this.realSeededRecursiveKeys = new ObjectOpenHashSet<>();
            this.recursiveDisplayRequestsDelta = new Reference2LongOpenHashMap<>();
            this.recursiveDisplayRequestsDelta.defaultReturnValue(0);
        }

        boolean isApplicable(CraftingSimulationState inv) {
            for (var entry : extracted) {
                if (inv.getAvailableAmount(entry.getKey()) < entry.getLongValue()) {
                    return false;
                }
            }

            return true;
        }

        void recordExtraction(AEKey key, long amount) {
            this.extracted.add(key, amount);
        }

        void recordContainerItem(AEKey key, long amount) {
            this.containerItems.add(key, amount);
        }

        void recordPattern(IPatternDetails pattern, long times) {
            this.patternTimes.addTo(pattern, times);
        }

        void recordBytes(double bytes) {
            this.bytes += bytes;
        }

        // 新增：副作用记录方法
        void recordEmitted(AEKey key, long amount) {
            this.emittedItems.add(key, amount);
        }

        void recordMissing(AEKey key, long amount) {
            this.missingItems.add(key, amount);
        }

        void recordInserted(AEKey key, long amount) {
            this.insertedItems.add(key, amount);
        }

        void recordPseudo(AEKey key, long amount) {
            this.pseudoItems.add(key, amount);
        }

        void recordIntermediateFinalOutput(long amount) {
            this.intermediateFinalOutputAmount += amount;
        }

        void recordRecursiveMissingSeeds(KeyCounter delta) {
            this.recursiveMissingSeeds.addAll(delta);
        }

        void recordClearedRecursiveMissingSeeds(KeyCounter delta) {
            this.clearedRecursiveMissingSeeds.addAll(delta);
        }

        void recordRealSeededRecursiveRequests(ObjectOpenHashSet<AEKey> keys) {
            this.realSeededRecursiveRequests.addAll(keys);
        }

        void recordRealRecursiveSeeds(ObjectOpenHashSet<AEKey> keys) {
            this.realRecursiveSeeds.addAll(keys);
        }

        void recordRealSeededRecursiveKeys(ObjectOpenHashSet<AEKey> keys) {
            this.realSeededRecursiveKeys.addAll(keys);
        }

        void recordRecursiveDisplayRequests(Reference2LongOpenHashMap<CraftingTreeNode> delta) {
            for (var entry : delta.reference2LongEntrySet()) {
                this.recursiveDisplayRequestsDelta.addTo(entry.getKey(), entry.getLongValue());
            }
        }

        Bundle toBundle(long baseTimes) {
            return new Bundle(this, baseTimes);
        }
    }

    static final class Bundle {
        final KeyCounter extracted;
        final KeyCounter containerItems;
        final Reference2LongOpenHashMap<IPatternDetails> patternTimes;
        final long baseTimes;
        double bytes;

        // 新增：副作用字段（与 MemoResult 对应）
        final KeyCounter emittedItems;
        final KeyCounter missingItems;
        final KeyCounter insertedItems;
        final KeyCounter pseudoItems;
        long intermediateFinalOutputAmount;

        final KeyCounter recursiveMissingSeeds;
        final KeyCounter clearedRecursiveMissingSeeds;
        final ObjectOpenHashSet<AEKey> realSeededRecursiveRequests;
        final ObjectOpenHashSet<AEKey> realRecursiveSeeds;
        final ObjectOpenHashSet<AEKey> realSeededRecursiveKeys;
        final Reference2LongOpenHashMap<CraftingTreeNode> recursiveDisplayRequestsDelta;

        Bundle(MemoResult result, long baseTimes) {
            this.extracted = new KeyCounter();
            this.containerItems = new KeyCounter();
            this.patternTimes = new Reference2LongOpenHashMap<>();
            this.patternTimes.defaultReturnValue(0);

            this.extracted.addAll(result.extracted);
            this.containerItems.addAll(result.containerItems);
            this.patternTimes.putAll(result.patternTimes);
            this.baseTimes = baseTimes;
            this.bytes = result.bytes;

            this.emittedItems = new KeyCounter();
            this.emittedItems.addAll(result.emittedItems);
            this.missingItems = new KeyCounter();
            this.missingItems.addAll(result.missingItems);
            this.insertedItems = new KeyCounter();
            this.insertedItems.addAll(result.insertedItems);
            this.pseudoItems = new KeyCounter();
            this.pseudoItems.addAll(result.pseudoItems);
            this.intermediateFinalOutputAmount = result.intermediateFinalOutputAmount;

            this.recursiveMissingSeeds = new KeyCounter();
            this.recursiveMissingSeeds.addAll(result.recursiveMissingSeeds);
            this.clearedRecursiveMissingSeeds = new KeyCounter();
            this.clearedRecursiveMissingSeeds.addAll(result.clearedRecursiveMissingSeeds);
            this.realSeededRecursiveRequests = new ObjectOpenHashSet<>(result.realSeededRecursiveRequests);
            this.realRecursiveSeeds = new ObjectOpenHashSet<>(result.realRecursiveSeeds);
            this.realSeededRecursiveKeys = new ObjectOpenHashSet<>(result.realSeededRecursiveKeys);
            this.recursiveDisplayRequestsDelta = new Reference2LongOpenHashMap<>(result.recursiveDisplayRequestsDelta);
            this.recursiveDisplayRequestsDelta.defaultReturnValue(0);
        }

        Bundle scale(long targetTimes) {
            if (targetTimes == this.baseTimes) {
                return this;
            }

            double multiplier = (double) targetTimes / this.baseTimes;
            var scaled = new Bundle(targetTimes);

            for (var e : this.extracted) {
                long scaledAmount = (long) Math.ceil(e.getLongValue() * multiplier);
                scaled.extracted.add(e.getKey(), scaledAmount);
            }
            for (var e : this.containerItems) {
                long scaledAmount = (long) Math.ceil(e.getLongValue() * multiplier);
                scaled.containerItems.add(e.getKey(), scaledAmount);
            }
            for (var e : this.patternTimes.reference2LongEntrySet()) {
                long scaledTimes = (long) Math.ceil(e.getLongValue() * multiplier);
                scaled.patternTimes.put(e.getKey(), scaledTimes);
            }
            scaled.bytes = this.bytes * multiplier;

            for (var e : this.emittedItems) {
                long scaledAmount = (long) Math.ceil(e.getLongValue() * multiplier);
                scaled.emittedItems.add(e.getKey(), scaledAmount);
            }
            for (var e : this.missingItems) {
                long scaledAmount = (long) Math.ceil(e.getLongValue() * multiplier);
                scaled.missingItems.add(e.getKey(), scaledAmount);
            }
            for (var e : this.insertedItems) {
                long scaledAmount = (long) Math.ceil(e.getLongValue() * multiplier);
                scaled.insertedItems.add(e.getKey(), scaledAmount);
            }
            for (var e : this.pseudoItems) {
                long scaledAmount = (long) Math.ceil(e.getLongValue() * multiplier);
                scaled.pseudoItems.add(e.getKey(), scaledAmount);
            }
            scaled.intermediateFinalOutputAmount = (long) Math.ceil(this.intermediateFinalOutputAmount * multiplier);

            for (var e : this.recursiveMissingSeeds) {
                long scaledAmount = (long) Math.ceil(e.getLongValue() * multiplier);
                scaled.recursiveMissingSeeds.add(e.getKey(), scaledAmount);
            }
            for (var e : this.clearedRecursiveMissingSeeds) {
                long scaledAmount = (long) Math.ceil(e.getLongValue() * multiplier);
                scaled.clearedRecursiveMissingSeeds.add(e.getKey(), scaledAmount);
            }
            scaled.realSeededRecursiveRequests.addAll(this.realSeededRecursiveRequests);
            scaled.realRecursiveSeeds.addAll(this.realRecursiveSeeds);
            scaled.realSeededRecursiveKeys.addAll(this.realSeededRecursiveKeys);
            for (var e : this.recursiveDisplayRequestsDelta.reference2LongEntrySet()) {
                long scaledAmount = (long) Math.ceil(e.getLongValue() * multiplier);
                scaled.recursiveDisplayRequestsDelta.put(e.getKey(), scaledAmount);
            }

            return scaled;
        }

        private Bundle(long baseTimes) {
            this.extracted = new KeyCounter();
            this.containerItems = new KeyCounter();
            this.patternTimes = new Reference2LongOpenHashMap<>();
            this.patternTimes.defaultReturnValue(0);
            this.baseTimes = baseTimes;
            this.bytes = 0;

            // 初始化新增字段
            this.emittedItems = new KeyCounter();
            this.missingItems = new KeyCounter();
            this.insertedItems = new KeyCounter();
            this.pseudoItems = new KeyCounter();
            this.intermediateFinalOutputAmount = 0;

            this.recursiveMissingSeeds = new KeyCounter();
            this.clearedRecursiveMissingSeeds = new KeyCounter();
            this.realSeededRecursiveRequests = new ObjectOpenHashSet<>();
            this.realRecursiveSeeds = new ObjectOpenHashSet<>();
            this.realSeededRecursiveKeys = new ObjectOpenHashSet<>();
            this.recursiveDisplayRequestsDelta = new Reference2LongOpenHashMap<>();
            this.recursiveDisplayRequestsDelta.defaultReturnValue(0);
        }

        boolean isSatisfiable(CraftingSimulationState inv) {
            var testInv = new ChildCraftingSimulationState(inv);
            for (var e : extracted) {
                long available = testInv.extract(e.getKey(), e.getLongValue(), Actionable.MODULATE);
                if (available < e.getLongValue()) {
                    return false;
                }
            }
            return true;
        }
    }

    @Nullable
    MemoResult getMemoResult(MemoKey key) {
        return (MemoResult) this.memoCache.getIfPresent(key);
    }

    void putMemoResult(MemoKey key, MemoResult result) {
        this.memoCache.put(key, result);
    }

    static final class BundleKey {
        private final AEKey what;
        private final IPatternDetails pattern;
        private final int hashCode;

        BundleKey(AEKey what, IPatternDetails pattern) {
            this.what = what;
            this.pattern = pattern;
            // 使用 pattern 的内容 hash，而非 identity
            this.hashCode = what.hashCode() * 31 + pattern.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BundleKey other)) return false;
            // 使用 pattern 的 equals，而非引用相等
            return what.equals(other.what) && pattern.equals(other.pattern);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    @Nullable
    Bundle getBundle(BundleKey key) {
        return (Bundle) this.memoCache.getIfPresent(key);
    }

    void putBundle(BundleKey key, Bundle bundle) {
        this.memoCache.put(key, bundle);
    }

    private static final class ObjectPool<T> {
        private final ObjectArrayList<T> pool = new ObjectArrayList<>();
        private final Supplier<T> factory;

        ObjectPool(Supplier<T> factory, int initialCapacity) {
            this.factory = factory;
            for (int i = 0; i < initialCapacity; i++) {
                pool.add(factory.get());
            }
        }

        T acquire() {
            if (!pool.isEmpty()) {
                return pool.removeLast();
            }
            return factory.get();
        }

        void release(T obj) {
            pool.add(obj);
        }
    }

}
