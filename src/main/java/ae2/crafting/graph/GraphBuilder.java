package ae2.crafting.graph;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEKey;
import ae2.crafting.CraftingCalculation;
import ae2.helpers.patternprovider.PseudoPatternDetails;
import com.google.common.math.LongMath;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Set;

public class GraphBuilder {
    private final CraftingCalculation calc;

    public GraphBuilder(CraftingCalculation calc) {
        this.calc = calc;
    }

    public CraftingGraph buildGraph(AEKey output, long requestedAmount) throws InterruptedException {
        var graph = new CraftingGraph();
        var requestStack = new ObjectOpenHashSet<AEKey>();
        var checkedEmitters = new ObjectOpenHashSet<AEKey>();

        var rootNode = buildNodeRecursive(output, graph, requestStack, checkedEmitters, true);
        if (rootNode == null) {
            throw unsupported("root has no crafting pattern");
        }
        graph.setRootNode(rootNode);
        rootNode.setDemandAmount(requestedAmount);

        return graph;
    }

    private CraftingGraphNode buildNodeRecursive(
        AEKey what,
        CraftingGraph graph,
        Set<AEKey> requestStack,
        Set<AEKey> checkedEmitters,
        boolean root
    ) throws InterruptedException {
        if (requestStack.contains(what)) {
            throw unsupported("recursive crafting dependency");
        }
        if (checkedEmitters.add(what) && calc.canEmitFor(what)) {
            throw unsupported("crafting emitter input");
        }

        var patterns = calc.getCraftingFor(what);
        if (patterns.isEmpty()) {
            if (root) {
                throw unsupported("root has no crafting pattern");
            }
            return null;
        }
        if (patterns.size() != 1) {
            throw unsupported("multiple crafting patterns for one output");
        }

        var primaryPattern = patterns.getFirst();
        var nodeKey = new CraftingGraph.NodeKey(what, primaryPattern);

        var existing = graph.getNode(nodeKey);
        if (existing != null) {
            return existing;
        }

        validatePattern(what, primaryPattern);
        long outputAmount = primaryPattern.getPrimaryOutput().amount();
        var node = new CraftingGraphNode(what, primaryPattern, outputAmount);
        graph.putNode(nodeKey, node);

        requestStack.add(what);
        calc.handlePausing();

        for (var inputEntry : primaryPattern.getInputs()) {
            var possible = inputEntry.possibleInputs()[0];
            var inputKey = possible.what();
            long inputAmount = LongMath.saturatedMultiply(possible.amount(), inputEntry.getMultiplier());

            var childNode = buildNodeRecursive(inputKey, graph, requestStack, checkedEmitters, false);

            var edge = new CraftingGraphEdge(inputKey, inputAmount, childNode);
            node.addInput(edge);

            if (childNode != null) {
                childNode.addParent(node);
            }
        }

        requestStack.remove(what);
        return node;
    }

    static void validatePattern(AEKey what, IPatternDetails pattern) {
        if (PseudoPatternDetails.isPseudo(pattern)) {
            throw unsupported("pseudo crafting pattern");
        }
        var outputs = pattern.getOutputs();
        if (outputs.size() != 1 || outputs.getFirst().amount() <= 0 || !outputs.getFirst().what().equals(what)) {
            throw unsupported("multiple, empty, or mismatched pattern outputs");
        }
        for (var input : pattern.getInputs()) {
            var possibleInputs = input.possibleInputs();
            if (possibleInputs.length != 1 || possibleInputs[0].amount() <= 0) {
                throw unsupported("substitute or empty pattern input");
            }
            if (input.getMultiplier() <= 0) {
                throw unsupported("non-positive pattern input multiplier");
            }
            if (input.getRemainingKey(possibleInputs[0].what()) != null) {
                throw unsupported("pattern input remainder");
            }
        }
    }

    private static IllegalArgumentException unsupported(String reason) {
        return new IllegalArgumentException("Graph crafting does not support " + reason);
    }
}
