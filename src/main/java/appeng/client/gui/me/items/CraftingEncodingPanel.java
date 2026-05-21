package appeng.client.gui.me.items;

import appeng.api.config.ActionItems;
import appeng.client.Point;
import appeng.client.gui.Icon;
import appeng.client.gui.Rect2i;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.container.SlotSemantics;
import net.minecraft.client.gui.Gui;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;

public class CraftingEncodingPanel extends EncodingModePanel {
    private static final Blitter BG = Blitter.texture("guis/pattern_modes.png").src(0, 0, 124, 66);

    private final ActionButton clearBtn;
    private final ToggleButton substitutionsBtn;
    private final ToggleButton fluidSubstitutionsBtn;

    public CraftingEncodingPanel(GuiPatternEncodingTerm screen, WidgetContainer widgets) {
        super(screen, widgets);
        this.clearBtn = new ActionButton(ActionItems.S_CLOSE, this.container::clear);
        this.clearBtn.setHalfSize(true);
        this.clearBtn.setDisableBackground(true);
        widgets.add("craftingClearPattern", this.clearBtn);

        this.substitutionsBtn = new ToggleButton(Icon.S_SUBSTITUTION_ENABLED, Icon.S_SUBSTITUTION_DISABLED,
            this.container::setSubstitute);
        this.substitutionsBtn.setHalfSize(true);
        this.substitutionsBtn.setDisableBackground(true);
        this.substitutionsBtn.setTooltipOn(List.of(
            new TextComponentTranslation("gui.tooltips.ae2.SubstitutionsOn"),
            new TextComponentTranslation("gui.tooltips.ae2.SubstitutionsDescEnabled")));
        this.substitutionsBtn.setTooltipOff(List.of(
            new TextComponentTranslation("gui.tooltips.ae2.SubstitutionsOff"),
            new TextComponentTranslation("gui.tooltips.ae2.SubstitutionsDescDisabled")));
        widgets.add("craftingSubstitutions", this.substitutionsBtn);

        this.fluidSubstitutionsBtn = new ToggleButton(Icon.S_FLUID_SUBSTITUTION_ENABLED,
            Icon.S_FLUID_SUBSTITUTION_DISABLED, this.container::setSubstituteFluids);
        this.fluidSubstitutionsBtn.setHalfSize(true);
        this.fluidSubstitutionsBtn.setDisableBackground(true);
        this.fluidSubstitutionsBtn.setTooltipOn(List.of(
            new TextComponentTranslation("gui.tooltips.ae2.FluidSubstitutions"),
            new TextComponentTranslation("gui.tooltips.ae2.FluidSubstitutionsDescEnabled")));
        this.fluidSubstitutionsBtn.setTooltipOff(List.of(
            new TextComponentTranslation("gui.tooltips.ae2.FluidSubstitutions"),
            new TextComponentTranslation("gui.tooltips.ae2.FluidSubstitutionsDescDisabled")));
        widgets.add("craftingFluidSubstitutions", this.fluidSubstitutionsBtn);
    }

    @Override
    Icon getIcon() {
        return Icon.TAB_CRAFTING;
    }

    @Override
    public ITextComponent getTabTooltip() {
        return new TextComponentTranslation("gui.ae2.CraftingPattern");
    }

    @Override
    public void drawBackgroundLayer(Rect2i bounds, Point mouse) {
        BG.dest(bounds.x() + this.position.x() - 1, bounds.y() + this.position.y() + 1).blit();

        if (this.container.substituteFluids && mouse.isIn(this.fluidSubstitutionsBtn.getTooltipArea())) {
            for (int slotIndex : this.container.slotsSupportingFluidSubstitution) {
                drawSlotGreenBG(bounds, this.container.getCraftingGridSlots()[slotIndex]);
            }
        }
    }

    private void drawSlotGreenBG(Rect2i bounds, Slot slot) {
        int x = bounds.x() + slot.xPos;
        int y = bounds.y() + slot.yPos;
        Gui.drawRect(x, y, x + 16, y + 16, 0xFF7AC25F);
    }

    @Override
    public void updateBeforeRender() {
        this.substitutionsBtn.setState(this.container.substitute);
        this.fluidSubstitutionsBtn.setState(this.container.substituteFluids);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        this.clearBtn.setVisibility(visible);
        this.substitutionsBtn.setVisibility(visible);
        this.fluidSubstitutionsBtn.setVisibility(visible);
        this.screen.setSlotsHidden(SlotSemantics.CRAFTING_GRID, !visible);
        this.screen.setSlotsHidden(SlotSemantics.CRAFTING_RESULT, !visible);
    }
}
