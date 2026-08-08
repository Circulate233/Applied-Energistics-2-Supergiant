package ae2.crafting.graph;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.ArrayDeque;
import java.util.List;

public class DemandPropagation {

    public void propagate(CraftingGraph graph) {
        var order = topologicalSort(graph);

        for (var node : order) {
            long times = divideCeil(node.getDemandAmount(), node.getOutputPerCraft());
            node.setCraftTimes(times);

            for (var edge : node.getInputs()) {
                if (edge.producer() != null) {
                    long childDemand = times * edge.amountPerCraft();
                    edge.producer().addDemandAmount(childDemand);
                }
            }
        }
    }

    public List<CraftingGraphNode> topologicalSort(CraftingGraph graph) {
        var inDegree = new Reference2IntOpenHashMap<CraftingGraphNode>();
        var queue = new ArrayDeque<CraftingGraphNode>();
        var result = new ObjectArrayList<CraftingGraphNode>();

        for (var node : graph.getAllNodes()) {
            int degree = node.getParents().size();
            inDegree.put(node, degree);
            if (degree == 0) {
                queue.add(node);
            }
        }

        while (!queue.isEmpty()) {
            var node = queue.poll();
            result.add(node);

            for (var child : node.getInputs()) {
                if (child.producer() != null) {
                    int newDegree = inDegree.getInt(child.producer()) - 1;
                    inDegree.put(child.producer(), newDegree);
                    if (newDegree == 0) {
                        queue.add(child.producer());
                    }
                }
            }
        }

        if (result.size() < graph.getNodeCount()) {
            for (var node : graph.getAllNodes()) {
                if (!result.contains(node)) {
                    var key = new CraftingGraph.NodeKey(node.getWhat(), node.getPattern());
                    graph.addCyclicNode(key);
                }
            }
        }

        return result;
    }

    private static long divideCeil(long dividend, long divisor) {
        if (divisor == 0) return 0;
        return (dividend + divisor - 1) / divisor;
    }
}
