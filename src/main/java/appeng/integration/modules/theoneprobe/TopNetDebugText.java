package appeng.integration.modules.theoneprobe;

import appeng.core.localization.LocalizationEnum;

public enum TopNetDebugText implements LocalizationEnum {
    grid_pivot_pos,
    grid_id,
    grid_nodes,
    grid_cpu_avg_max,
    storage,
    crafting,
    tick,
    misc;

    @Override
    public String getTranslationKey() {
        return "theoneprobe.ae2.netdebug." + name();
    }
}
