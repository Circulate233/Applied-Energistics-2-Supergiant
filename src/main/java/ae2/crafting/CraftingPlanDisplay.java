package ae2.crafting;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.crafting.graph.CraftingGraphDisplaySnapshot;
import ae2.integration.data.CraftingGraphDisplayTreeConverter;
import ae2.integration.data.LiteCraftTreeNode;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
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
    private final @Nullable CraftingGraphDisplaySnapshot graphSnapshot;
    private final AtomicReference<CompletableFuture<LiteCraftTreeNode>> tree = new AtomicReference<>();

    public CraftingPlanDisplay(GenericStack output, Object2LongMap<IPatternDetails> patternTimes,
                               KeyCounter usedItems, KeyCounter missingItems,
                               @Nullable CraftingTreeNode legacyTree,
                               @Nullable CraftingGraphDisplaySnapshot graphSnapshot) {
        this.output = output;
        this.patternTimes = patternTimes;
        this.usedItems = usedItems;
        this.missingItems = missingItems;
        this.legacyTree = legacyTree;
        this.graphSnapshot = graphSnapshot;
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
        if (graphSnapshot != null) {
            return CraftingGraphDisplayTreeConverter.toLiteTree(graphSnapshot, output.amount());
        }
        if (legacyTree != null) {
            return LiteCraftTreeNode.of(legacyTree, null, output.amount(),
                new LiteCraftTreeNode.PatternTimesAllocator(patternTimes, usedItems, missingItems));
        }
        throw new IllegalStateException("Crafting plan has no display source");
    }
}
