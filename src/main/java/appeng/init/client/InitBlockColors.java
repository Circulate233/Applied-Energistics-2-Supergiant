package appeng.init.client;

import appeng.api.util.AEColor;
import appeng.block.networking.CableBusColor;
import appeng.client.render.ColorableBlockEntityBlockColor;
import appeng.client.render.StaticBlockColor;
import appeng.core.definitions.AEBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.BlockColors;

public final class InitBlockColors {
    private InitBlockColors() {
    }

    public static void init() {
        BlockColors blockColors = Minecraft.getMinecraft().getBlockColors();
        blockColors.registerBlockColorHandler(new StaticBlockColor(AEColor.TRANSPARENT),
            AEBlocks.WIRELESS_ACCESS_POINT.block());
        blockColors.registerBlockColorHandler(new CableBusColor(), AEBlocks.CABLE_BUS.block());
        blockColors.registerBlockColorHandler(ColorableBlockEntityBlockColor.INSTANCE, AEBlocks.ME_CHEST.block());
    }
}
