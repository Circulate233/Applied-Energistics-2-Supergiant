package appeng.api.storage.cells;

import appeng.items.storage.StorageCellTooltipComponent;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Optional;

public interface IStackTooltipDataProvider {
    default void addToTooltip(ItemStack stack, List<String> lines) {
    }

    Optional<StorageCellTooltipComponent> getStackTooltipData(ItemStack stack);

    default Optional<StorageCellTooltipComponent> getTooltipImage(ItemStack stack) {
        return getStackTooltipData(stack);
    }
}
