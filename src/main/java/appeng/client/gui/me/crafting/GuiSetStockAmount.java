package appeng.client.gui.me.crafting;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.implementations.AESubGui;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.container.implementations.ContainerSetStockAmount;
import appeng.core.localization.GuiText;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;

public class GuiSetStockAmount extends AEBaseGui<ContainerSetStockAmount> {

    private final NumberEntryWidget amount;
    private boolean amountInitialized;

    public GuiSetStockAmount(ContainerSetStockAmount container, InventoryPlayer playerInventory, ITextComponent title,
                             GuiStyle style) {
        super(container, playerInventory, style);

        widgets.addButton("save", GuiText.Set.text(), this::confirm);
        AESubGui.addBackButton(container, "back", widgets);

        this.amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.UNITLESS);
        this.amount.setLongValue(1);
        this.amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
        this.amount.setMinValue(0);
        this.amount.setHideValidationIcon(true);
        this.amount.setOnConfirm(this::confirm);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!this.amountInitialized) {
            var whatToStock = container.getWhatToStock();
            if (whatToStock != null) {
                this.amount.setType(NumberEntryType.of(whatToStock));
                this.amount.setLongValue(container.getInitialAmount());
                this.amount.setMaxValue(container.getMaxAmount());
                this.amountInitialized = true;
            }
        }
    }

    private void confirm() {
        this.amount.getIntValue().ifPresent(container::confirm);
    }
}

