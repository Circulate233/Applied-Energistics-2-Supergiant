package appeng.client.gui.implementations;

import appeng.api.config.ActionItems;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ActionButton;
import appeng.container.implementations.ContainerODStorageBus;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class GuiODStorageBus extends GuiSpecialStorageBus<ContainerODStorageBus> {
    private final AETextField white;
    private final AETextField black;

    public GuiODStorageBus(ContainerODStorageBus container, InventoryPlayer playerInventory, ITextComponent title,
                           GuiStyle style) {
        super(container, playerInventory, title, style);
        addToLeftToolbar(new ActionButton(ActionItems.COG, container::partition));
        this.white = widgets.addTextField("whiteExpression");
        this.black = widgets.addTextField("blackExpression");
        this.white.setResponder(container::setWhiteExpression);
        this.black.setResponder(container::setBlackExpression);
        this.white.setMaxStringLength(1024);
        this.black.setMaxStringLength(1024);
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
