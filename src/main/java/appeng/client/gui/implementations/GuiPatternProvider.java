package appeng.client.gui.implementations;

import appeng.api.config.BlockingMode;
import appeng.api.config.LockCraftingMode;
import appeng.api.config.PatternProviderBlockingType;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.Icon;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.PatternModifierPanelWidget;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.container.SlotSemantics;
import appeng.container.implementations.ContainerPatternProvider;
import appeng.container.slot.AppEngSlot;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.core.network.InitNetwork;
import appeng.core.network.bidirectional.ConfigValuePacket;
import appeng.helpers.patternprovider.PatternProviderCapacity;
import appeng.util.EnumCycler;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;

public class GuiPatternProvider extends AEBaseGui<ContainerPatternProvider> {

    private final SettingToggleButton<BlockingMode> blockingModeButton;
    private final SettingToggleButton<LockCraftingMode> lockCraftingModeButton;
    private final ToggleButton blockingTypeButton;
    private final ToggleButton showInPatternAccessTerminalButton;
    private final PatternProviderLockReason lockReason;
    private final PageButton previousPageButton;
    private final PageButton nextPageButton;
    private final PatternModifierPanelWidget patternModifierPanel;

    public GuiPatternProvider(ContainerPatternProvider container, InventoryPlayer playerInventory, ITextComponent unusedTitle,
                              GuiStyle style) {
        super(container, playerInventory, style);
        if (unusedTitle != null) {
            setTextContent(TEXT_ID_DIALOG_TITLE, unusedTitle);
        }

        this.blockingModeButton = addToLeftToolbar(new ServerSettingToggleButton<>(Settings.BLOCKING_MODE,
            BlockingMode.NO));
        this.blockingTypeButton = new ToggleButton(Icon.BLOCKING_MODE_TYPE_SMART, Icon.BLOCKING_MODE_TYPE_NORMAL,
            ignored -> selectBlockingType());
        this.blockingTypeButton.setTooltipOff(List.of(
            ButtonToolTips.PatternProviderBlockingType.text(),
            ButtonToolTips.NormalBlockingDescription.text()));
        this.blockingTypeButton.setTooltipOn(List.of(
            ButtonToolTips.PatternProviderBlockingType.text(),
            ButtonToolTips.SmartBlockingDescription.text()));
        addToLeftToolbar(this.blockingTypeButton);
        this.lockCraftingModeButton = addToLeftToolbar(
            new ServerSettingToggleButton<>(Settings.LOCK_CRAFTING_MODE, LockCraftingMode.NONE));
        this.widgets.addOpenPriorityButton();
        this.widgets.add("upgrades", UpgradesPanel.create(
            this.widgets,
            container.getSlots(SlotSemantics.UPGRADE),
            this::getCompatibleUpgrades));
        this.showInPatternAccessTerminalButton = new ToggleButton(Icon.PATTERN_ACCESS_SHOW,
            Icon.PATTERN_ACCESS_HIDE,
            GuiText.PatternAccessTerminal.text(),
            GuiText.PatternAccessTerminalHint.text(),
            this::selectPatternProviderMode);
        addToLeftToolbar(this.showInPatternAccessTerminalButton);
        this.lockReason = new PatternProviderLockReason(this);
        this.widgets.add("lockReason", this.lockReason);
        this.previousPageButton = new PageButton(Icon.ARROW_LEFT, () -> container.setPage(container.getCurrentPage() - 1));
        this.nextPageButton = new PageButton(Icon.ARROW_RIGHT, () -> container.setPage(container.getCurrentPage() + 1));
        this.widgets.add("previousPage", this.previousPageButton);
        this.widgets.add("nextPage", this.nextPageButton);
        this.patternModifierPanel = new PatternModifierPanelWidget(this, new PatternProviderPanelHost());
        this.patternModifierPanel.addButtons();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.repositionPatternPageSlots();

        this.lockReason.setVisible(this.container.getLockCraftingMode() != LockCraftingMode.NONE);
        this.blockingModeButton.set(this.container.getBlockingMode());
        this.blockingTypeButton.setVisibility(this.container.getBlockingMode() != BlockingMode.NO);
        this.blockingTypeButton.setState(this.container.getBlockingType() == PatternProviderBlockingType.SMART);
        this.lockCraftingModeButton.set(this.container.getLockCraftingMode());
        this.showInPatternAccessTerminalButton.setState(this.container.getShowInAccessTerminal() == YesNo.YES);
        this.previousPageButton.setVisibility(this.container.getPageCount() > 1 && this.container.getCurrentPage() > 0);
        this.nextPageButton.setVisibility(this.container.getPageCount() > 1
            && this.container.getCurrentPage() + 1 < this.container.getPageCount());
        this.patternModifierPanel.update();
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY, partialTicks);
        this.patternModifierPanel.drawBackground();
    }

    private void repositionPatternPageSlots() {
        int firstSlotOnPage = PatternProviderCapacity.getFirstSlotOnPage(this.container.getCurrentPage());
        for (Slot slot : this.container.getSlots(SlotSemantics.ENCODED_PATTERN)) {
            if (!slot.isEnabled() || !(slot instanceof AppEngSlot appEngSlot)) {
                continue;
            }

            int pageIndex = appEngSlot.getSlotIndex() - firstSlotOnPage;
            if (pageIndex < 0 || pageIndex >= PatternProviderCapacity.GUI_PATTERN_SLOTS_PER_PAGE) {
                continue;
            }

            slot.xPos = 8 + pageIndex % 9 * 18;
            slot.yPos = 42 + pageIndex / 9 * 18;
        }
    }

    private void selectBlockingType() {
        var nextValue = EnumCycler.rotateEnum(
            this.container.getBlockingType(),
            isHandlingRightClick(),
            Settings.PATTERN_PROVIDER_BLOCKING_TYPE.getValues());
        InitNetwork.CHANNEL.sendToServer(new ConfigValuePacket(Settings.PATTERN_PROVIDER_BLOCKING_TYPE, nextValue));
    }

    private void selectPatternProviderMode(boolean ignored) {
        InitNetwork.CHANNEL.sendToServer(new ConfigValuePacket(
            Settings.PATTERN_ACCESS_TERMINAL,
            EnumCycler.rotateEnum(
                this.container.getShowInAccessTerminal(),
                isHandlingRightClick(),
                Settings.PATTERN_ACCESS_TERMINAL.getValues())));
    }

    private List<ITextComponent> getCompatibleUpgrades() {
        var list = new ObjectArrayList<ITextComponent>();
        list.add(GuiText.CompatibleUpgrades.text());
        list.addAll(Upgrades.getTooltipLinesForInventory(this.container.getLogic().getUpgrades()));
        return list;
    }

    private static class PageButton extends IconButton {
        private final Icon icon;

        PageButton(Icon icon, Runnable onPress) {
            super(onPress);
            this.icon = icon;
            this.setMessage(new TextComponentTranslation(
                icon == Icon.ARROW_LEFT ? "gui.ae2.PatternProviderPagePrevious" : "gui.ae2.PatternProviderPageNext"));
        }

        @Override
        protected Icon getIcon() {
            return this.icon;
        }
    }

    private final class PatternProviderPanelHost implements PatternModifierPanelWidget.PanelHost {
        @Override
        public boolean isPatternModifierPanelAvailable() {
            return container.isPatternModifierPanelAvailable();
        }

        @Override
        public void updatePatternModifierPanelVisibleSlots(boolean visible) {
            container.updatePatternModifierPanelVisibleSlots(visible);
        }

        @Override
        public void clearPatternModifierPanel() {
            container.getPatternModifierPanel().clearPatterns();
        }

        @Override
        public void modifyPatternModifierPanelAmounts(int factor, boolean divide) {
            container.getPatternModifierPanel().modifyAmounts(factor, divide);
        }
    }
}
