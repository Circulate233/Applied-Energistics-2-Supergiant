package appeng.client.gui.implementations;

import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.me.items.GuiSetProcessingPatternAmount;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.container.SlotSemantics;
import appeng.container.implementations.ContainerIOBus;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.Tooltips;
import appeng.core.network.InitNetwork;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.io.IOException;

public class GuiSpecialPreciseExportBus extends GuiSpecialExportBus<ContainerIOBus> {
    private final SettingToggleButton<YesNo> craftMode;
    private final SettingToggleButton<SchedulingMode> schedulingMode;

    public GuiSpecialPreciseExportBus(ContainerIOBus container, InventoryPlayer playerInventory, ITextComponent title,
                                      GuiStyle style) {
        super(container, playerInventory, title, style);
        if (container.getHost().getConfigManager().hasSetting(Settings.CRAFT_ONLY)) {
            this.craftMode = addToLeftToolbar(new ServerSettingToggleButton<>(Settings.CRAFT_ONLY, YesNo.NO));
        } else {
            this.craftMode = null;
        }
        if (container.getHost().getConfigManager().hasSetting(Settings.SCHEDULING_MODE)) {
            this.schedulingMode = addToLeftToolbar(
                new ServerSettingToggleButton<>(Settings.SCHEDULING_MODE, SchedulingMode.DEFAULT));
        } else {
            this.schedulingMode = null;
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (this.craftMode != null) {
            this.craftMode.set(container.getCraftingMode());
            this.craftMode.setVisibility(container.hasUpgrade(AEItems.CRAFTING_CARD.item()));
        }
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

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 2) {
            Slot slot = findSlot(mouseX, mouseY);
            if (canModifyAmount(slot)) {
                GenericStack currentStack = GenericStack.fromItemStack(slot.getStack());
                if (currentStack != null) {
                    switchToScreen(new GuiSetProcessingPatternAmount(this, currentStack,
                        newStack -> InitNetwork.sendToServer(new InventoryActionPacket(
                            this.container.windowId,
                            InventoryAction.SET_FILTER,
                            slot.slotNumber,
                            GenericStack.wrapInItemStack(newStack))),
                        AEParts.PRECISE_EXPORT_BUS.stack().getTextComponent()));
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (mouseButton == 2 && canModifyAmount(slot)) {
            return;
        }
        super.handleMouseClick(slot, slotId, mouseButton, clickType);
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        Slot slot = getSlotUnderMouse();
        if (this.playerInventory.getItemStack().isEmpty() && canModifyAmount(slot)) {
            var itemTooltip = new ObjectArrayList<>(getItemToolTip(slot.getStack()));
            GenericStack unwrapped = GenericStack.fromItemStack(slot.getStack());
            if (unwrapped != null) {
                itemTooltip.add(Tooltips.getAmountTooltip(ButtonToolTips.Amount, unwrapped).getFormattedText());
            }
            itemTooltip.add(Tooltips.getSetAmountTooltip().getFormattedText());
            drawTooltipLines(mouseX, mouseY, itemTooltip);
            return;
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    private boolean canModifyAmount(Slot slot) {
        return slot != null
            && slot.isEnabled()
            && slot.getHasStack()
            && this.container.getSlots(SlotSemantics.CONFIG).contains(slot);
    }
}
