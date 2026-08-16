package ae2.crafting.graph;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEKey;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class CraftingGraph {
    private final Object2ObjectMap<NodeKey, CraftingGraphNode> nodes = new Object2ObjectOpenHashMap<>();
    private final ObjectList<CraftingGraphNode> nodesInCreationOrder = new ObjectArrayList<>();
    private final List<CraftingGraphNode> allNodes = ObjectLists.unmodifiable(nodesInCreationOrder);
    private final ObjectOpenHashSet<AEKey> cyclicKeys = new ObjectOpenHashSet<>();
    private CraftingGraphNode rootNode;
    private CraftingGraphTopology topology;
    private final Int2ObjectMap<LocalComponentPlan> localComponentPlans = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<SccPlan> sccPlans = new Int2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<CraftingGraphNode, LocalPatternPlan> localUnitPlans =
        new Reference2ObjectOpenHashMap<>();
    private Object2ObjectMap<AEKey, CraftingGraphNode> nodeByWhat;

    public record NodeKey(AEKey what, @Nullable IPatternDetails pattern) {
        @Override
        public int hashCode() {
            return what.hashCode() * 31 + Objects.hashCode(pattern);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NodeKey(AEKey what1, IPatternDetails pattern1))) return false;
            return what.equals(what1) && Objects.equals(pattern, pattern1);
        }
    }

    @Nullable
    public CraftingGraphNode getNode(NodeKey key) {
        return nodes.get(key);
    }

    public void putNode(NodeKey key, CraftingGraphNode node) {
        if (nodes.containsKey(key)) {
            return;
        }
        node.assignOrdinal(nodesInCreationOrder.size());
        nodes.put(key, node);
        nodesInCreationOrder.add(node);
        topology = null;
        cyclicKeys.clear();
        nodeByWhat = null;
    }

    public List<CraftingGraphNode> getAllNodes() {
        return allNodes;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getEdgeCount() {
        int count = 0;
        for (var node : nodesInCreationOrder) {
            count += node.getInputs().size();
        }
        return count;
    }

    @Nullable
    public CraftingGraphNode getRootNode() {
        return rootNode;
    }

    public void setRootNode(CraftingGraphNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Lazily computes and caches the graph order. Tarjan decomposition is only performed if the graph is not a DAG.
     * The graph must be fully built before this method is first called.
     */
    public CraftingGraphTopology getTopology() {
        if (topology == null) {
            topology = CraftingGraphTopology.analyze(this);
        }
        return topology;
    }

    /**
     * Returns the cached parent-before-dependency order used by demand propagation and, in reverse, execution.
     */
    public List<CraftingGraphNode> getTopologicalOrder() {
        return getTopology().getTopologicalOrder();
    }

    public boolean isCyclicNode(NodeKey key) {
        if (topology == null || !topology.isCondensed()) {
            return false;
        }
        if (key.pattern() != null) {
            var node = nodes.get(key);
            return node != null && topology.isCyclic(node);
        }
        if (cyclicKeys.isEmpty()) {
            for (var node : nodesInCreationOrder) {
                if (topology.isCyclic(node)) {
                    cyclicKeys.add(node.getWhat());
                }
            }
        }
        return cyclicKeys.contains(key.what());
    }

    public void setLocalComponentPlan(int componentId, LocalComponentPlan plan) {
        localComponentPlans.put(componentId, plan);
    }

    @Nullable
    public LocalComponentPlan getLocalComponentPlan(int componentId) {
        return localComponentPlans.get(componentId);
    }

    public void setSccPlan(int componentId, SccPlan plan) {
        if (plan.componentId() != componentId) {
            throw new IllegalArgumentException("SCC plan component id mismatch");
        }
        sccPlans.put(componentId, plan);
    }

    @Nullable
    public SccPlan getSccPlan(int componentId) {
        return sccPlans.get(componentId);
    }

    public void setLocalUnitPlan(CraftingGraphNode node, LocalPatternPlan plan) {
        localUnitPlans.put(node, plan);
    }

    @Nullable
    public LocalPatternPlan getLocalUnitPlan(CraftingGraphNode node) {
        return localUnitPlans.get(node);
    }

    @Nullable
    public CraftingGraphNode getNodeFor(AEKey what) {
        if (nodeByWhat == null) {
            nodeByWhat = new Object2ObjectOpenHashMap<>();
            for (var node : nodesInCreationOrder) {
                nodeByWhat.putIfAbsent(node.getWhat(), node);
            }
        }
        return nodeByWhat.get(what);
    }

    /**
     * Resets all per-attempt planning state so the graph structure can be reused by the next calculation. The topology
     * and creation order are structural and kept; every mutable demand/seed/component marker is cleared.
     */
    public void resetForReuse() {
        for (var node : nodesInCreationOrder) {
            node.resetPlanningAmounts();
            node.setLocalComponentId(-1);
            node.clearPlannedBoundaryDemands();
        }
        localComponentPlans.clear();
        sccPlans.clear();
        localUnitPlans.clear();
    }
}
