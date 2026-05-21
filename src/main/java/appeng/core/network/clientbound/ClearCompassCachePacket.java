package appeng.core.network.clientbound;

import appeng.core.network.ClientboundPacket;
import appeng.hooks.CompassManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ClearCompassCachePacket extends ClientboundPacket {

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
        CompassManager.INSTANCE.clear();
    }
}
