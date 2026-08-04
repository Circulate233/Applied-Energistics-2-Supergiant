package ae2.container.pattern;

import ae2.api.stacks.GenericStack;
import ae2.crafting.pattern.AEProcessingPattern;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ContainerProcessingPattern extends ContainerPattern {

    public ContainerProcessingPattern(InventoryPlayer playerInventory, ItemStack stack) {
        super(playerInventory, stack);
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                this.addDisplaySlot(this.inputs, row * 9 + column, 8 + column * 18, 9 + row * 18);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addDisplaySlot(this.outputs, row * 9 + column, 8 + column * 18, 189 + row * 18);
            }
        }
    }

    @Override
    protected void analyse() {
        if (this.details instanceof AEProcessingPattern pattern) {
            this.addContents(pattern.getSparseInputs(), this.inputs);
            this.addContents(pattern.getSparseOutputs(), this.outputs);
        } else {
            this.invalidate();
        }
    }

    private void addContents(List<GenericStack> stacks, List<GenericStack[]> slots) {
        for (GenericStack stack : stacks) {
            if (stack == null) {
                slots.add(new GenericStack[0]);
            } else {
                slots.add(new GenericStack[]{new GenericStack(stack.what(), stack.amount())});
            }
        }
    }
}
