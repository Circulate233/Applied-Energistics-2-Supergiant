package ae2.crafting.graph;

import ae2.api.stacks.AEKey;
import ae2.crafting.CraftingCalculation;
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

        var rootNode = buildNodeRecursive(output, graph, requestStack);
        if (rootNode != null) {
            graph.setRootNode(rootNode);
            rootNode.setDemandAmount(requestedAmount);
        }

        return graph;
    }

    private CraftingGraphNode buildNodeRecursive(
        AEKey what,
        CraftingGraph graph,
        Set<AEKey> requestStack
    ) throws InterruptedException {
        if (requestStack.contains(what)) {
            if (calc.cycleHasNetOutput(what)) {
                graph.addCyclicNode(new CraftingGraph.NodeKey(what, null));
            }
            return null;
        }

        var patterns = calc.getCraftingFor(what);
        if (patterns.isEmpty()) {
            return null;
        }

        var primaryPattern = patterns.get(0);
        var nodeKey = new CraftingGraph.NodeKey(what, primaryPattern);

        var existing = graph.getNode(nodeKey);
        if (existing != null) {
            return existing;
        }

        long outputAmount = primaryPattern.getPrimaryOutput().amount();
        var node = new CraftingGraphNode(what, primaryPattern, outputAmount);
        graph.putNode(nodeKey, node);

        requestStack.add(what);
        calc.handlePausing();

        for (var inputEntry : primaryPattern.getInputs()) {
            for (var possible : inputEntry.possibleInputs()) {
                var inputKey = possible.what();
                long inputAmount = possible.amount();

                var childNode = buildNodeRecursive(inputKey, graph, requestStack);

                var edge = new CraftingGraphEdge(inputKey, inputAmount, childNode);
                node.addInput(edge);

                if (childNode != null) {
                    childNode.addParent(node);
                }
            }
        }

        requestStack.remove(what);
        return node;
    }
}
