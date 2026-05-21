package appeng.core.gui;

import appeng.container.GuiIds;
import appeng.core.AppEngBase;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.parts.AEBasePart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

import java.util.Objects;

public final class GuiOpener {

    private GuiOpener() {
    }

    public static void openGui(EntityPlayer player, GuiIds.GuiKey bridge, TileEntity tile) {
        openGui(player, bridge, tile, false);
    }

    public static void openGui(EntityPlayer player, GuiIds.GuiKey bridge, TileEntity tile, boolean returnedFromSubScreen) {
        net.minecraft.util.math.BlockPos pos = tile.getPos();
        player.openGui(AppEngBase.instance(), GuiIds.getGuiId(bridge, returnedFromSubScreen), tile.getWorld(),
            pos.getX(), pos.getY(), pos.getZ());
    }

    public static void openPartGui(EntityPlayer player, GuiIds.GuiKey key, AEBasePart part) {
        openPartGui(player, key, part, false);
    }

    public static void openPartGui(EntityPlayer player, GuiIds.GuiKey key, AEBasePart part,
                                   boolean returnedFromSubScreen) {
        BlockPos pos = part.getHost().getLocation().getPos();
        int side = Objects.requireNonNull(part.getSide(), "Part GUI requires a sided part").ordinal();
        player.openGui(AppEngBase.instance(), GuiIds.getGuiId(key, returnedFromSubScreen), player.world, pos.getX(),
            (side << 8) | (pos.getY() & 255), pos.getZ());
    }

    public static boolean openItemGui(EntityPlayer player, GuiIds.GuiKey key, ItemGuiHostLocator locator) {
        return openItemGui(player, key, locator, false);
    }

    public static boolean openItemGui(EntityPlayer player, GuiIds.GuiKey key, ItemGuiHostLocator locator,
                                      boolean returnedFromSubScreen) {
        Integer slot = locator.getPlayerInventorySlot();
        if (slot == null) {
            return false;
        }

        if (key == GuiIds.GuiKey.NETWORK_STATUS) {
            RayTraceResult hitResult = locator.hitResult();
            if (hitResult == null || hitResult.getBlockPos() == null) {
                return false;
            }
            openItemGui(player, key, slot, hitResult.getBlockPos(), returnedFromSubScreen);
            return true;
        }

        player.openGui(AppEngBase.instance(), GuiIds.getGuiId(key, returnedFromSubScreen), player.world, slot, 0, 0);
        return true;
    }

    public static void openItemGui(EntityPlayer player, GuiIds.GuiKey key, int slot, BlockPos pos) {
        openItemGui(player, key, slot, pos, false);
    }

    public static void openItemGui(EntityPlayer player, GuiIds.GuiKey key, int slot, BlockPos pos,
                                   boolean returnedFromSubScreen) {
        player.openGui(AppEngBase.instance(), GuiIds.getGuiId(key, returnedFromSubScreen), player.world, pos.getX(),
            (slot << 8) | (pos.getY() & 255), pos.getZ());
    }
}
