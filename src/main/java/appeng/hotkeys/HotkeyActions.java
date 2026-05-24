package appeng.hotkeys;

import appeng.api.features.HotkeyAction;
import appeng.api.implementations.items.WirelessTerminalDefinition;
import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.ItemDefinition;
import appeng.items.tools.powered.AbstractPortableCell;
import appeng.items.tools.powered.WirelessTerminalRegistry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.Item;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static appeng.api.features.HotkeyAction.PORTABLE_FLUID_CELL;
import static appeng.api.features.HotkeyAction.PORTABLE_ITEM_CELL;

/**
 * Registry of {@link HotkeyAction}
 */
public final class HotkeyActions {
    public static final String WIRELESS_RESTOCK = "wireless_restock";
    public static final String WIRELESS_STOW = "wireless_stow";
    public static final String WIRELESS_MAGNET = "wireless_magnet";
    public static final Map<String, List<HotkeyAction>> REGISTRY = new Object2ObjectOpenHashMap<>();

    private HotkeyActions() {
    }

    public static void init() {
        for (WirelessTerminalDefinition definition : WirelessTerminalRegistry.allDefinitions()) {
            registerWirelessTerminal(definition);
        }

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

        register(new RestockHotkeyAction(), WIRELESS_RESTOCK);
        register(new StowHotkeyAction(), WIRELESS_STOW);
        register(new MagnetHotkeyAction(), WIRELESS_MAGNET);
    }

    public static void registerPortableCell(ItemDefinition<? extends AbstractPortableCell> cell, String id) {
        register(Objects.requireNonNull(cell.item()), (player, locator) -> cell.get().openFromInventory(player, locator),
            id);
    }

    private static void registerWirelessTerminal(WirelessTerminalDefinition definition) {
        register(new WirelessTerminalHotkeyAction(definition), definition.hotkeyName());
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
