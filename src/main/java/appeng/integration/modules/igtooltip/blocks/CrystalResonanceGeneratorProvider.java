package appeng.integration.modules.igtooltip.blocks;

import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.core.localization.InGameTooltip;
import appeng.tile.networking.TileCrystalResonanceGenerator;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

public final class CrystalResonanceGeneratorProvider implements BodyProvider<TileCrystalResonanceGenerator> {
    @Override
    public void buildTooltip(TileCrystalResonanceGenerator generator, TooltipContext context, TooltipBuilder tooltip) {
        if (generator.isSuppressed()) {
            tooltip.addLine(InGameTooltip.Suppressed.text().setStyle(new Style().setColor(TextFormatting.RED)));
        }
    }
}
