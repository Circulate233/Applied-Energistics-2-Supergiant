package appeng.api.integrations.igtooltip.providers;

import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
@ApiStatus.OverrideOnly
@FunctionalInterface
public interface BodyProvider<T> {
    void buildTooltip(T object, TooltipContext context, TooltipBuilder tooltip);
}
