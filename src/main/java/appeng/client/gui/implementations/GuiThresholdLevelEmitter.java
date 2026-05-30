package appeng.client.gui.implementations;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.container.implementations.ContainerThresholdLevelEmitter;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;

public class GuiThresholdLevelEmitter extends GuiUpgradeable<ContainerThresholdLevelEmitter> {
    private final SettingToggleButton<RedstoneMode> redstoneMode;
    private final SettingToggleButton<FuzzyMode> fuzzyMode;
    private final NumberEntryWidget upperLevel;
    private final NumberEntryWidget lowerLevel;

    public GuiThresholdLevelEmitter(ContainerThresholdLevelEmitter container, InventoryPlayer playerInventory,
                                    ITextComponent title, GuiStyle style) {
        super(container, playerInventory, title, style);
        this.redstoneMode = addToLeftToolbar(
            new ServerSettingToggleButton<>(Settings.REDSTONE_EMITTER, RedstoneMode.LOW_SIGNAL));
        this.fuzzyMode = addToLeftToolbar(new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL));
        this.upperLevel = widgets.addNumberEntryWidget("upperLevel", NumberEntryType.of(container.getConfiguredFilter()));
        this.upperLevel.setTextFieldStyle(style.getWidget("upperLevelInput"));
        this.upperLevel.setLongValue(this.container.getUpperValue());
        this.upperLevel.setOnChange(this::saveUpperValue);
        this.upperLevel.setOnConfirm(this::onClose);
        this.lowerLevel = widgets.addNumberEntryWidget("lowerLevel", NumberEntryType.of(container.getConfiguredFilter()));
        this.lowerLevel.setTextFieldStyle(style.getWidget("lowerLevelInput"));
        this.lowerLevel.setLongValue(this.container.getLowerValue());
        this.lowerLevel.setOnChange(this::saveLowerValue);
        this.lowerLevel.setOnConfirm(this::onClose);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.upperLevel.setType(NumberEntryType.of(container.getConfiguredFilter()));
        this.lowerLevel.setType(NumberEntryType.of(container.getConfiguredFilter()));
        this.redstoneMode.set(container.getRedStoneMode());
        this.fuzzyMode.set(container.getFuzzyMode());
        this.fuzzyMode.setVisibility(container.supportsFuzzySearch());
    }

    private void onClose() {
        if (this.mc.player != null) {
            this.mc.player.closeScreen();
        }
    }

    private void saveUpperValue() {
        this.upperLevel.getLongValue().ifPresent(container::setUpperValue);
    }

    private void saveLowerValue() {
        this.lowerLevel.getLongValue().ifPresent(container::setLowerValue);
    }
}
