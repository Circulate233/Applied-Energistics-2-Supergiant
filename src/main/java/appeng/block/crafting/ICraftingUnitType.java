package appeng.block.crafting;

import net.minecraft.item.Item;

public interface ICraftingUnitType {

    long getStorageBytes();

    int getAcceleratorThreads();

    Item getItemFromType();
}

