package ae2.crafting.graph;

import ae2.api.config.Actionable;
import ae2.crafting.CraftingCalculation;
import ae2.crafting.inv.CraftingSimulationState;

import java.util.Collections;

public class GraphExecutor {
    private final CraftingCalculation calc;
    private final CraftingGraph graph;

    public GraphExecutor(CraftingCalculation calc, CraftingGraph graph) {
        this.calc = calc;
        this.graph = graph;
    }

    public void applyGraph(CraftingSimulationState inv) throws InterruptedException {
        var propagation = new DemandPropagation();
        var order = propagation.topologicalSort(graph);
        Collections.reverse(order);

        for (var node : order) {
            if (node.getCraftTimes() == 0) continue;
            applyNode(node, inv);
            calc.handlePausing();
        }
    }

    private void applyNode(CraftingGraphNode node, CraftingSimulationState inv) {
        long times = node.getCraftTimes();
        var pattern = node.getPattern();

        for (var edge : node.getInputs()) {
            long needed = edge.amountPerCraft() * times;
            long got = inv.extract(edge.inputKey(), needed, Actionable.MODULATE);

            long shortfall = needed - got;
            if (shortfall > 0) {
                var nodeKey = new CraftingGraph.NodeKey(edge.inputKey(), null);
                if (graph.isCyclicNode(nodeKey) || edge.producer() == null) {
                    calc.addMissing(edge.inputKey(), shortfall);
                }
            }
        }

        long outputAmount = node.getOutputPerCraft() * times;
        inv.insert(node.getWhat(), outputAmount, Actionable.MODULATE);

        inv.addCrafting(pattern, times);

        inv.addBytes(times * 8.0);
    }
}
