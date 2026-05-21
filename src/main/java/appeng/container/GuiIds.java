package appeng.container;

import org.jspecify.annotations.Nullable;

public final class GuiIds {

    public static final int CONTROLLER_STATUS = GuiKey.CONTROLLER_STATUS.getGuiId();
    public static final int ME_CHEST = GuiKey.ME_CHEST.getGuiId();
    private static final int RETURNED_FROM_SUBSCREEN_FLAG = 1 << 30;

    private GuiIds() {
    }

    public static int getGuiId(GuiKey key, boolean returnedFromSubScreen) {
        return returnedFromSubScreen ? key.getGuiId() | RETURNED_FROM_SUBSCREEN_FLAG : key.getGuiId();
    }

    public static boolean isReturnedFromSubScreen(int guiId) {
        return (guiId & RETURNED_FROM_SUBSCREEN_FLAG) != 0;
    }

    private static int getBaseGuiId(int guiId) {
        return guiId & ~RETURNED_FROM_SUBSCREEN_FLAG;
    }

    public enum GuiKey {
        CONTROLLER_STATUS(1),
        ME_CHEST(2),
        DRIVE(3),
        CELL_WORKBENCH(4),
        CONDENSER(5),
        SKY_CHEST(6),
        INSCRIBER(7),
        IO_PORT(8),
        MOLECULAR_ASSEMBLER(9),
        VIBRATION_CHAMBER(10),
        QNB(11),
        WIRELESS_ACCESS_POINT(12),
        SPATIAL_IO_PORT(13),
        SPATIAL_ANCHOR(14),
        QUARTZ_KNIFE(15),
        NETWORK_TOOL(16),
        NETWORK_STATUS(17),
        INTERFACE(18),
        PATTERN_PROVIDER(19),
        CRAFTING_CPU(20),
        IMPORT_BUS(21),
        EXPORT_BUS(22),
        STORAGE_BUS(23),
        FORMATION_PLANE(24),
        ENERGY_LEVEL_EMITTER(25),
        STORAGE_LEVEL_EMITTER(26),
        ME_STORAGE_TERMINAL(27),
        CRAFTING_TERMINAL(28),
        PATTERN_ENCODING_TERMINAL(29),
        PATTERN_ACCESS_TERMINAL(30),
        PORTABLE_ITEM_CELL(31),
        PORTABLE_FLUID_CELL(32),
        WIRELESS_TERMINAL(33),
        WIRELESS_CRAFTING_TERMINAL(34),
        CRAFT_AMOUNT(35),
        CRAFT_CONFIRM(36),
        CRAFTING_STATUS(37),
        SET_STOCK_AMOUNT(38),
        PRIORITY(39),
        BASIC_CELL_CHEST(40);

        private final int guiId;

        GuiKey(int guiId) {
            this.guiId = guiId;
        }

        public static @Nullable GuiKey fromId(int guiId) {
            int baseGuiId = getBaseGuiId(guiId);
            for (GuiKey bridge : values()) {
                if (bridge.guiId == baseGuiId) {
                    return bridge;
                }
            }
            return null;
        }

        public int getGuiId() {
            return this.guiId;
        }
    }
}
