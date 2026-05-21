package appeng.helpers;

import appeng.parts.encoding.PatternEncodingLogic;
import net.minecraft.world.World;

public interface IPatternTerminalLogicHost {
    PatternEncodingLogic getLogic();

    World getLevel();

    void markForSave();
}
