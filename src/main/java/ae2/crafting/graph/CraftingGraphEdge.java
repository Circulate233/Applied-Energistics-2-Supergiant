package ae2.crafting.graph;

import ae2.api.stacks.AEKey;
import org.jetbrains.annotations.Nullable;

public record CraftingGraphEdge(
    AEKey inputKey,
    long amountPerCraft,
    @Nullable CraftingGraphNode producer
) {
}
