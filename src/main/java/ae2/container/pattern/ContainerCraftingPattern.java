package ae2.container.pattern;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.GenericStack;
import ae2.crafting.pattern.AECraftingPattern;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import java.util.Collections;

public class ContainerCraftingPattern extends ContainerPattern {

    public ContainerCraftingPattern(InventoryPlayer playerInventory, ItemStack stack) {
        super(playerInventory, stack);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                this.addDisplaySlot(this.inputs, row * 3 + column, 29 + column * 18, 35 + row * 18);
            }
        }
        this.addDisplaySlot(this.outputs, 0, 121, 53);
    }

    @Override
    protected void analyse() {
        if (!(this.details instanceof AECraftingPattern pattern)) {
            this.invalidate();
            return;
        }

        IPatternDetails.IInput[] rawInputs = pattern.getInputs();
        GenericStack[][] sparseInputs = new GenericStack[9][];
        for (int i = 0; i < sparseInputs.length; i++) {
            int compressedIndex = pattern.getCompressedInputIndex(i);
            if (compressedIndex == -1) {
                sparseInputs[i] = new GenericStack[0];
                continue;
            }

            GenericStack[] possibleInputs = this.clean(rawInputs[compressedIndex].possibleInputs());
            GenericStack[] displayInputs = new GenericStack[possibleInputs.length];
            for (int j = 0; j < possibleInputs.length; j++) {
                displayInputs[j] = new GenericStack(possibleInputs[j].what(), possibleInputs[j].amount());
            }
            sparseInputs[i] = displayInputs;
        }
        Collections.addAll(this.inputs, sparseInputs);

        GenericStack output = pattern.getPrimaryOutput();
        this.outputs.add(new GenericStack[]{new GenericStack(output.what(), output.amount())});
    }

    public boolean canSubstitute() {
        return this.details instanceof AECraftingPattern pattern && pattern.canSubstitute();
    }

    public boolean canSubstituteFluids() {
        return this.details instanceof AECraftingPattern pattern && pattern.canSubstituteFluids();
    }
}
