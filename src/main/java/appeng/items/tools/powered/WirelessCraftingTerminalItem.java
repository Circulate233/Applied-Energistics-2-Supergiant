package appeng.items.tools.powered;

import appeng.container.GuiIds;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.helpers.WirelessCraftingTerminalGuiHost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import org.jetbrains.annotations.Nullable;

public class WirelessCraftingTerminalItem extends WirelessTerminalItem {
    public WirelessCraftingTerminalItem(double powerCapacity) {
        super(powerCapacity);
    }

    @Override
    public GuiIds.GuiKey getGuiKey() {
        return GuiIds.GuiKey.WIRELESS_CRAFTING_TERMINAL;
    }

    @Nullable
    @Override
    public WirelessCraftingTerminalGuiHost<?> getGuiHost(EntityPlayer player, ItemGuiHostLocator locator,
                                                         @Nullable RayTraceResult hitResult) {
        return new WirelessCraftingTerminalGuiHost<>(this, player, locator,
            (p, sm) -> openFromInventory(p, locator, true));
    }
}
