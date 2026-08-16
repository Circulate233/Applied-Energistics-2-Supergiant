package ae2.crafting.graph;

import ae2.crafting.CraftingCalculation;
import ae2.api.stacks.KeyCounter;
import com.google.common.math.LongMath;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

public class DemandPropagation {
    private final CraftingCalculation calculation;

    public DemandPropagation(CraftingCalculation calculation) {
        this.calculation = calculation;
    }

    public void propagate(CraftingGraph graph) throws InterruptedException {
        var order = graph.getTopologicalOrder();
        var topology = graph.getTopology();
        var solvedComponents = new IntOpenHashSet();

        for (var node : order) {
            calculation.handlePausing();
            long times;
            if (topology.isCyclic(node)) {
                int componentId = topology.getComponentId(node);
                if (solvedComponents.add(componentId)) {
                    var component = topology.getComponents().get(componentId);
                    solveCyclicComponent(graph, topology, component);
                    propagateLocalComponentBoundary(graph, topology, component);
                }
                times = node.getCraftTimes();
            } else {
                times = divideCeil(node.getDemandAmount(), node.getOutputPerCraft());
                node.setCraftTimes(times);
            }

            for (var edge : node.getInputs()) {
                if (node.getLocalComponentId() >= 0) continue;
                if (node.isLocalUnit() && node.getPlannedBoundaryDemands() != null) continue;
                if (!edge.cycleCut() && edge.producer() != null) {
                    edge.producer().addDemandAmount(LongMath.saturatedMultiply(times, edge.amountPerCraft()));
                }
            }
            propagateLocalUnitBoundary(node);
        }
    }

    private void solveCyclicComponent(CraftingGraph graph, CraftingGraphTopology topology,
                                      CraftingGraphTopology.Component component) {
        // Multi-pattern components cannot be solved from graph edge weights alone: expanded legacy closure can select
        // patterns and account for remainders that are intentionally absent from the graph. Keep that transaction local.
        if (component.nodes().size() != 1) {
            markLocalComponent(graph, component);
            return;
        }

        var node = component.nodes().getFirst();
        var pattern = node.getPattern();
        if (pattern == null || node.isLocalUnit()) {
            markLocalComponent(graph, component);
            return;
        }

        CraftingGraphEdge feedback = null;
        for (var edge : node.getInputs()) {
            if (edge.producer() != null && topology.getComponentId(edge.producer()) == component.id()) {
                if (feedback != null || !edge.inputKey().equals(node.getWhat())) {
                    markLocalComponent(graph, component);
                    return;
                }
                feedback = edge;
            }
        }
        if (feedback == null) {
            markLocalComponent(graph, component);
            return;
        }

        long demand = Math.max(0, node.getDemandAmount());
        long directNet = LongMath.saturatedSubtract(node.getOutputPerCraft(), feedback.amountPerCraft());
        long expandedNet = calculation.getExpandedPatternNetOutput(pattern, node.getWhat());
        var batch = calculation.getRecursivePatternBatch(pattern, node.getWhat());

        if (demand == 0) {
            node.setCraftTimes(0);
            feedback.markCycleCut(0);
            feedback.setReserveDemand(Math.max(0, directNet));
            graph.setSccPlan(component.id(), nativePlan(component, node, feedback, 0, 0, 0, Math.max(0, directNet)));
            return;
        }

        // A closure without any positive net output can never grow the inventory by crafting: only the real seed
        // inventory can satisfy the demand. Serve it natively as a seed-only plan instead of falling back to a legacy
        // transaction; execution records the actually allocated/missing seed amounts.
        if (expandedNet <= 0 && !calculation.hasPositiveRecursiveNet(pattern)) {
            node.setCraftTimes(0);
            feedback.markCycleCut(demand);
            long reserve = Math.max(1, feedback.amountPerCraft());
            feedback.setReserveDemand(reserve);
            graph.setSccPlan(component.id(), nativePlan(component, node, feedback, demand, 0, 0, reserve));
            return;
        }

        if (expandedNet != directNet || batch.rootTimes() != 1 || batch.netOutput() != directNet) {
            // Zero/negative-net recursion and expanded closures that differ from the single-pattern self-loop cannot
            // be expressed by the native closed form. The component-local legacy transaction records any seed
            // shortfall explicitly instead of silently treating it as fulfilled.
            markLocalComponent(graph, component);
            return;
        }

        // From here on the direct net is positive: non-positive nets never pass the expanded/batch guard above,
        // because getRecursivePatternBatch falls back to a positive direct output for them.
        long seedCapacity = feedback.amountPerCraft();
        long seedRequired = Math.min(demand, seedCapacity);
        long batches = divideCeil(demand - seedRequired, directNet);
        long plannedOutput = batches == 0 ? 0 : LongMath.saturatedAdd(seedRequired,
            LongMath.saturatedMultiply(batches, directNet));
        node.setCraftTimes(batches);
        feedback.markCycleCut(seedRequired);
        feedback.setReserveDemand(directNet);
        graph.setSccPlan(component.id(), nativePlan(component, node, feedback, seedRequired, batches, plannedOutput,
            directNet));
    }

