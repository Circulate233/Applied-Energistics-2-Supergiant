package appeng.helpers.patternprovider;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nullable;

public interface PatternContainer {
    @Nullable
    IGrid getGrid();

    default boolean isVisibleInTerminal() {
        return true;
    }

    InternalInventory getTerminalPatternInventory();

    boolean containsPattern(AEItemKey pattern);

    default long getTerminalSortOrder() {
        return 0;
    }

    default void openTerminalPatternContainerGui(EntityPlayer player) {
    }

    PatternContainerGroup getTerminalGroup();
}
