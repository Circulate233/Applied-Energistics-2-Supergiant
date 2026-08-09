package ae2.integration.data;

import ae2.api.stacks.GenericStack;
import ae2.crafting.graph.CraftingGraphDisplaySnapshot;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Converts immutable graph display ownership into the existing display tree DTO.
 */
public final class CraftingGraphDisplayTreeConverter {
    private CraftingGraphDisplayTreeConverter() {
    }

    public static LiteCraftTreeNode toLiteTree(CraftingGraphDisplaySnapshot snapshot, long finalOutputAmount) {
        if (snapshot.nodes().isEmpty()) {
            throw new IllegalArgumentException("Graph display snapshot has no nodes");
        }
        return materialize(snapshot, 0, null, finalOutputAmount,
            new BitSet(snapshot.nodes().size()), new BitSet(snapshot.nodes().size()));
    }

    private static LiteCraftTreeNode materialize(CraftingGraphDisplaySnapshot snapshot, int nodeIndex,
                                                 LiteCraftTreeProc parent, long amount,
                                                 BitSet path, BitSet materializedProcesses) {
        if (nodeIndex < 0 || nodeIndex >= snapshot.nodes().size()) {
            throw new IllegalArgumentException("Graph display node index out of range: " + nodeIndex);
        }
        if (path.get(nodeIndex)) {
            throw new IllegalArgumentException("Graph display snapshot cycle at node " + nodeIndex);
        }

        var node = snapshot.nodes().get(nodeIndex);
        if (!node.executed() || materializedProcesses.get(nodeIndex)) {
            return new LiteCraftTreeNode(parent, new GenericStack(node.what(), amount), List.of(), 0);
        }

        path.set(nodeIndex);
        materializedProcesses.set(nodeIndex);
        try {
            var children = new ArrayList<LiteCraftTreeNode>(node.inputs().size());
            var process = new LiteCraftTreeProc(children, node.machines(), node.machineLocations());
            for (var edge : node.inputs()) {
                if (edge.producerIndex() != null && edge.allocatedOutput() > 0) {
                    children.add(materialize(snapshot, edge.producerIndex(), process,
                        edge.allocatedOutput(), path, materializedProcesses));
                }
                long terminalAmount = edge.extractedExternal() + edge.missing();
                if (terminalAmount > 0) {
                    children.add(new LiteCraftTreeNode(process,
                        new GenericStack(edge.inputKey(), terminalAmount), List.of(), edge.missing()));
                }
            }
            List<LiteCraftTreeProc> processes = children.isEmpty() ? List.of() : List.of(process);
            return new LiteCraftTreeNode(parent, new GenericStack(node.what(), amount), processes, 0);
        } finally {
            path.clear(nodeIndex);
        }
    }
}
