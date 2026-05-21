package appeng.hooks;

import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.implementations.tiles.IColorableTile;
import appeng.core.definitions.AEItems;
import appeng.core.network.InitNetwork;
import appeng.core.network.serverbound.ColorApplicatorSelectColorPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;

public final class ColorApplicatorPickColorHook {
    private ColorApplicatorPickColorHook() {
    }

    public static boolean onPickColor(EntityPlayer player, RayTraceResult hitResult) {
        if (!AEItems.COLOR_APPLICATOR.is(player.getHeldItemOffhand())
            && !AEItems.COLOR_APPLICATOR.is(player.getHeldItemMainhand())) {
            return false;
        }

        TileEntity tile = player.world.getTileEntity(hitResult.getBlockPos());
        if (tile instanceof IColorableBlockEntity colorableBlockEntity) {
            InitNetwork.sendToServer(new ColorApplicatorSelectColorPacket(colorableBlockEntity.getColor()));
            return true;
        }

        if (tile instanceof IColorableTile colorableTile) {
            InitNetwork.sendToServer(new ColorApplicatorSelectColorPacket(colorableTile.getColor()));
            return true;
        }

        return false;
    }
}
