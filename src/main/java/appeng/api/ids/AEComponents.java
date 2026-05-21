package appeng.api.ids;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.Nullable;

public final class AEComponents {
    public static final ComponentKey<NBTTagString> EXPORTED_SETTINGS_SOURCE_COMPONENT = string("exported_settings_source");
    public static final ComponentKey<NBTTagString> EXPORTED_CUSTOM_NAME_COMPONENT = string("exported_custom_name");
    public static final ComponentKey<NBTTagList> EXPORTED_UPGRADES_COMPONENT = list("exported_upgrades");
    public static final ComponentKey<NBTTagCompound> EXPORTED_SETTINGS_COMPONENT = compound("exported_settings");
    public static final ComponentKey<NBTBase> EXPORTED_PRIORITY_COMPONENT = numeric("exported_priority");
    public static final ComponentKey<NBTTagString> EXPORTED_P2P_TYPE_COMPONENT = string("exported_p2p_type");
    public static final ComponentKey<NBTBase> EXPORTED_P2P_FREQUENCY_COMPONENT = numeric("exported_p2p_frequency");
    public static final ComponentKey<NBTBase> MEMORY_CARD_COLORS_COMPONENT = any("memory_card_colors");
    public static final ComponentKey<NBTTagList> EXPORTED_CONFIG_INV_COMPONENT = list("exported_config_inv");
    public static final ComponentKey<NBTBase> EXPORTED_LEVEL_EMITTER_VALUE_COMPONENT = numeric("exported_level_emitter_value");
    public static final ComponentKey<NBTTagList> EXPORTED_PATTERNS_COMPONENT = list("exported_patterns");
    public static final ComponentKey<NBTBase> EXPORTED_PUSH_DIRECTION_COMPONENT = numeric("pushDirection");
    public static final ComponentKey<NBTTagString> NAME_PRESS_NAME_COMPONENT = string("name_press_name");
    public static final ComponentKey<NBTTagList> UPGRADES_COMPONENT = list("upgrades");
    public static final ComponentKey<NBTBase> ENTANGLED_SINGULARITY_ID_COMPONENT = numeric("entangled_singularity_id");
    public static final ComponentKey<NBTBase> STORED_ENERGY_COMPONENT = numeric("stored_energy");
    public static final ComponentKey<NBTBase> ENERGY_CAPACITY_COMPONENT = numeric("energy_capacity");
    public static final ComponentKey<NBTTagCompound> ENCODED_CRAFTING_PATTERN_COMPONENT = compound("encoded_crafting_pattern");
    public static final ComponentKey<NBTTagCompound> ENCODED_PROCESSING_PATTERN_COMPONENT = compound("encoded_processing_pattern");
    public static final ComponentKey<NBTTagList> ENABLED_KEY_TYPES_COMPONENT = list("enabled_key_types");
    public static final ComponentKey<NBTTagCompound> WIRELESS_LINK_TARGET_COMPONENT = compound("wireless_link_target");
    public static final ComponentKey<NBTTagString> SELECTED_COLOR_COMPONENT = string("selected_color");
    public static final ComponentKey<NBTTagString> STORAGE_CELL_FUZZY_MODE_COMPONENT = string("storage_cell_fuzzy_mode");
    public static final ComponentKey<NBTTagList> STORAGE_CELL_INV_COMPONENT = list("storage_cell_inv");
    public static final ComponentKey<NBTTagList> STORAGE_CELL_CONFIG_INV_COMPONENT = list("storage_cell_config_inv");
    public static final ComponentKey<NBTTagCompound> MISSING_CONTENT_ITEMSTACK_DATA_COMPONENT = compound("missing_content_itemstack_data");
    public static final ComponentKey<NBTTagCompound> MISSING_CONTENT_AEKEY_DATA_COMPONENT = compound("missing_content_aekey_data");
    public static final ComponentKey<NBTTagString> MISSING_CONTENT_ERROR_COMPONENT = string("missing_content_error");
    public static final ComponentKey<NBTTagCompound> CRAFTING_INV_COMPONENT = compound("crafting_inv");
    public static final ComponentKey<NBTTagString> FACADE_CYCLE_PROPERTY_COMPONENT = string("facade_cycle_property");
    public static final ComponentKey<NBTTagString> FACADE_ITEM_COMPONENT = string("facade_item");
    public static final ComponentKey<NBTTagCompound> SPATIAL_PLOT_INFO_COMPONENT = compound("spatial_plot_info");
    public static final ComponentKey<NBTTagCompound> WRAPPED_STACK_COMPONENT = compound("wrapped_stack");
    public static final String EXPORTED_SETTINGS_SOURCE = EXPORTED_SETTINGS_SOURCE_COMPONENT.name();
    public static final String EXPORTED_CUSTOM_NAME = EXPORTED_CUSTOM_NAME_COMPONENT.name();
    public static final String EXPORTED_UPGRADES = EXPORTED_UPGRADES_COMPONENT.name();
    public static final String EXPORTED_SETTINGS = EXPORTED_SETTINGS_COMPONENT.name();
    public static final String EXPORTED_PRIORITY = EXPORTED_PRIORITY_COMPONENT.name();
    public static final String EXPORTED_P2P_TYPE = EXPORTED_P2P_TYPE_COMPONENT.name();
    public static final String EXPORTED_P2P_FREQUENCY = EXPORTED_P2P_FREQUENCY_COMPONENT.name();
    public static final String MEMORY_CARD_COLORS = MEMORY_CARD_COLORS_COMPONENT.name();
    public static final String EXPORTED_CONFIG_INV = EXPORTED_CONFIG_INV_COMPONENT.name();
    public static final String EXPORTED_LEVEL_EMITTER_VALUE = EXPORTED_LEVEL_EMITTER_VALUE_COMPONENT.name();
    public static final String EXPORTED_PATTERNS = EXPORTED_PATTERNS_COMPONENT.name();
    public static final String EXPORTED_PUSH_DIRECTION = EXPORTED_PUSH_DIRECTION_COMPONENT.name();
    public static final String NAME_PRESS_NAME = NAME_PRESS_NAME_COMPONENT.name();
    public static final String UPGRADES = UPGRADES_COMPONENT.name();
    public static final String ENTANGLED_SINGULARITY_ID = ENTANGLED_SINGULARITY_ID_COMPONENT.name();
    public static final String STORED_ENERGY = STORED_ENERGY_COMPONENT.name();
    public static final String ENCODED_CRAFTING_PATTERN = ENCODED_CRAFTING_PATTERN_COMPONENT.name();
    public static final String ENCODED_PROCESSING_PATTERN = ENCODED_PROCESSING_PATTERN_COMPONENT.name();
    public static final String ENABLED_KEY_TYPES = ENABLED_KEY_TYPES_COMPONENT.name();
    public static final String WIRELESS_LINK_TARGET = WIRELESS_LINK_TARGET_COMPONENT.name();
    public static final String STORAGE_CELL_FUZZY_MODE = STORAGE_CELL_FUZZY_MODE_COMPONENT.name();
    public static final String CRAFTING_INV = CRAFTING_INV_COMPONENT.name();
    public static final String FACADE_CYCLE_PROPERTY = FACADE_CYCLE_PROPERTY_COMPONENT.name();
    public static final String FACADE_ITEM = FACADE_ITEM_COMPONENT.name();
    public static final String SPATIAL_PLOT_INFO = SPATIAL_PLOT_INFO_COMPONENT.name();
    public static final String WRAPPED_STACK = WRAPPED_STACK_COMPONENT.name();

