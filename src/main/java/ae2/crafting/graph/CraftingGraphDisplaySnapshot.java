package ae2.crafting.graph;

import ae2.api.crafting.IPatternDetails;
import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.stacks.AEKey;
import ae2.crafting.CraftingTreeProcess;
import ae2.crafting.execution.CraftingSupplierLocation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable graph execution decisions with deterministic display ownership for produced output.
 */
public record CraftingGraphDisplaySnapshot(AEKey rootWhat, long requestedAmount, List<Node> nodes) {
    public CraftingGraphDisplaySnapshot {
        nodes = List.copyOf(nodes);
    }

    public record Node(AEKey what, @Nullable IPatternDetails pattern, long outputPerCraft,
                       long demandAmount, long craftTimes, boolean executed, List<Edge> inputs,
                       List<PatternContainerGroup> machines,
                       Map<PatternContainerGroup, List<CraftingSupplierLocation>> machineLocations) {
        public Node {
            inputs = List.copyOf(inputs);
            var copiedMachines = new ArrayList<PatternContainerGroup>(machines.size());
            var copiedLocations = new LinkedHashMap<PatternContainerGroup, List<CraftingSupplierLocation>>();
            for (var machine : machines) {
                var copiedTooltip = machine.tooltip().stream().map(ITextComponent::createCopy).collect(ObjectArrayList.toList());
                var copiedMachine = new PatternContainerGroup(machine.icon(), machine.name().createCopy(), copiedTooltip);
                copiedMachines.add(copiedMachine);
                var locations = machineLocations.get(machine);
                if (locations != null && !locations.isEmpty()) {
                    copiedLocations.put(copiedMachine, List.copyOf(locations));
                }
            }
            machines = List.copyOf(copiedMachines);
            machineLocations = Collections.unmodifiableMap(copiedLocations);
        }

        public Node(AEKey what, @Nullable IPatternDetails pattern, long outputPerCraft,
                    long demandAmount, long craftTimes, boolean executed, List<Edge> inputs) {
            this(what, pattern, outputPerCraft, demandAmount, craftTimes, executed, inputs, List.of(), Map.of());
        }
    }

    public record Edge(AEKey inputKey, long amountPerCraft, @Nullable Integer producerIndex,
                       boolean cycleCut, long needed, long allocatedOutput,
                       long extractedExternal, long missing) {
    }

    static Builder builder(CraftingGraph graph) {
        return new Builder(graph);
    }

    static final class Builder {
        private final CraftingGraph graph;
        private final List<CraftingGraphNode> orderedNodes = new ArrayList<>();
        private final Map<CraftingGraphNode, Integer> indexes = new IdentityHashMap<>();
        private final Map<CraftingGraphNode, Long> remainingOutput = new IdentityHashMap<>();
        private final Map<CraftingGraphNode, Boolean> executed = new IdentityHashMap<>();
        private final Map<CraftingGraphNode, CraftingTreeProcess.MachineInfo> machineInfo = new IdentityHashMap<>();
        private final Map<CraftingGraphEdge, EdgeResult> edgeResults = new IdentityHashMap<>();

        private Builder(CraftingGraph graph) {
            this.graph = graph;
            var root = graph.getRootNode();
            if (root == null) {
                throw new IllegalStateException("Graph has no root node");
            }
            collect(root);
        }

        void recordNode(CraftingGraphNode node) {
            executed.put(node, true);
        }

        void recordNode(CraftingGraphNode node, CraftingTreeProcess.MachineInfo info) {
            recordNode(node);
            machineInfo.put(node, info);
        }

        void recordEdge(CraftingGraphEdge edge, long needed, long extracted) {
            long allocated = consume(edge.producer(), extracted);
            edgeResults.put(edge, new EdgeResult(needed, allocated, extracted - allocated, needed - extracted));
        }

        void recordOutput(CraftingGraphNode node, long amount) {
            if (amount > 0) {
                remainingOutput.put(node, amount);
            }
        }

        CraftingGraphDisplaySnapshot build(long requestedAmount) {
            var snapshots = new ArrayList<Node>(orderedNodes.size());
            for (var node : orderedNodes) {
                var edges = new ArrayList<Edge>(node.getInputs().size());
                for (var edge : node.getInputs()) {
                    var result = edgeResults.getOrDefault(edge, new EdgeResult(0, 0, 0, 0));
                    var producer = edge.producer();
                    edges.add(new Edge(edge.inputKey(), edge.amountPerCraft(),
                        producer == null ? null : indexes.get(producer),
                        producer == null && graph.isCyclicNode(new CraftingGraph.NodeKey(edge.inputKey(), null)),
                        result.needed, result.allocated, result.external, result.missing));
                }
                var info = machineInfo.get(node);
                snapshots.add(new Node(node.getWhat(), node.getPattern(), node.getOutputPerCraft(),
                    node.getDemandAmount(), node.getCraftTimes(), executed.containsKey(node), edges,
                    info == null ? List.of() : info.groups(), info == null ? Map.of() : info.locations()));
            }
            return new CraftingGraphDisplaySnapshot(orderedNodes.getFirst().getWhat(), requestedAmount, snapshots);
        }

        private long consume(@Nullable CraftingGraphNode producer, long amount) {
            if (producer == null || amount <= 0) return 0;
            long available = remainingOutput.getOrDefault(producer, 0L);
            long allocated = Math.min(available, amount);
            if (allocated == available) {
                remainingOutput.remove(producer);
            } else {
                remainingOutput.put(producer, available - allocated);
            }
            return allocated;
        }

        private void collect(CraftingGraphNode node) {
            if (indexes.containsKey(node)) return;
            indexes.put(node, orderedNodes.size());
            orderedNodes.add(node);
            for (var edge : node.getInputs()) {
                if (edge.producer() != null) collect(edge.producer());
            }
        }

        private record EdgeResult(long needed, long allocated, long external, long missing) {
        }
    }
}
