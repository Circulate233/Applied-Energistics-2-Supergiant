package appeng.helpers;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.container.GuiIds;
import appeng.container.ISubGui;
import appeng.core.gui.GuiOpener;
import appeng.core.gui.locator.GuiHostLocator;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.parts.AEBasePart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

public interface InterfaceLogicHost extends IConfigurableObject, IUpgradeableObject, IPriorityHost, IConfigInvHost {
    TileEntity getTileEntity();

    void saveChanges();

    InterfaceLogic getInterfaceLogic();

    @Override
    default IConfigManager getConfigManager() {
        return getInterfaceLogic().getConfigManager();
    }

    @Override
    default IUpgradeInventory getUpgrades() {
        return getInterfaceLogic().getUpgrades();
    }

    @Override
    default int getPriority() {
        return getInterfaceLogic().getPriority();
    }

    @Override
    default void setPriority(int newValue) {
        getInterfaceLogic().setPriority(newValue);
    }

    @Override
    default GenericStackInv getConfig() {
        return getInterfaceLogic().getConfig();
    }

    default GenericStackInv getStorage() {
        return getInterfaceLogic().getStorage();
    }

    default void openGui(EntityPlayer player, GuiHostLocator locator) {
        openGui(player, locator, false);
    }

    default void openGui(EntityPlayer player, GuiHostLocator ignoredLocator, boolean returnedFromSubScreen) {
        if (this instanceof AEBasePart part) {
            GuiOpener.openPartGui(player, GuiIds.GuiKey.INTERFACE, part, returnedFromSubScreen);
        } else {
            GuiOpener.openGui(player, GuiIds.GuiKey.INTERFACE, getTileEntity(), returnedFromSubScreen);
        }
    }

    @Override
    default void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        openGui(player, subGui.getLocator(), true);
    }
}