    private AEComponents() {
    }

    public static ComponentKey<NBTTagCompound> compound(String name) {
        return new ComponentKey<>(name, Constants.NBT.TAG_COMPOUND, NBTTagCompound.class);
    }

    public static ComponentKey<NBTTagList> list(String name) {
        return new ComponentKey<>(name, Constants.NBT.TAG_LIST, NBTTagList.class);
    }

    public static ComponentKey<NBTTagString> string(String name) {
        return new ComponentKey<>(name, Constants.NBT.TAG_STRING, NBTTagString.class);
    }

    public static ComponentKey<NBTBase> numeric(String name) {
        return new ComponentKey<>(name, Constants.NBT.TAG_ANY_NUMERIC, NBTBase.class);
    }

    public static ComponentKey<NBTBase> any(String name) {
        return new ComponentKey<>(name, -1, NBTBase.class);
    }

    public static final class ComponentKey<T extends NBTBase> {
        private final String name;
        private final int tagType;
        private final Class<T> valueClass;

        private ComponentKey(String name, int tagType, Class<T> valueClass) {
            this.name = name;
            this.tagType = tagType;
            this.valueClass = valueClass;
        }

        public String name() {
            return name;
        }

        public boolean isPresentIn(@Nullable NBTTagCompound tag) {
            return tag != null && (tagType == -1 ? tag.hasKey(name) : tag.hasKey(name, tagType));
        }

        @Nullable
        public T readFrom(@Nullable NBTTagCompound tag) {
            if (!isPresentIn(tag)) {
                return null;
            }
            return copy(tag.getTag(name));
        }

        public void writeTo(NBTTagCompound tag, T value) {
            tag.setTag(name, value.copy());
        }

        @Nullable
        public T copy(@Nullable NBTBase value) {
            if (!valueClass.isInstance(value)) {
                return null;
            }
            return valueClass.cast(value.copy());
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
