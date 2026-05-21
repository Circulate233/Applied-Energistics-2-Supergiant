package appeng.tile.crafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

public interface IMolecularAssemblerSupportedPattern extends IPatternDetails {
    ItemStack assemble(InventoryCrafting input, World level);

    default NonNullList<ItemStack> getRemainingItems(InventoryCrafting input) {
        return NonNullList.withSize(input.getSizeInventory(), ItemStack.EMPTY);
    }

    boolean isItemValid(int slot, AEItemKey key, World level);

    boolean isSlotEnabled(int slot);

    void fillCraftingGrid(KeyCounter[] table, CraftingGridAccessor gridAccessor);

    @Override
    default boolean supportsPushInputsToExternalInventory() {
        return false;
    }

    @FunctionalInterface
    interface CraftingGridAccessor {
        void set(int slot, ItemStack stack);
    }
}
