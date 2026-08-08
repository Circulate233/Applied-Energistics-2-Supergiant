package ae2.crafting.graph;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEKey;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public class CraftingGraph {
    private final Object2ObjectMap<NodeKey, CraftingGraphNode> nodes = new Object2ObjectOpenHashMap<>();
    private final Set<NodeKey> cyclicNodes = new ObjectOpenHashSet<>();
    private CraftingGraphNode rootNode;

    public record NodeKey(AEKey what, @Nullable IPatternDetails pattern) {
        @Override
        public int hashCode() {
            return what.hashCode() * 31 + Objects.hashCode(pattern);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NodeKey other)) return false;
            return what.equals(other.what) && Objects.equals(pattern, other.pattern);
        }
    }

    @Nullable
    public CraftingGraphNode getNode(NodeKey key) {
        return nodes.get(key);
    }

    public void putNode(NodeKey key, CraftingGraphNode node) {
        nodes.put(key, node);
    }

    public Collection<CraftingGraphNode> getAllNodes() {
        return nodes.values();
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getEdgeCount() {
        int count = 0;
        for (var node : nodes.values()) {
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

    public void addCyclicNode(NodeKey key) {
        cyclicNodes.add(key);
    }

    public boolean isCyclicNode(NodeKey key) {
        return cyclicNodes.contains(key);
    }
}
