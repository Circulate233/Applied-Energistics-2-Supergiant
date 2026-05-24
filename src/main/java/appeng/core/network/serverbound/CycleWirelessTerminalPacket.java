package appeng.core.network.serverbound;

import appeng.api.implementations.items.WirelessTerminalDefinition;
import appeng.container.AEBaseContainer;
import appeng.core.gui.locator.GuiHostLocator;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.core.localization.GuiText;
import appeng.core.network.ServerboundPacket;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.items.tools.powered.WirelessTerminalRegistry;
import appeng.items.tools.powered.WirelessUniversalTerminalItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CycleWirelessTerminalPacket extends ServerboundPacket {
    private boolean reverse;

    public CycleWirelessTerminalPacket() {
    }

    public CycleWirelessTerminalPacket(boolean reverse) {
        this.reverse = reverse;
    }

    @Override
    protected void read(ByteBuf buf) {
        this.reverse = new PacketBuffer(buf).readBoolean();
    }

    @Override
    protected void write(ByteBuf buf) {
        new PacketBuffer(buf).writeBoolean(this.reverse);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (player.openContainer instanceof AEBaseContainer container) {
            cycleOpenContainer(player, container);
            return;
        }
        if (cycleHeldTerminal(player, EnumHand.MAIN_HAND)) {
            return;
        }
        cycleHeldTerminal(player, EnumHand.OFF_HAND);
    }

    private void cycleOpenContainer(EntityPlayerMP player, AEBaseContainer container) {
        GuiHostLocator locator = container.getLocator();
        if (!(locator instanceof ItemGuiHostLocator itemLocator)) {
            return;
        }

        ItemStack stack = itemLocator.locateItem(player);
        if (!(stack.getItem() instanceof WirelessUniversalTerminalItem universalTerminal)) {
            return;
        }
        if (!cycleStack(universalTerminal, stack)) {
            return;
        }

        WirelessTerminalItem selected = universalTerminal.getCurrentTerminal(stack);
        if (selected != null) {
            selected.getWirelessTerminalDefinition().open(player, itemLocator, stack, true);
        }
    }

    private boolean cycleHeldTerminal(EntityPlayerMP player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!(stack.getItem() instanceof WirelessUniversalTerminalItem universalTerminal)) {
            return false;
        }
        if (!cycleStack(universalTerminal, stack)) {
            return false;
        }

        WirelessTerminalItem selected = universalTerminal.getCurrentTerminal(stack);
        if (selected != null) {
            player.sendStatusMessage(
                GuiText.WirelessTerminalCurrent.text(selected.getWirelessTerminalDefinition().displayName()),
                true);
        }
        return true;
    }

    private boolean cycleStack(WirelessUniversalTerminalItem universalTerminal, ItemStack stack) {
        Set<String> installedIds = universalTerminal.getInstalledTerminalIds(stack);
        List<String> installed = new ArrayList<>();
        for (WirelessTerminalDefinition definition : WirelessTerminalRegistry.allDefinitions()) {
            if (installedIds.contains(definition.id())) {
                installed.add(definition.id());
            }
        }
        if (installed.size() < 2) {
            return false;
        }

        WirelessTerminalItem current = universalTerminal.getCurrentTerminal(stack);
        String currentId = current == null ? installed.getFirst() : current.getWirelessTerminalDefinition().id();
        int index = installed.indexOf(currentId);
        if (index < 0) {
            index = 0;
        }
        int nextIndex = this.reverse
            ? (index + installed.size() - 1) % installed.size()
            : (index + 1) % installed.size();
        return universalTerminal.selectTerminal(stack, installed.get(nextIndex));
    }
}
