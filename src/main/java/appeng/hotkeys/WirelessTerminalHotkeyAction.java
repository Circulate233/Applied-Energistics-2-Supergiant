package appeng.hotkeys;

import appeng.api.features.HotkeyAction;
import appeng.api.implementations.items.WirelessTerminalDefinition;
import appeng.core.gui.locator.GuiHostLocators;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.integration.modules.baubles.BaublesIntegration;
import appeng.items.tools.powered.WirelessUniversalTerminalItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public record WirelessTerminalHotkeyAction(WirelessTerminalDefinition definition) implements HotkeyAction {
    @Override
    public boolean run(EntityPlayerMP player) {
        ItemGuiHostLocator fallback = null;
        int inventorySize = player.inventory.getSizeInventory();
        for (int slot = 0; slot < inventorySize; slot++) {
            ItemGuiHostLocator locator = GuiHostLocators.forInventorySlot(slot);
            if (openUniversal(player, locator)) {
                return true;
            }
            if (fallback == null && isStandaloneTerminal(player, locator)) {
                fallback = locator;
            }
        }

        int baubleSlots = BaublesIntegration.getSlots(player);
        for (int slot = 0; slot < baubleSlots; slot++) {
            ItemGuiHostLocator locator = GuiHostLocators.forBaubleSlot(slot);
            if (openUniversal(player, locator)) {
                return true;
            }
            if (fallback == null && isStandaloneTerminal(player, locator)) {
                fallback = locator;
            }
        }
        return fallback != null && openStandalone(player, fallback);
    }

    private boolean openUniversal(EntityPlayerMP player, ItemGuiHostLocator locator) {
        ItemStack stack = locator.locateItem(player);
        if (!(stack.getItem() instanceof WirelessUniversalTerminalItem universalTerminal)) {
            return false;
        }
        if (!universalTerminal.hasTerminal(stack, this.definition.item())) {
            return false;
        }
        if (!universalTerminal.selectTerminal(stack, this.definition.id())) {
            return false;
        }
        return this.definition.open(player, locator, stack, false);
    }

    private boolean isStandaloneTerminal(EntityPlayerMP player, ItemGuiHostLocator locator) {
        return locator.locateItem(player).getItem() == this.definition.item();
    }

    private boolean openStandalone(EntityPlayerMP player, ItemGuiHostLocator locator) {
        ItemStack stack = locator.locateItem(player);
        if (stack.getItem() != this.definition.item()) {
            return false;
        }
        return this.definition.open(player, locator, stack, false);
    }
}
