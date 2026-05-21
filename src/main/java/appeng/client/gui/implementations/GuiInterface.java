package appeng.client.gui.implementations;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.client.gui.Icon;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.container.SlotSemantics;
import appeng.container.implementations.ContainerInterface;
import appeng.core.definitions.AEItems;
import appeng.core.localization.ButtonToolTips;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

public class GuiInterface extends GuiUpgradeable<ContainerInterface> {

    private final SettingToggleButton<FuzzyMode> fuzzyMode;
    private final List<SetAmountButton> amountButtons = new ObjectArrayList<>();

    public GuiInterface(ContainerInterface container, InventoryPlayer playerInventory, ITextComponent title, GuiStyle style) {
        super(container, playerInventory, title, style);

        this.fuzzyMode = addToLeftToolbar(new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL));
        widgets.addOpenPriorityButton();

        var configSlots = container.getSlots(SlotSemantics.CONFIG);
        for (int i = 0; i < configSlots.size(); i++) {
            int slotIndex = i;
            var button = new SetAmountButton(() -> {
                var configSlot = (appeng.container.slot.AppEngSlot) container.getSlots(SlotSemantics.CONFIG).get(slotIndex);
                container.openSetAmountGui(configSlot.getSlotIndex());
            });
            button.setDisableBackground(true);
            button.setMessage(ButtonToolTips.InterfaceSetStockAmount.text());
            widgets.add("amtButton" + (i + 1), button);
            amountButtons.add(button);
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.fuzzyMode.set(container.getFuzzyMode());
        this.fuzzyMode.setVisibility(container.hasUpgrade(AEItems.FUZZY_CARD.item()));

        var configSlots = container.getSlots(SlotSemantics.CONFIG);
        for (int i = 0; i < amountButtons.size(); i++) {
            var button = amountButtons.get(i);
            button.setVisibility(!configSlots.get(i).getStack().isEmpty());
        }
    }

    static class SetAmountButton extends IconButton {
        SetAmountButton(Runnable onPress) {
            super(onPress);
        }

        @Override
        protected Icon getIcon() {
            return this.hovered ? Icon.COG : Icon.COG_DISABLED;
        }
    }
}
