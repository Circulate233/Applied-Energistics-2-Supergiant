package appeng.api.behaviors;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface PickupSink {
    long insert(AEKey what, long amount, Actionable mode);
}
