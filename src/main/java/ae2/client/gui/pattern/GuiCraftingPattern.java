package ae2.client.gui.pattern;

import ae2.container.pattern.ContainerCraftingPattern;
import ae2.core.AppEng;
import ae2.core.localization.Tooltips;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GuiCraftingPattern extends GuiPattern<ContainerCraftingPattern> {

    private static final ResourceLocation BACKGROUND =
        AppEng.makeId("textures/guis/crafting_pattern_recipe.png");

    public GuiCraftingPattern(ContainerCraftingPattern container, InventoryPlayer playerInventory) {
        super(container, playerInventory);
        this.ySize = 109;
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRenderer.drawString(
            I18n.format("gui.pattern_view.craft.substitute",
                this.container.canSubstitute() ? Tooltips.On.getLocal() : Tooltips.Off.getLocal()),
            8, 6, 0x303030);
        this.fontRenderer.drawString(
            I18n.format("gui.pattern_view.craft.fluid_substitute",
                this.container.canSubstituteFluids() ? Tooltips.On.getLocal() : Tooltips.Off.getLocal()),
            8, 19, 0x303030);
    }
}
