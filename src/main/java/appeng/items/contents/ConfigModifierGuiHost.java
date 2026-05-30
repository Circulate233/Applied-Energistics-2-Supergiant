package appeng.items.contents;

import appeng.api.implementations.guiobjects.ItemGuiHost;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.items.tools.ConfigModifierItem;
import net.minecraft.entity.player.EntityPlayer;

public class ConfigModifierGuiHost extends ItemGuiHost<ConfigModifierItem> {

    public ConfigModifierGuiHost(ConfigModifierItem item, EntityPlayer player, ItemGuiHostLocator locator) {
        super(item, player, locator);
    }

    public ConfigModifierItem.Settings getSettings() {
        return getItem().getSettings(getItemStack());
    }

    public void setMode(ConfigModifierItem.Mode mode) {
        getItem().setSettings(getItemStack(), new ConfigModifierItem.Settings(mode, getSettings().data()));
    }

    public void setData(long data) {
        getItem().setSettings(getItemStack(), new ConfigModifierItem.Settings(getSettings().mode(), data));
    }
}
