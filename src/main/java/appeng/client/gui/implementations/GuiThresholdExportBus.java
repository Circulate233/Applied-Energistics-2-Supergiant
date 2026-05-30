package appeng.client.gui.implementations;

import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.container.implementations.ContainerThresholdExportBus;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;

public class GuiThresholdExportBus extends GuiSpecialExportBus<ContainerThresholdExportBus> {
    private final SettingToggleButton<SchedulingMode> schedulingMode;

    public GuiThresholdExportBus(ContainerThresholdExportBus container, InventoryPlayer playerInventory,
                                 ITextComponent title, GuiStyle style) {
        super(container, playerInventory, title, style);
        if (container.getHost().getConfigManager().hasSetting(Settings.SCHEDULING_MODE)) {
            this.schedulingMode = addToLeftToolbar(
                new ServerSettingToggleButton<>(Settings.SCHEDULING_MODE, SchedulingMode.DEFAULT));
        } else {
            this.schedulingMode = null;
        }
        addToLeftToolbar(new ThresholdModeButton(container));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (this.schedulingMode != null) {
            this.schedulingMode.set(container.getSchedulingMode());
        }
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        GlStateManager.pushMatrix();
        GlStateManager.translate(10, 17, 0);
        GlStateManager.scale(0.6f, 0.6f, 1);
        int color = this.style != null
            ? this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB() & 0xFFFFFF
            : 0x404040;
        this.fontRenderer.drawString(new TextComponentTranslation("gui.ae2.PreciseBusSetAmount").getFormattedText(),
            0, 0, color);
        GlStateManager.popMatrix();
    }

    private static final class ThresholdModeButton extends appeng.client.gui.widgets.IconButton {
        private final ContainerThresholdExportBus container;

        private ThresholdModeButton(ContainerThresholdExportBus container) {
            super(() -> container.setMode(
                container.mode == appeng.parts.automation.special.ThresholdMode.GREATER
                    ? appeng.parts.automation.special.ThresholdMode.LOWER
                    : appeng.parts.automation.special.ThresholdMode.GREATER));
            this.container = container;
        }

        @Override
        protected appeng.client.gui.Icon getIcon() {
            return this.container.mode == appeng.parts.automation.special.ThresholdMode.GREATER
                ? appeng.client.gui.Icon.ARROW_UP
                : appeng.client.gui.Icon.ARROW_DOWN;
        }

        @Override
        public List<ITextComponent> getTooltipMessage() {
            String key = this.container.mode == appeng.parts.automation.special.ThresholdMode.GREATER
                ? "gui.ae2.ThresholdGreater"
                : "gui.ae2.ThresholdLower";
            return List.of(new TextComponentTranslation(key));
        }
    }
}
