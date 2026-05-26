package appeng.me.cells;

import appeng.api.stacks.GenericStack;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.storage.cells.ISaveProvider;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.items.storage.StorageCellTooltipComponent;
import appeng.items.storage.VoidCellItem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class VoidCellHandler implements ICellHandler {
    public static final VoidCellHandler INSTANCE = new VoidCellHandler();

    @Override
    public boolean isCell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof VoidCellItem;
    }

    @Nullable
    @Override
    public VoidCellInventory getCellInventory(ItemStack stack, @Nullable ISaveProvider host) {
        if (isCell(stack)) {
            return new VoidCellInventory(stack);
        }
        return null;
    }

    public java.util.Optional<StorageCellTooltipComponent> getTooltipData(ItemStack stack) {
        if (!(stack.getItem() instanceof ICellWorkbenchItem workbenchItem)) {
            return java.util.Optional.empty();
        }

        var upgrades = new ObjectArrayList<ItemStack>();
        for (var upgrade : workbenchItem.getUpgrades(stack)) {
            if (!upgrade.isEmpty()) {
                upgrades.add(upgrade.copy());
            }
        }

        List<GenericStack> content = Collections.emptyList();
        boolean hasMoreContent = false;
        return java.util.Optional.of(new StorageCellTooltipComponent(upgrades, content, hasMoreContent, false));
    }

    public void addPartitionInformation(ItemStack stack, List<String> lines) {
        if (!(stack.getItem() instanceof ICellWorkbenchItem workbenchItem)) {
            return;
        }

        var config = workbenchItem.getConfigInventory(stack);
        if (config.isEmpty()) {
            lines.add(GuiText.Partitioned.text().appendText(" - ").appendSibling(GuiText.Nothing.text()).getFormattedText());
            return;
        }

        var upgrades = workbenchItem.getUpgrades(stack);
        var includeMode = upgrades.isInstalled(AEItems.INVERTER_CARD.item()) ? GuiText.Excluded : GuiText.Included;
        var precisionMode = upgrades.isInstalled(AEItems.FUZZY_CARD.item()) ? GuiText.Fuzzy : GuiText.Precise;

        lines.add(GuiText.Partitioned.text()
                                     .appendText(" - ")
                                     .appendSibling(includeMode.text())
                                     .appendText(" ")
                                     .appendSibling(precisionMode.text())
                                     .getFormattedText());
    }
}
