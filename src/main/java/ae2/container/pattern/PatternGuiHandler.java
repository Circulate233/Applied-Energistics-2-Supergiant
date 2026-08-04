package ae2.container.pattern;

import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.container.GuiIds;
import ae2.core.AppEngBase;
import ae2.crafting.pattern.AECraftingPattern;
import ae2.crafting.pattern.AEProcessingPattern;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PatternGuiHandler {

    private static final Map<UUID, ItemStack> SERVER_PATTERNS = new ConcurrentHashMap<>();
    private static ItemStack clientPattern = ItemStack.EMPTY;

    private PatternGuiHandler() {
    }

    public static boolean open(EntityPlayerMP player, ItemStack pattern) {
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, player.world);
        if (!isSupported(details)) {
            return false;
        }

        SERVER_PATTERNS.put(player.getUniqueID(), pattern.copy());
        player.openGui(AppEngBase.instance(), GuiIds.GuiKey.PATTERN_VIEW.getGuiId(), player.world, 0, 0, 0);
        return true;
    }

    public static void prepareClient(ItemStack pattern) {
        clientPattern = pattern.copy();
    }

    @Nullable
    public static ContainerPattern createServerContainer(InventoryPlayer playerInventory) {
        ItemStack pattern = SERVER_PATTERNS.remove(playerInventory.player.getUniqueID());
        return createContainer(playerInventory, pattern);
    }

    @Nullable
    public static ContainerPattern createClientContainer(InventoryPlayer playerInventory) {
        ItemStack pattern = clientPattern;
        clientPattern = ItemStack.EMPTY;
        return createContainer(playerInventory, pattern);
    }

    private static boolean isSupported(@Nullable IPatternDetails details) {
        return details instanceof AECraftingPattern || details instanceof AEProcessingPattern;
    }

    @Nullable
    private static ContainerPattern createContainer(InventoryPlayer playerInventory, @Nullable ItemStack pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return null;
        }

        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, playerInventory.player.world);
        if (details instanceof AECraftingPattern) {
            return new ContainerCraftingPattern(playerInventory, pattern);
        }
        if (details instanceof AEProcessingPattern) {
            return new ContainerProcessingPattern(playerInventory, pattern);
        }
        return null;
    }
}
