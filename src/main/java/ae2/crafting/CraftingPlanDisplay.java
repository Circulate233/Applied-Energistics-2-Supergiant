package ae2.crafting;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.crafting.graph.CraftingGraphDisplaySnapshot;
import ae2.crafting.graph.LocalDisplayFragment;
import ae2.integration.data.CraftingGraphDisplayTreeConverter;
import ae2.integration.data.LiteCraftTreeNode;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class CraftingPlanDisplay {
    private static final AtomicInteger THREAD_IDS = new AtomicInteger();
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
        1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8), task -> {
        var thread = new Thread(task, "AE2 Crafting Tree Display " + THREAD_IDS.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.AbortPolicy());

    private final GenericStack output;
    private final Object2LongMap<IPatternDetails> patternTimes;
    private final KeyCounter usedItems;
    private final KeyCounter missingItems;
    private final @Nullable CraftingTreeNode legacyTree;
    private final @Nullable CraftingGraphDisplaySnapshot.Builder graphDisplayBuilder;
    private final CraftingAttemptMetrics attemptMetrics;
    private final @Nullable CraftingCalculation calculation;
    private final AtomicReference<CompletableFuture<LiteCraftTreeNode>> tree = new AtomicReference<>();

    public CraftingPlanDisplay(GenericStack output, Object2LongMap<IPatternDetails> patternTimes,
                               KeyCounter usedItems, KeyCounter missingItems,
                               @Nullable CraftingTreeNode legacyTree,
                               @Nullable CraftingGraphDisplaySnapshot.Builder graphDisplayBuilder,
                               CraftingAttemptMetrics attemptMetrics,
                               @Nullable CraftingCalculation calculation) {
        this.output = output;
        this.patternTimes = patternTimes;
        this.usedItems = usedItems;
        this.missingItems = missingItems;
        this.legacyTree = legacyTree;
        this.graphDisplayBuilder = graphDisplayBuilder;
        this.attemptMetrics = attemptMetrics;
        this.calculation = calculation;
    }

    public CompletableFuture<LiteCraftTreeNode> requestTree() {
        var current = tree.get();
        if (current != null) return current;

        var created = new CompletableFuture<LiteCraftTreeNode>();
        if (!tree.compareAndSet(null, created)) return tree.get();
        try {
            EXECUTOR.execute(() -> {
                try {
                    created.complete(materialize());
                } catch (RuntimeException failure) {
                    created.completeExceptionally(failure);
                }
            });
        } catch (RuntimeException failure) {
            created.completeExceptionally(failure);
        }
        return created;
    }

    private LiteCraftTreeNode materialize() {
        long start = System.nanoTime();
        try {
            var builder = this.graphDisplayBuilder;
            if (builder != null) {
                // Build the immutable graph snapshot lazily on first GUI open, then hydrate machine info.
                return CraftingGraphDisplayTreeConverter.toLiteTree(hydrateMachineInfo(builder.build()),
                    output.amount(), missingItems);
            }
            if (legacyTree != null) {
                return LiteCraftTreeNode.of(legacyTree, null, output.amount(),
                    new LiteCraftTreeNode.PatternTimesAllocator(patternTimes, usedItems, missingItems));
            }
            throw new IllegalStateException("Crafting plan has no display source");
        } finally {
            attemptMetrics.recordDisplayMaterialization(System.nanoTime() - start);
        }
    }

    private CraftingGraphDisplaySnapshot hydrateMachineInfo(CraftingGraphDisplaySnapshot snapshot) {
        var calculation = this.calculation;
        if (calculation == null) {
            return snapshot;
        }
        var nodes = new ObjectArrayList<CraftingGraphDisplaySnapshot.Node>(snapshot.nodes().size());
        for (var node : snapshot.nodes()) {
            var machines = node.machines();
            var locations = node.machineLocations();
            if (node.executed() && node.pattern() != null && machines.isEmpty()) {
                var info = calculation.getMachineInfo(node.pattern());
                machines = info.groups();
                locations = info.locations();
            }
            nodes.add(new CraftingGraphDisplaySnapshot.Node(node.what(), node.pattern(), node.outputPerCraft(),
                node.demandAmount(), node.craftTimes(), node.executed(), node.inputs(), machines, locations,
                hydrateMachineInfo(node.localDisplay(), calculation), node.external(), node.missing(),
                node.componentId(), node.componentOrder()));
        }
        return new CraftingGraphDisplaySnapshot(snapshot.rootWhat(), snapshot.requestedAmount(), nodes);
    }

    private static @Nullable LocalDisplayFragment hydrateMachineInfo(@Nullable LocalDisplayFragment fragment,
                                                                     CraftingCalculation calculation) {
        if (fragment == null) {
            return null;
        }
        var processes = new ObjectArrayList<LocalDisplayFragment.Process>(fragment.processes().size());
        for (var process : fragment.processes()) {
            var machines = process.machines();
            var locations = process.locations();
            if (process.pattern() != null && machines.isEmpty()) {
                var info = calculation.getMachineInfo(process.pattern());
                machines = info.groups();
                locations = info.locations();
            }
            var inputs = new ObjectArrayList<LocalDisplayFragment>(process.inputs().size());
            for (var input : process.inputs()) {
                inputs.add(hydrateMachineInfo(input, calculation));
            }
            processes.add(new LocalDisplayFragment.Process(process.times(), process.pattern(), machines, locations,
                inputs));
        }
        return new LocalDisplayFragment(fragment.output(), fragment.amount(), fragment.missing(), processes);
    }
}
