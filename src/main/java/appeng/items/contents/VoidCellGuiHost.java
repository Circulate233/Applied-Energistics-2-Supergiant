package appeng.items.contents;

import appeng.api.config.CondenserOutput;
import appeng.api.implementations.guiobjects.ItemGuiHost;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.items.storage.VoidCellItem;
import net.minecraft.entity.player.EntityPlayer;

public class VoidCellGuiHost extends ItemGuiHost<VoidCellItem> {

    public VoidCellGuiHost(VoidCellItem item, EntityPlayer player, ItemGuiHostLocator locator) {
        super(item, player, locator);
    }

    public CondenserOutput getMode() {
        return getItem().getMode(getItemStack());
    }

    public void setMode(CondenserOutput mode) {
        getItem().setMode(getItemStack(), mode);
    }
}
