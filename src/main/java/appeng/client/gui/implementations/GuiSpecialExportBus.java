package appeng.client.gui.implementations;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.style.GuiStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.container.implementations.UpgradeableContainer;
import appeng.core.definitions.AEItems;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;

public class GuiSpecialExportBus<T extends UpgradeableContainer<?>> extends GuiUpgradeable<T> {
    private final SettingToggleButton<RedstoneMode> redstoneMode;

    public GuiSpecialExportBus(T container, InventoryPlayer playerInventory, ITextComponent title, GuiStyle style) {
        super(container, playerInventory, title, style);
        this.redstoneMode = addToLeftToolbar(
            new ServerSettingToggleButton<>(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.redstoneMode.set(container.getRedStoneMode());
        this.redstoneMode.setVisibility(container.hasUpgrade(AEItems.REDSTONE_CARD.item()));
    }
}
