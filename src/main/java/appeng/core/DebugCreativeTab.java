package appeng.core;

import appeng.api.ids.AECreativeTabIds;
import appeng.core.definitions.AEItems;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public final class DebugCreativeTab extends CreativeTabs {

    public static final DebugCreativeTab INSTANCE = new DebugCreativeTab();

    private DebugCreativeTab() {
        super(AECreativeTabIds.DEBUG.toString());
    }

    @Override
    public ItemStack createIcon() {
        return AEItems.DEBUG_CARD.stack();
    }
}
