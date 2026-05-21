package appeng.container.slot;

import appeng.api.inventories.InternalInventory;
import appeng.client.gui.Icon;
import net.minecraft.item.ItemStack;

public class OutputSlot extends AppEngSlot {
    public OutputSlot(InternalInventory inventory, int slotIndex, int x, int y) {
        super(inventory, slotIndex, x, y);
    }

    public OutputSlot(InternalInventory inventory, int slotIndex, int x, int y, Icon icon) {
        this(inventory, slotIndex, x, y);
        this.setIcon(icon);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }
}
