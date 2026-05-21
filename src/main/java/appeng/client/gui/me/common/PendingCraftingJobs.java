package appeng.client.gui.me.common;

import appeng.api.client.AEKeyRendering;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.stacks.AEKey;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.network.clientbound.CraftingJobStatusPacket;
import appeng.util.SearchInventoryEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.UUID;

/**
 * Tracks pending crafting jobs started by this player.
 */
@SideOnly(Side.CLIENT)
public final class PendingCraftingJobs {
    private static final Map<UUID, PendingJob> jobs = new Object2ObjectOpenHashMap<>();

    private PendingCraftingJobs() {
    }

    public static boolean hasPendingJob(AEKey what) {
        return jobs.entrySet().stream().anyMatch(s -> s.getValue().what().equals(what));
    }

    public static void clearPendingJobs() {
        jobs.clear();
    }

    public static void jobStatus(UUID id,
                                 AEKey what,
                                 long requestedAmount,
                                 long remainingAmount,
                                 CraftingJobStatusPacket.Status status) {

        AELog.debug("Crafting job " + id + " for " + requestedAmount
            + "x" + AEKeyRendering.getDisplayName(what).getUnformattedText() + ". State=" + status);

        PendingJob existing = jobs.get(id);
        switch (status) {
            case STARTED:
                if (existing == null) {
                    jobs.put(id, new PendingJob(id, what, requestedAmount, remainingAmount));
                }
                break;
            case CANCELLED:
                jobs.remove(id);
                break;
            case FINISHED:
                jobs.remove(id);
                Minecraft minecraft = Minecraft.getMinecraft();
                if (AEConfig.instance().isNotifyForFinishedCraftingJobs()
                    && !(minecraft.currentScreen instanceof GuiMEStorage)
                    && minecraft.player != null && hasNotificationEnablingItem(minecraft.player)) {
                    minecraft.getToastGui().add(new FinishedJobToast(what, requestedAmount));
                }
                break;
            default:
                break;
        }
    }

    private static boolean hasNotificationEnablingItem(EntityPlayerSP player) {
        for (ItemStack stack : SearchInventoryEvent.getItems(player)) {
            net.minecraft.nbt.NBTTagCompound tag = stack.getTagCompound();
            if (!stack.isEmpty()
                && stack.getItem() instanceof IAEItemPowerStorage
                && ((IAEItemPowerStorage) stack.getItem()).getAECurrentPower(stack) > 0
                && AEComponents.WIRELESS_LINK_TARGET_COMPONENT.isPresentIn(tag)) {
                return true;
            }
        }
        return false;
    }

    private record PendingJob(UUID jobId, AEKey what, long requestedAmount, long remainingAmount) {
    }
}

