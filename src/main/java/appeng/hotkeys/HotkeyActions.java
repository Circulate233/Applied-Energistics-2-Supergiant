package appeng.hotkeys;

import appeng.api.features.HotkeyAction;
import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.ItemDefinition;
import appeng.items.tools.powered.AbstractPortableCell;
import net.minecraft.item.Item;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static appeng.api.features.HotkeyAction.PORTABLE_FLUID_CELL;
import static appeng.api.features.HotkeyAction.PORTABLE_ITEM_CELL;
import static appeng.api.features.HotkeyAction.WIRELESS_TERMINAL;

/**
 * Registry of {@link HotkeyAction}
 */
public final class HotkeyActions {
    public static final Map<String, List<HotkeyAction>> REGISTRY = new Object2ObjectOpenHashMap<>();

    private HotkeyActions() {
    }

    public static void init() {
        register(Objects.requireNonNull(AEItems.WIRELESS_TERMINAL.item()),
            (player, locator) -> AEItems.WIRELESS_TERMINAL.get().openFromInventory(player, locator),
            WIRELESS_TERMINAL);
        register(Objects.requireNonNull(AEItems.WIRELESS_CRAFTING_TERMINAL.item()),
            (player, locator) -> AEItems.WIRELESS_CRAFTING_TERMINAL.get().openFromInventory(player, locator),
            WIRELESS_TERMINAL);

        registerPortableCell(AEItems.PORTABLE_ITEM_CELL1K, PORTABLE_ITEM_CELL);
        registerPortableCell(AEItems.PORTABLE_ITEM_CELL4K, PORTABLE_ITEM_CELL);
        registerPortableCell(AEItems.PORTABLE_ITEM_CELL16K, PORTABLE_ITEM_CELL);
        registerPortableCell(AEItems.PORTABLE_ITEM_CELL64K, PORTABLE_ITEM_CELL);
        registerPortableCell(AEItems.PORTABLE_ITEM_CELL256K, PORTABLE_ITEM_CELL);

        registerPortableCell(AEItems.PORTABLE_FLUID_CELL1K, PORTABLE_FLUID_CELL);
        registerPortableCell(AEItems.PORTABLE_FLUID_CELL4K, PORTABLE_FLUID_CELL);
        registerPortableCell(AEItems.PORTABLE_FLUID_CELL16K, PORTABLE_FLUID_CELL);
        registerPortableCell(AEItems.PORTABLE_FLUID_CELL64K, PORTABLE_FLUID_CELL);
        registerPortableCell(AEItems.PORTABLE_FLUID_CELL256K, PORTABLE_FLUID_CELL);
    }

    public static void registerPortableCell(ItemDefinition<? extends AbstractPortableCell> cell, String id) {
        register(Objects.requireNonNull(cell.item()), (player, locator) -> cell.get().openFromInventory(player, locator),
            id);
    }

    public static void register(Item item, InventoryHotkeyAction.Opener opener, String id) {
        register(new InventoryHotkeyAction(item, opener), id);
        register(new BaublesHotkeyAction(item, opener), id);
    }

    public static synchronized void register(HotkeyAction hotkeyAction, String id) {
        List<HotkeyAction> actions = REGISTRY.get(id);
        if (actions == null) {
            actions = new ObjectArrayList<>();
            REGISTRY.put(id, actions);
            AppEng.instance().registerHotkey(id);
        }

        actions.addFirst(hotkeyAction);
    }
}
