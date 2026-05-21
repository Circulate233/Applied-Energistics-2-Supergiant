package appeng.container.slot;

import appeng.client.Point;

public interface IOptionalSlot {
    default boolean isRenderDisabled() {
        return false;
    }

    boolean isSlotEnabled();

    default Point getBackgroundPos() {
        return Point.ZERO;
    }
}