    private static SccPlan nativePlan(CraftingGraphTopology.Component component, CraftingGraphNode node,
                                      CraftingGraphEdge feedback, long seedRequired, long craftTimes,
                                      long plannedOutput, long reserve) {
        var craftTimesByNode = new Reference2LongOpenHashMap<CraftingGraphNode>();
        craftTimesByNode.put(node, craftTimes);
        var plannedOutputByNode = new Reference2LongOpenHashMap<CraftingGraphNode>();
        plannedOutputByNode.put(node, plannedOutput);
        var seeds = new Reference2ObjectOpenHashMap<CraftingGraphEdge, SccPlan.SeedLedger>();
        seeds.put(feedback, new SccPlan.SeedLedger(seedRequired, 0, 0, reserve));
        return SccPlan.nativePlan(component.id(), component.nodes(), craftTimesByNode, plannedOutputByNode, seeds);
    }

    private static void markLocalComponent(CraftingGraph graph, CraftingGraphTopology.Component component) {
        for (var node : component.nodes()) {
            node.setLocalComponentId(component.id());
            node.setCraftTimes(0);
        }
        var sccPlan = graph.getSccPlan(component.id());
        if (sccPlan == null) {
            var localPlan = graph.getLocalComponentPlan(component.id());
            sccPlan = SccPlan.localPlan(component.id(), component.nodes(),
                localPlan == null ? new KeyCounter() : localPlan.boundaryDemands());
            graph.setSccPlan(component.id(), sccPlan);
        }
        sccPlan.restorePlanning();
    }

    private static void propagateLocalComponentBoundary(CraftingGraph graph, CraftingGraphTopology topology,
                                                        CraftingGraphTopology.Component component) {
        var plan = graph.getLocalComponentPlan(component.id());
        if (plan == null) return;
        var sccPlan = graph.getSccPlan(component.id());
        var boundaryDemands = sccPlan == null ? plan.boundaryDemands() : sccPlan.boundaryDemands();
        for (var boundary : boundaryDemands) {
            long remaining = boundary.getLongValue();
            for (var node : component.nodes()) {
                for (var edge : node.getInputs()) {
                    if (edge.inputKey().equals(boundary.getKey()) && edge.producer() != null
                        && topology.getComponentId(edge.producer()) != component.id()) {
                        edge.producer().addDemandAmount(remaining);
                        remaining = 0;
                        break;
                    }
                }
                if (remaining == 0) break;
            }
        }
    }

    private static void propagateLocalUnitBoundary(CraftingGraphNode node) {
        if (!node.isLocalUnit()) return;
        var planned = node.getPlannedBoundaryDemands();
        if (planned == null) return;
        for (var boundary : planned) {
            for (var edge : node.getInputs()) {
                if (edge.inputKey().equals(boundary.getKey()) && edge.producer() != null) {
                    edge.producer().addDemandAmount(boundary.getLongValue());
                    break;
                }
            }
        }
    }

    private static long divideCeil(long dividend, long divisor) {
        if (dividend <= 0 || divisor <= 0) return 0;
        return 1 + (dividend - 1) / divisor;
    }
}
