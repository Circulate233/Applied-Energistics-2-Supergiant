package appeng.helpers.patternmodifier;

import appeng.core.definitions.AEItems;
import appeng.core.gui.locator.GuiHostLocators;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.integration.modules.baubles.BaublesIntegration;
import appeng.items.tools.PatternModifierItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class PatternModifierLocator {

    private PatternModifierLocator() {
    }

    @Nullable
    public static LocatedPatternModifier find(EntityPlayer player) {
        for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
            ItemStack stack = player.inventory.getStackInSlot(slot);
            if (isPatternModifier(stack)) {
                return new LocatedPatternModifier(stack, GuiHostLocators.forInventorySlot(slot), slot);
            }
        }

        for (int slot = 0; slot < BaublesIntegration.getSlots(player); slot++) {
            ItemStack stack = BaublesIntegration.getStackInSlot(player, slot);
            if (isPatternModifier(stack)) {
                return new LocatedPatternModifier(stack, GuiHostLocators.forBaubleSlot(slot), null);
            }
        }
        return null;
    }

    public static boolean isPatternModifier(ItemStack stack) {
        return AEItems.PATTERN_MODIFIER.is(stack) && stack.getItem() instanceof PatternModifierItem;
    }

    public record LocatedPatternModifier(ItemStack stack, ItemGuiHostLocator locator,
                                         @Nullable Integer playerInventorySlot) {
        public PatternModifierItem item() {
            return (PatternModifierItem) stack.getItem();
        }
    }
}
