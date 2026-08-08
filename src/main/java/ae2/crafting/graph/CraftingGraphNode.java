package ae2.crafting.graph;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEKey;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public class CraftingGraphNode {
    private final AEKey what;
    private final IPatternDetails pattern;
    private final long outputPerCraft;
    private final List<CraftingGraphEdge> inputs = new ObjectArrayList<>();
    private final List<CraftingGraphNode> parents = new ObjectArrayList<>();

    private long demandAmount = 0;
    private long craftTimes = 0;

    public CraftingGraphNode(AEKey what, IPatternDetails pattern, long outputPerCraft) {
        this.what = what;
        this.pattern = pattern;
        this.outputPerCraft = outputPerCraft;
    }

    public AEKey getWhat() {
        return what;
    }

    public IPatternDetails getPattern() {
        return pattern;
    }

    public long getOutputPerCraft() {
        return outputPerCraft;
    }

    public List<CraftingGraphEdge> getInputs() {
        return inputs;
    }

    public void addInput(CraftingGraphEdge edge) {
        inputs.add(edge);
    }

    public List<CraftingGraphNode> getParents() {
        return parents;
    }

    public void addParent(CraftingGraphNode parent) {
        parents.add(parent);
    }

    public long getDemandAmount() {
        return demandAmount;
    }

    public void setDemandAmount(long demandAmount) {
        this.demandAmount = demandAmount;
    }

    public void addDemandAmount(long amount) {
        this.demandAmount += amount;
    }

    public long getCraftTimes() {
        return craftTimes;
    }

    public void setCraftTimes(long craftTimes) {
        this.craftTimes = craftTimes;
    }
}
