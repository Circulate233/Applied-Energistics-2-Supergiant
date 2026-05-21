package appeng.helpers;

import appeng.api.storage.ITerminalHost;
import appeng.parts.encoding.PatternEncodingLogic;

public interface IPatternTerminalGuiHost extends ITerminalHost {
    PatternEncodingLogic getLogic();
}
