package ae2.crafting.graph;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.crafting.CraftingCalculation;
import ae2.helpers.patternprovider.PseudoPatternDetails;
import com.google.common.math.LongMath;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.List;
import java.util.Set;

public class GraphBuilder {
    private final CraftingCalculation calc;
    private long patternLookupNanos;
    private long emitLookupNanos;
    private long nodeCount;

    public GraphBuilder(CraftingCalculation calc) {
        this.calc = calc;
    }

    public CraftingGraph buildGraph(AEKey output, long requestedAmount) throws InterruptedException {
        long start = System.nanoTime();
        this.patternLookupNanos = 0;
        this.emitLookupNanos = 0;
        this.nodeCount = 0;
        var graph = new CraftingGraph();
        var requestStack = new ObjectOpenHashSet<AEKey>();

        var rootNode = buildNodeRecursive(output, graph, requestStack, true);
        if (rootNode == null) throw new IllegalStateException("Graph root was not created");
        graph.setRootNode(rootNode);
        rootNode.setDemandAmount(requestedAmount);

        long total = System.nanoTime() - start;
        calc.recordPerformanceCount("buildGraphNodes", nodeCount);
        calc.recordPerformanceStage("buildGraphPatternLookup", patternLookupNanos);
        calc.recordPerformanceStage("buildGraphEmitLookup", emitLookupNanos);
        calc.recordPerformanceStage("buildGraphStructure",
            Math.max(0, total - patternLookupNanos - emitLookupNanos));
        return graph;
    }

    private CraftingGraphNode buildNodeRecursive(
        AEKey what,
        CraftingGraph graph,
        Set<AEKey> requestStack,
        boolean root
    ) throws InterruptedException {
        long emitStart = System.nanoTime();
        boolean canEmit = calc.canEmitFor(what);
        this.emitLookupNanos += System.nanoTime() - emitStart;
        if (canEmit) {
            var nodeKey = new CraftingGraph.NodeKey(what, null);
            var existing = graph.getNode(nodeKey);
            if (existing != null) {
                return existing;
            }
            var emitter = new CraftingGraphNode(what, null, List.of(new GenericStack(what, 1)), 1);
            graph.putNode(nodeKey, emitter);
            this.nodeCount++;
            return emitter;
        }

        long patternStart = System.nanoTime();
        var patterns = calc.getCraftingFor(what);
        this.patternLookupNanos += System.nanoTime() - patternStart;
        if (patterns.isEmpty()) {
            if (root) {
                var nodeKey = new CraftingGraph.NodeKey(what, null);
                var leaf = new CraftingGraphNode(what, null, List.of(), 1, false, true);
                graph.putNode(nodeKey, leaf);
                this.nodeCount++;
                return leaf;
            }
            return null;
        }
        boolean localUnit = patterns.size() != 1;
        String localReason = localUnit ? "multiple-pattern-candidates" : null;

        var primaryPattern = patterns.getFirst();
        var nodeKey = new CraftingGraph.NodeKey(what, primaryPattern);

        var existing = graph.getNode(nodeKey);
        if (existing != null) {
            return existing;
        }

        if (requestStack.contains(what)) {
            return graph.getNode(nodeKey);
        }

        long outputAmount = getRequestedOutputAmount(what, primaryPattern);
        if (outputAmount <= 0) {
            throw invalidPattern("mismatched pattern outputs");
        }
        if (!localUnit && !isNativePattern(primaryPattern)) {
            localUnit = true;
            localReason = "unsupported-pattern-semantics";
        }
        var node = new CraftingGraphNode(what, primaryPattern, patterns, primaryPattern.getOutputs(), outputAmount,
            localUnit, false, localReason);
        graph.putNode(nodeKey, node);
        this.nodeCount++;

        requestStack.add(what);
        calc.handlePausing();

        var graphInputs = new Object2LongLinkedOpenHashMap<AEKey>();
        var patternsToScan = localUnit ? patterns : List.of(primaryPattern);
        for (var pattern : patternsToScan) {
            var patternInputs = new Object2LongLinkedOpenHashMap<AEKey>();
            for (var inputEntry : pattern.getInputs()) {
                var possibleInputs = inputEntry.possibleInputs();
                if (possibleInputs.length == 0 || inputEntry.getMultiplier() <= 0) continue;
                int possibleInputIndex = 0;
                while (possibleInputIndex < possibleInputs.length) {
                    var possible = possibleInputs[possibleInputIndex++];
                    if (possible.amount() > 0 && (!possible.what().equals(what) || !localUnit)) {
                        long amount = LongMath.saturatedMultiply(possible.amount(), inputEntry.getMultiplier());
                        patternInputs.put(possible.what(),
                            LongMath.saturatedAdd(patternInputs.getLong(possible.what()), amount));
                    }
                }
            }
            // A local unit is previewed with one selected pattern. Keep the largest per-key demand
            // across candidates, while still summing repeated slots within the same candidate.
            for (var input : patternInputs.object2LongEntrySet()) {
                graphInputs.put(input.getKey(), Math.max(graphInputs.getLong(input.getKey()), input.getLongValue()));
            }
        }
        for (var graphInput : graphInputs.object2LongEntrySet()) {
            var inputKey = graphInput.getKey();
            long inputAmount = graphInput.getLongValue();

            var childNode = buildNodeRecursive(inputKey, graph, requestStack, false);

            var edge = new CraftingGraphEdge(inputKey, inputAmount, childNode,
                PseudoPatternDetails.isPseudo(primaryPattern));
            node.addInput(edge);

            if (childNode != null) {
                childNode.addParent(node);
            }
        }

        requestStack.remove(what);
        return node;
    }

    private static long getRequestedOutputAmount(AEKey what, IPatternDetails pattern) {
        var outputs = pattern.getOutputs();
        long requestedOutputAmount = 0;
        for (var output : outputs) {
            if (output.amount() <= 0) {
                throw invalidPattern("empty pattern output");
            }
            if (output.what().equals(what)) {
                requestedOutputAmount = LongMath.saturatedAdd(requestedOutputAmount, output.amount());
            }
        }
        if (requestedOutputAmount <= 0) {
            throw invalidPattern("mismatched pattern outputs");
        }
        return requestedOutputAmount;
    }

    private static boolean isNativePattern(IPatternDetails pattern) {
        if (PseudoPatternDetails.isPseudo(pattern)) return true;
        if (pattern.getOutputs().size() != 1) return false;
        for (var input : pattern.getInputs()) {
            var possible = input.possibleInputs();
            if (possible.length != 1 || possible[0].amount() <= 0 || input.getMultiplier() <= 0
                || input.getRemainingKey(possible[0].what()) != null) {
                return false;
            }
        }
        return true;
    }

    private static IllegalArgumentException invalidPattern(String reason) {
        return new IllegalArgumentException("Invalid graph crafting pattern: " + reason);
    }
}
