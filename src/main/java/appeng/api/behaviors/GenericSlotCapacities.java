package appeng.api.behaviors;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKeyType;
import appeng.util.CowMap;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

/**
 * Allows custom key types to define slot capacities for pattern providers and interfaces.
 */
@ApiStatus.Experimental
public class GenericSlotCapacities {
    private static final CowMap<AEKeyType, Long> map = CowMap.identityHashMap();

    static {
        register(AEKeyType.items(), InternalInventory.DEFAULT_SLOT_LIMIT);
        register(AEKeyType.fluids(), 4L * AEFluidKey.AMOUNT_BUCKET);
    }

    private GenericSlotCapacities() {
    }

    public static void register(AEKeyType type, long capacity) {
        Preconditions.checkArgument(capacity >= 0, "capacity >= 0");
        map.putIfAbsent(type, capacity);
    }

    public static Map<AEKeyType, Long> getMap() {
        return map.getMap();
    }
}


