package ae2.client.gui.pattern;

import ae2.container.pattern.ContainerProcessingPattern;
import ae2.core.AppEng;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiProcessingPattern extends GuiPattern<ContainerProcessingPattern> {

    private static final ResourceLocation BACKGROUND =
        AppEng.makeId("textures/guis/processing_pattern_recipe.png");

    public GuiProcessingPattern(ContainerProcessingPattern container, InventoryPlayer playerInventory) {
        super(container, playerInventory);
        this.ySize = 251;
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }
}
