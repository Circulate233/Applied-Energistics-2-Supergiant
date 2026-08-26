package ae2.api.implementations.items;

import ae2.api.config.Actionable;
import net.minecraft.item.ItemStack;

/**
 * Adapts an item that stores energy without implementing {@link IAEItemPowerStorage}
 * so it can be charged by the AE2 charger.
 */
public interface IChargeableItemAdapter {
    /**
     * @return whether this adapter owns the charging behavior for the stack.
     */
    boolean handles(ItemStack stack);

    double getCurrentPower(ItemStack stack);

    double getMaxPower(ItemStack stack);

    /**
     * @return the amount of AE power accepted per tick.
     */
    double getChargeRate(ItemStack stack);

    /**
     * Injects AE power and returns the amount not accepted by the item.
     */
    double injectPower(ItemStack stack, double amount, Actionable mode);

    default boolean isFullyCharged(ItemStack stack) {
        return getCurrentPower(stack) >= getMaxPower(stack);
    }
}
