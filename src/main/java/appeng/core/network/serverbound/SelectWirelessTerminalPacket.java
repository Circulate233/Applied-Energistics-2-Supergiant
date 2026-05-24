package appeng.core.network.serverbound;

import appeng.container.AEBaseContainer;
import appeng.core.gui.locator.GuiHostLocator;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.core.network.ServerboundPacket;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.items.tools.powered.WirelessUniversalTerminalItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class SelectWirelessTerminalPacket extends ServerboundPacket {
    private String terminalId;
    private int windowId;

    public SelectWirelessTerminalPacket() {
    }

    public SelectWirelessTerminalPacket(int windowId, String terminalId) {
        this.windowId = windowId;
        this.terminalId = terminalId;
    }

    @Override
    protected void read(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        this.windowId = packetBuffer.readVarInt();
        this.terminalId = ByteBufUtils.readUTF8String(packetBuffer);
    }

    @Override
    protected void write(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        packetBuffer.writeVarInt(this.windowId);
        ByteBufUtils.writeUTF8String(packetBuffer, this.terminalId);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (!(player.openContainer instanceof AEBaseContainer container)) {
            return;
        }
        if (container.windowId != this.windowId) {
            return;
        }
        GuiHostLocator locator = container.getLocator();
        if (!(locator instanceof ItemGuiHostLocator itemLocator)) {
            return;
        }
        ItemStack stack = itemLocator.locateItem(player);
        if (!(stack.getItem() instanceof WirelessUniversalTerminalItem universalTerminal)) {
            return;
        }
        if (!universalTerminal.selectTerminal(stack, this.terminalId)) {
            return;
        }
        WirelessTerminalItem selected = universalTerminal.getCurrentTerminal(stack);
        if (selected != null) {
            selected.getWirelessTerminalDefinition().open(player, itemLocator, stack, true);
        }
    }
}
