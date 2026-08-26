package ae2.api.features;

import com.google.common.base.Preconditions;
import ae2.api.implementations.items.IChargeableItemAdapter;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Registry for addon-provided item energy adapters used by the AE2 charger.
 */
public final class ChargeableItems {
    private static final ObjectList<IChargeableItemAdapter> adapters = new ObjectArrayList<>();

    private ChargeableItems() {
    }

    public static synchronized void register(IChargeableItemAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        Preconditions.checkArgument(!adapters.contains(adapter),
            "Tried to register the same chargeable item adapter twice.");
        adapters.add(adapter);
    }

    @Nullable
    public static synchronized IChargeableItemAdapter get(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (IChargeableItemAdapter adapter : adapters) {
            if (adapter.handles(stack)) {
                return adapter;
            }
        }
        return null;
    }

    public static synchronized boolean isChargeable(ItemStack stack) {
        return get(stack) != null;
    }
}
