package appeng.client.gui.me.items;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.Icon;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.style.GuiStyleManager;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;
import appeng.container.AEBaseContainer;
import appeng.container.SlotSemantics;
import appeng.core.localization.GuiText;
import appeng.text.TextComponentItemStack;
import com.google.common.primitives.Longs;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;

public class GuiSetProcessingPatternAmount extends AEBaseGui<AEBaseContainer> {
    private final AEBaseGui<?> parent;
    private final GenericStack currentStack;
    private final Consumer<GenericStack> setter;
    private final NumberEntryWidget amount;
    private final Slot displaySlot;

    public GuiSetProcessingPatternAmount(GuiPatternEncodingTerm parent, GenericStack currentStack,
                                         Consumer<GenericStack> setter) {
        this(parent, currentStack, setter,
            TextComponentItemStack.of(parent.getContainer().getHost().getMainContainerIcon()));
    }

    public GuiSetProcessingPatternAmount(AEBaseGui<?> parent, GenericStack currentStack,
                                         Consumer<GenericStack> setter, ITextComponent parentIconTooltip) {
        super(parent.getContainer(), parent.getContainer().getPlayerInventory(),
            GuiStyleManager.loadStyleDoc("/screens/set_processing_pattern_amount.json"));
        this.parent = parent;
        this.currentStack = currentStack;
        this.setter = setter;

        widgets.addButton("save", GuiText.Set.text(), this::confirm);
        widgets.add("back", new TabButton(Icon.BACK, parentIconTooltip, this::returnToParent));

        this.amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.of(currentStack.what()));
        this.amount.setLongValue(currentStack.amount());
        this.amount.setMaxValue(getMaxAmount());
        GuiStyle currentStyle = getStyle();
        if (currentStyle == null) {
            throw new IllegalStateException("GUI style has not been initialized");
        }
        this.amount.setTextFieldStyle(currentStyle.getWidget("amountToStockInput"));
        this.amount.setMinValue(0);
        this.amount.setHideValidationIcon(true);
        this.amount.setOnConfirm(this::confirm);

        this.displaySlot = getContainer().addClientSideSlot(new ClientDisplaySlot(currentStack),
            SlotSemantics.MACHINE_OUTPUT);
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
    }

    private void confirm() {
        this.amount.getLongValue().ifPresent(newAmount -> {
            long constrained = Longs.constrainToRange(newAmount, 0, getMaxAmount());
            this.setter.accept(constrained <= 0 ? null : new GenericStack(this.currentStack.what(), constrained));
            returnToParent();
        });
    }

    private long getMaxAmount() {
        return 2147483647L;
    }

    private void removeDisplaySlot() {
        if (getContainer().isClientSideSlot(this.displaySlot)) {
            getContainer().removeClientSideSlot(this.displaySlot);
        }
    }

    private void returnToParent() {
        removeDisplaySlot();
        switchToScreen(this.parent);
        this.parent.returnFromSubScreen(this);
    }

    @Override
    public void onGuiClosed() {
        removeDisplaySlot();
        super.onGuiClosed();
    }

}
