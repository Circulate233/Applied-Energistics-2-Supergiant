package appeng.client.gui.me.crafting;

import appeng.client.gui.widgets.ITooltip;
import appeng.core.AppEng;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.awt.Rectangle;
import java.util.List;

final class CraftingTreeButton extends GuiButton implements ITooltip {
    private static final ResourceLocation TEXTURE = AppEng.makeId("textures/guis/ctl/guicraftingtree.png");
    private static final int SIZE_X = 24;
    private static final int SIZE_Y = 20;
    private static final int NORMAL_TEXTURE_X = 232;
    private static final int HOVERED_TEXTURE_X = 208;
    private static final int MOUSE_DOWN_TEXTURE_X = 184;

    private final Runnable onPress;
    private final int textureY;
    private final int activeTextureX;
    private ITextComponent tooltip;
    private boolean active;
    private boolean mouseDown;

    CraftingTreeButton(int textureY, int activeTextureX, ITextComponent tooltip, Runnable onPress) {
        super(0, 0, 0, SIZE_X, SIZE_Y, "");
        this.textureY = textureY;
        this.activeTextureX = activeTextureX;
        this.tooltip = tooltip;
        this.onPress = onPress;
    }

    void setTooltip(ITextComponent tooltip) {
        this.tooltip = tooltip;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }

        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        int textureX;
        if (active) {
            textureX = activeTextureX;
        } else if (mouseDown) {
            textureX = MOUSE_DOWN_TEXTURE_X;
        } else if (hovered) {
            textureX = HOVERED_TEXTURE_X;
        } else {
            textureX = NORMAL_TEXTURE_X;
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, enabled ? 1.0F : 0.45F);
        minecraft.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(x, y, textureX, textureY, width, height);
    }

    @Override
    public boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY) {
        this.mouseDown = enabled && visible
            && mouseX >= x
            && mouseY >= y
            && mouseX < x + width
            && mouseY < y + height;
        return mouseDown;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        boolean releasedInside = mouseDown
            && enabled
            && visible
            && mouseX >= x
            && mouseY >= y
            && mouseX < x + width
            && mouseY < y + height;
        mouseDown = false;
        if (releasedInside && onPress != null) {
            onPress.run();
        }
    }

    @Override
    public List<ITextComponent> getTooltipMessage() {
        return tooltip == null ? List.of() : List.of(tooltip);
    }

    @Override
    public Rectangle getTooltipArea() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible && enabled && !mouseDown && tooltip != null;
    }
}
