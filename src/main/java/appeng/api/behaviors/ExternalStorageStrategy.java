package appeng.api.behaviors;

import appeng.api.stacks.AEKeyType;
import appeng.api.storage.MEStorage;
import appeng.parts.automation.StackWorldBehaviors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
public interface ExternalStorageStrategy {
    static void register(AEKeyType type, Factory factory) {
        StackWorldBehaviors.registerExternalStorageStrategy(type, factory);
    }

    @Nullable
    MEStorage createWrapper(boolean extractableOnly, Runnable injectOrExtractCallback);

    @FunctionalInterface
    interface Factory {
        ExternalStorageStrategy create(WorldServer level, BlockPos fromPos, EnumFacing fromSide);
    }
}
