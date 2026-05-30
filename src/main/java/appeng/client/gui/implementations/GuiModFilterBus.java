package appeng.client.gui.implementations;

import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.container.implementations.ContainerModFilterBus;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;

public class GuiModFilterBus extends GuiSpecialExportBus<ContainerModFilterBus<?>> {
    private final AETextField modExpression;

    public GuiModFilterBus(ContainerModFilterBus<?> container, InventoryPlayer playerInventory, ITextComponent title,
                           GuiStyle style) {
        super(container, playerInventory, title, style);
        this.modExpression = widgets.addTextField("modExpression");
        this.modExpression.setResponder(container::setModExpression);
        this.modExpression.setMaxStringLength(512);
        this.modExpression.setText(container.modExpression);
        this.modExpression.setPlaceholder(new net.minecraft.util.text.TextComponentTranslation("gui.ae2.ModFilterTooltip"));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!this.modExpression.isFocused() && !this.modExpression.getText().equals(container.modExpression)) {
            this.modExpression.setText(container.modExpression);
        }
    }

}
