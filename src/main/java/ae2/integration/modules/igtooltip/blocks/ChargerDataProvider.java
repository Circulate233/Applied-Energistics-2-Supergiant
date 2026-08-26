package ae2.integration.modules.igtooltip.blocks;

import ae2.api.features.ChargeableItems;
import ae2.api.implementations.items.IChargeableItemAdapter;
import ae2.api.implementations.items.IAEItemPowerStorage;
import ae2.api.integrations.igtooltip.TooltipBuilder;
import ae2.api.integrations.igtooltip.TooltipContext;
import ae2.api.integrations.igtooltip.providers.BodyProvider;
import ae2.integration.modules.theoneprobe.TopText;
import ae2.integration.modules.theoneprobe.TopTooltipFormatter;
import ae2.tile.misc.TileCharger;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

/**
 * Shows the tooltip of the item being charged, which usually includes a charge meter.
 */
public final class ChargerDataProvider implements BodyProvider<TileCharger> {
    @Override
    public void buildTooltip(TileCharger charger, TooltipContext context, TooltipBuilder tooltip) {
        ItemStack chargingItem = charger.getClientDisplayItem();

        if (!chargingItem.isEmpty()) {
            tooltip.addLabel(TopText.contains, TopTooltipFormatter.displayName(chargingItem), TextFormatting.GREEN);

            if (chargingItem.getItem() instanceof IAEItemPowerStorage powerStorage) {
                addFillRate(tooltip, powerStorage.getAECurrentPower(chargingItem),
                    powerStorage.getAEMaxPower(chargingItem));
            } else {
                IChargeableItemAdapter adapter = ChargeableItems.get(chargingItem);
                if (adapter != null) {
                    addFillRate(tooltip, adapter.getCurrentPower(chargingItem), adapter.getMaxPower(chargingItem));
                }
            }
        }
    }

    private static void addFillRate(TooltipBuilder tooltip, double currentPower, double maxPower) {
        if (!Double.isFinite(currentPower) || !Double.isFinite(maxPower) || maxPower <= 0.0D) {
            return;
        }
        var fillRate = (int) Math.floor(Math.clamp(currentPower * 100 / maxPower, 0.0D, 100.0D));
        tooltip.addLabel(TopText.charged, fillRate + "%");
    }
}
