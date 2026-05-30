package appeng.container.implementations;

import appeng.api.config.Settings;
import appeng.api.stacks.AEKey;
import appeng.api.util.IConfigManager;
import appeng.core.definitions.AEItems;
import appeng.parts.automation.AnnihilationPlanePart;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerAnnihilationPlane extends UpgradeableContainer<AnnihilationPlanePart> {

    public ContainerAnnihilationPlane(InventoryPlayer ip, AnnihilationPlanePart host) {
        super(ip, host);
    }

    @Override
    protected void setupConfig() {
        addExpandableConfigSlots(getHost().getConfig());
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        if (supportsFuzzyMode()) {
            this.setFuzzyMode(cm.getSetting(Settings.FUZZY_MODE));
        }
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        int upgrades = getUpgrades().getInstalledUpgrades(AEItems.CAPACITY_CARD.item());
        return upgrades > idx;
    }

    public boolean supportsFuzzyMode() {
        if (!hasUpgrade(AEItems.FUZZY_CARD.item())) {
            return false;
        }
        for (AEKey key : getHost().getConfig().keySet()) {
            if (key.supportsFuzzyRangeSearch()) {
                return true;
            }
        }
        return false;
    }
}
