package appeng.helpers.patternprovider;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;

import javax.annotation.Nullable;

public interface PatternContainer {
    @Nullable
    IGrid getGrid();

    default boolean isVisibleInTerminal() {
        return true;
    }

    InternalInventory getTerminalPatternInventory();

    default long getTerminalSortOrder() {
        return 0;
    }

    PatternContainerGroup getTerminalGroup();
}
