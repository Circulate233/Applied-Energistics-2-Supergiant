package appeng.integration.modules.igtooltip;

import appeng.api.networking.IGridNode;
import appeng.core.localization.InGameTooltip;
import appeng.core.localization.LocalizationEnum;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

public enum GridNodeState {
    OFFLINE(InGameTooltip.DeviceOffline),
    NETWORK_BOOTING(InGameTooltip.NetworkBooting),
    MISSING_CHANNEL(InGameTooltip.DeviceMissingChannel),
    ONLINE(InGameTooltip.DeviceOnline);

    private final LocalizationEnum text;

    GridNodeState(LocalizationEnum text) {
        this.text = text;
    }

    public static GridNodeState fromNode(@Nullable IGridNode gridNode) {
        var state = GridNodeState.OFFLINE;
        if (gridNode != null && gridNode.isPowered()) {
            if (!gridNode.hasGridBooted()) {
                state = GridNodeState.NETWORK_BOOTING;
            } else if (!gridNode.meetsChannelRequirements()) {
                state = GridNodeState.MISSING_CHANNEL;
            } else {
                state = GridNodeState.ONLINE;
            }
        }
        return state;
    }

    public ITextComponent textComponent() {
        return text.text();
    }

}
