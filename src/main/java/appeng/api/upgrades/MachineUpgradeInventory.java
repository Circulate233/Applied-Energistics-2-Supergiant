package appeng.api.upgrades;

import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.item.Item;
import org.jetbrains.annotations.Nullable;

class MachineUpgradeInventory extends UpgradeInventory {
    @Nullable
    private final MachineUpgradesChanged changeCallback;

    public MachineUpgradeInventory(Item item, int slots, @Nullable MachineUpgradesChanged changeCallback) {
        super(item, slots);
        this.changeCallback = changeCallback;
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        super.onChangeInventory(inv, slot);

        if (changeCallback != null) {
            changeCallback.onUpgradesChanged();
        }
    }
}
