package ae2.client.gui.me.crafting;

import ae2.api.stacks.GenericStack;
import ae2.client.gui.AEBaseGui;
import ae2.client.gui.Icon;
import ae2.client.gui.NumberEntryType;
import ae2.client.gui.me.common.ClientDisplaySlot;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.GuiNumberEntryButtonSettings;
import ae2.client.gui.widgets.NumberEntryButtonConfigButton;
import ae2.client.gui.widgets.NumberEntryWidget;
import ae2.client.gui.widgets.TabButton;
import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2.core.localization.GuiText;
import net.minecraft.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;
import java.util.function.IntConsumer;

/**
 * Edits the priority of the crafting task associated with a parent crafting screen.
 */
public class GuiCraftingTaskPriority extends AEBaseGui<AEBaseContainer> {
    private final AEBaseGui<?> parent;
    private final IntConsumer setter;
    private final NumberEntryWidget priority;
    @Nullable
    private final GenericStack finalOutput;
    @Nullable
    private Slot displaySlot;

    public GuiCraftingTaskPriority(AEBaseGui<?> parent, int currentPriority, IntConsumer setter,
                                   @Nullable GenericStack finalOutput) {
        super(parent.getContainer(), parent.getContainer().getPlayerInventory(),
            GuiStyleManager.loadStyleDoc("/screens/crafting_task_priority.json"));
        this.parent = parent;
        this.setter = setter;
        this.finalOutput = finalOutput;

        widgets.addButton("save", GuiText.Set.text(), this::confirm);
        widgets.add("back", new TabButton(Icon.BACK, GuiText.ReturnToPreviousGui.text(), this::returnToParent));
        widgets.add("numberEntryButtonConfig", new NumberEntryButtonConfigButton(this::openNumberEntryButtonSettings));

        this.priority = widgets.addNumberEntryWidget("priority", NumberEntryType.UNITLESS);
        this.priority.setMinValue(Integer.MIN_VALUE);
        this.priority.setMaxValue(Integer.MAX_VALUE);
        this.priority.setLongValue(currentPriority);
        GuiStyle currentStyle = getStyle();
        if (currentStyle == null) {
            throw new IllegalStateException("GUI style has not been initialized");
        }
        this.priority.setTextFieldStyle(currentStyle.getWidget("priorityInput"));
        this.priority.setPreviewFieldStyle(currentStyle.getWidget("priorityPreview"));
        this.priority.setOnConfirm(this::confirm);

        setSlotsHidden(SlotSemantics.TOOLBOX, true);
        setSlotsHidden(SlotSemantics.PLAYER_INVENTORY, true);
        setSlotsHidden(SlotSemantics.PLAYER_HOTBAR, true);
    }

    @Override
    public void initGui() {
        ensureDisplaySlot();
        super.initGui();
    }

    private void confirm() {
        OptionalInt value = this.priority.getIntValue();
        if (value.isPresent()) {
            this.setter.accept(value.getAsInt());
            returnToParent();
        }
    }

    private void openNumberEntryButtonSettings() {
        switchToScreen(new GuiNumberEntryButtonSettings(this));
    }

    private void removeDisplaySlot() {
        if (this.displaySlot != null && getContainer().isClientSideSlot(this.displaySlot)) {
            getContainer().removeClientSideSlot(this.displaySlot);
        }
        this.displaySlot = null;
    }

    private void ensureDisplaySlot() {
        if (this.displaySlot != null && getContainer().isClientSideSlot(this.displaySlot)) {
            return;
        }
        this.displaySlot = getContainer().addClientSideSlot(new ClientDisplaySlot(this.finalOutput),
            SlotSemantics.MACHINE_OUTPUT);
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
