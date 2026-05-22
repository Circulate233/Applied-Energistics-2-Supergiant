package appeng.container.me.common;

import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.IPortableTerminal;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.StorageHelper;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.core.gui.locator.GuiHostLocator;
import appeng.core.gui.locator.GuiHostLocators;
import appeng.integration.modules.baubles.BaublesIntegration;
import appeng.me.helpers.ActionHostEnergySource;
import appeng.me.helpers.PlayerSource;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public final class MEIngredientActions {
    private MEIngredientActions() {
    }

    public static boolean handleContainer(ContainerMEStorage container, EntityPlayerMP player, MEIngredientAction action,
                                          GenericStack stack) {
        return switch (action) {
            case RETRIEVE -> stack.what() instanceof AEItemKey itemKey && container.retrieveItemToPlayer(itemKey);
            case CRAFT -> container.openCraftAmount(player, stack.what());
        };
    }

    public static boolean handleWirelessTerminals(EntityPlayerMP player, MEIngredientAction action,
                                                  GenericStack stack) {
        int inventorySize = player.inventory.getSizeInventory();
        for (int slot = 0; slot < inventorySize; slot++) {
            if (!isGuiItem(player.inventory.getStackInSlot(slot))) {
                continue;
            }
            if (handleWirelessTerminal(player, GuiHostLocators.forInventorySlot(slot), action, stack)) {
                return true;
            }
        }

        int baubleSlots = BaublesIntegration.getSlots(player);
        for (int slot = 0; slot < baubleSlots; slot++) {
            if (!isGuiItem(BaublesIntegration.getStackInSlot(player, slot))) {
                continue;
            }
            if (handleWirelessTerminal(player, GuiHostLocators.forBaubleSlot(slot), action, stack)) {
                return true;
            }
        }

        return false;
    }

    private static boolean handleWirelessTerminal(EntityPlayerMP player, GuiHostLocator locator,
                                                  MEIngredientAction action, GenericStack stack) {
        ITerminalHost host = locator.locate(player, ITerminalHost.class);
        if (!(host instanceof IPortableTerminal) || !host.getLinkStatus().connected()) {
            return false;
        }

        if (!(host instanceof IActionHost actionHost)) {
            return false;
        }

        IGridNode node = actionHost.getActionableNode();
        if (node == null || !node.isActive()) {
            return false;
        }

        return switch (action) {
            case RETRIEVE -> stack.what() instanceof AEItemKey itemKey
                && retrieveFromWirelessTerminal(player, host, actionHost, itemKey);
            case CRAFT -> openWirelessCraftAmount(player, locator, node, stack.what());
        };
    }

    private static boolean retrieveFromWirelessTerminal(EntityPlayerMP player, ITerminalHost host,
                                                        IActionHost actionHost, AEItemKey what) {
        int amount = Math.min(what.getMaxStackSize(), getInsertableAmount(player.inventory, what));
        if (amount <= 0) {
            return false;
        }

        IEnergySource energySource = host instanceof IEnergySource source
            ? source
            : new ActionHostEnergySource(actionHost);
        long extracted = StorageHelper.poweredExtraction(energySource, host.getInventory(), what, amount,
            new PlayerSource(player, actionHost));
        if (extracted <= 0) {
            return false;
        }

        ItemStack extractedStack = what.toStack((int) extracted);
        return player.inventory.addItemStackToInventory(extractedStack);
    }

    private static boolean openWirelessCraftAmount(EntityPlayerMP player, GuiHostLocator locator, IGridNode node,
                                                   AEKey what) {
        if (!node.getGrid().getCraftingService().isCraftable(what)) {
            return false;
        }

        ContainerCraftAmount.open(player, locator, what, what.getAmountPerUnit(), player.openContainer);
        return true;
    }

    private static int getInsertableAmount(InventoryPlayer inventory, AEItemKey what) {
        int result = 0;
        int maxStackSize = what.getMaxStackSize();
        for (ItemStack stack : inventory.mainInventory) {
            if (stack.isEmpty()) {
                result += maxStackSize;
            } else if (what.matches(stack) && stack.getCount() < Math.min(stack.getMaxStackSize(), maxStackSize)) {
                result += Math.min(stack.getMaxStackSize(), maxStackSize) - stack.getCount();
            }

            if (result >= maxStackSize) {
                return maxStackSize;
            }
        }
        return result;
    }

    private static boolean isGuiItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IGuiItem;
    }
}
