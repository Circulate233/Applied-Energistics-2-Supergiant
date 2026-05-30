package appeng.client.gui.implementations;

import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.container.implementations.ContainerODFilterBus;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class GuiODFilterBus extends GuiSpecialExportBus<ContainerODFilterBus<?>> {
    protected final AETextField white;
    protected final AETextField black;

    public GuiODFilterBus(ContainerODFilterBus<?> container, InventoryPlayer playerInventory, ITextComponent title,
                          GuiStyle style) {
        super(container, playerInventory, title, style);
        this.white = widgets.addTextField("whiteExpression");
        this.black = widgets.addTextField("blackExpression");
        this.white.setResponder(container::setWhiteExpression);
        this.black.setResponder(container::setBlackExpression);
        this.white.setMaxStringLength(512);
        this.black.setMaxStringLength(512);
        this.white.setText(container.whiteExpression);
        this.black.setText(container.blackExpression);
        this.white.setPlaceholder(new TextComponentTranslation("gui.ae2.ODFilterWhiteTooltip"));
        this.black.setPlaceholder(new TextComponentTranslation("gui.ae2.ODFilterBlackTooltip"));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!this.white.isFocused() && !this.white.getText().equals(container.whiteExpression)) {
            this.white.setText(container.whiteExpression);
        }
        if (!this.black.isFocused() && !this.black.getText().equals(container.blackExpression)) {
            this.black.setText(container.blackExpression);
        }
    }

}
