package appeng.integration.modules.igtooltip.blocks;

import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.core.localization.InGameTooltip;
import appeng.tile.misc.TileCharger;
import net.minecraft.item.ItemStack;

/**
 * Shows the tooltip of the item being charged, which usually includes a charge meter.
 */
public final class ChargerDataProvider implements BodyProvider<TileCharger> {
    @Override
    public void buildTooltip(TileCharger charger, TooltipContext context, TooltipBuilder tooltip) {
        ItemStack chargingItem = charger.getClientDisplayItem();

        if (!chargingItem.isEmpty()) {
            tooltip.addLine(InGameTooltip.Contains.text(chargingItem.getDisplayName()));

            if (chargingItem.getItem() instanceof IAEItemPowerStorage powerStorage) {
                var fillRate = (int) Math.floor(
                    powerStorage.getAECurrentPower(chargingItem) * 100 / powerStorage.getAEMaxPower(chargingItem));
                tooltip.addLine(InGameTooltip.Charged.text(fillRate));
            }
        }
    }
}
