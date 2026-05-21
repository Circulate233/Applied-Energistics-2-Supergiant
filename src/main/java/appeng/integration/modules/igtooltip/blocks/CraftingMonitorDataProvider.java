package appeng.integration.modules.igtooltip.blocks;

import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.core.localization.InGameTooltip;
import appeng.tile.crafting.TileCraftingMonitor;

/**
 * Shows the name of the item being crafted.
 */
public final class CraftingMonitorDataProvider implements BodyProvider<TileCraftingMonitor> {
    @Override
    public void buildTooltip(TileCraftingMonitor monitor, TooltipContext context, TooltipBuilder tooltip) {
        var displayStack = monitor.getJobProgress();

        if (displayStack != null) {
            tooltip.addLine(InGameTooltip.Crafting.text(displayStack.what().getDisplayName()));
        }
    }
}
