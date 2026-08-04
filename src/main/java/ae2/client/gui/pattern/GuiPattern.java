package ae2.client.gui.pattern;

import ae2.api.client.AEKeyRendering;
import ae2.api.stacks.AEKey;
import ae2.client.gui.AEBaseGui;
import ae2.core.localization.ButtonToolTips;
import ae2.core.localization.Tooltips;
import ae2.container.pattern.ContainerPattern;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

public abstract class GuiPattern<T extends ContainerPattern> extends AEBaseGui<T> {

    private int cycle;
    private int cycleTick;

    protected GuiPattern(T container, InventoryPlayer playerInventory) {
        super(container, playerInventory);
        this.container.setCycleItem(this.cycle);
        this.xSize = 176;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.cycleTick % 80 == 0) {
            this.cycle++;
            this.container.setCycleItem(this.cycle);
        }
        this.cycleTick++;
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        Slot slot = this.getSlotUnderMouse();
        if (slot instanceof ContainerPattern.DisplayOnlySlot displaySlot
            && displaySlot.hasGenericDisplayStack()) {
            AEKey key = displaySlot.getGenericDisplayKey();
            long amount = displaySlot.getGenericDisplayAmount();
            if (key != null && amount > 0) {
                List<ITextComponent> tooltip = AEKeyRendering.getTooltip(key);
                if (Tooltips.shouldShowAmountTooltip(key, amount)) {
                    tooltip.add(Tooltips.getAmountTooltip(ButtonToolTips.StoredAmount, key, amount));
                }
                this.drawKeyTooltipWithImages(mouseX, mouseY, key, tooltip);
            }
            return;
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }
}
