package appeng.client.gui.implementations;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.style.GuiStyle;
import appeng.container.implementations.ContainerIngredientBuffer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;

public class GuiIngredientBuffer extends AEBaseGui<ContainerIngredientBuffer> {
    public GuiIngredientBuffer(ContainerIngredientBuffer container, InventoryPlayer playerInventory,
                               ITextComponent title, GuiStyle style) {
        super(container, playerInventory, style);
    }
}
