package appeng.container.implementations;

import appeng.container.AEBaseContainer;
import appeng.container.SlotSemantics;
import appeng.container.slot.AppEngSlot;
import appeng.tile.misc.TileIngredientBuffer;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerIngredientBuffer extends AEBaseContainer {
    public ContainerIngredientBuffer(InventoryPlayer ip, TileIngredientBuffer host) {
        super(ip, host);
        for (int index = 0; index < host.getBuffer().size(); index++) {
            addSlot(new AppEngSlot(host.getBuffer().createGuiWrapper(), index), SlotSemantics.STORAGE);
        }
        addPlayerInventorySlots(8, 112);
    }
}
