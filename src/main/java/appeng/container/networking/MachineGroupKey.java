package appeng.container.networking;

import appeng.api.stacks.AEItemKey;
import net.minecraft.network.PacketBuffer;

record MachineGroupKey(AEItemKey display, boolean missingChannel) {
    public static MachineGroupKey fromPacket(PacketBuffer data) {
        return new MachineGroupKey(AEItemKey.fromPacket(data), data.readBoolean());
    }

    public void write(PacketBuffer data) {
        this.display.writeToPacket(data);
        data.writeBoolean(this.missingChannel);
    }
}
