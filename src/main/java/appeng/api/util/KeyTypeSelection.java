package appeng.api.util;

import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.function.Predicate;

/**
 * Helper class to store the selection of key types.
 */
public class KeyTypeSelection {
    private final Listener listener;
    private final Object2BooleanLinkedOpenHashMap<AEKeyType> keyTypes = new Object2BooleanLinkedOpenHashMap<>();

    public KeyTypeSelection(Runnable listener, Predicate<AEKeyType> allowKeyType) {
        this(selection -> listener.run(), allowKeyType);
    }

    public KeyTypeSelection(Listener listener, Predicate<AEKeyType> allowKeyType) {
        this.listener = listener;
        for (var keyType : AEKeyTypes.getAll()) {
            if (allowKeyType.test(keyType)) {
                keyTypes.put(keyType, true);
            }
        }
    }

    public static KeyTypeSelection forStack(ItemStack stack, Predicate<AEKeyType> allowKeyType) {
        var out = new KeyTypeSelection(selection -> {
            var tag = stack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            selection.writeToNBT(tag);
        }, allowKeyType);
        var tag = stack.getTagCompound();
        if (tag != null) {
            out.readFromNBT(tag);
        }
        return out;
    }

    public void setEnabled(AEKeyType type, boolean enabled) {
        if (!keyTypes.containsKey(type)) {
            throw new IllegalArgumentException("Key type " + type + " is not allowed.");
        }

        // Disabling all key types is not allowed
        if (!enabled && enabledSet().size() <= 1) {
            return;
        }

        keyTypes.put(type, enabled);
        listener.onKeyTypeSelectionChanged(this);
    }

    public boolean isEnabled(AEKeyType type) {
        if (!keyTypes.containsKey(type)) {
            throw new IllegalArgumentException("Key type " + type + " is not allowed.");
        }

        return keyTypes.getBoolean(type);
    }

    public Object2BooleanMap<AEKeyType> enabled() {
        return new Object2BooleanLinkedOpenHashMap<>(keyTypes);
    }

    public List<AEKeyType> enabledSet() {
        List<AEKeyType> out = new ObjectArrayList<>(keyTypes.size());
        for (Object2BooleanMap.Entry<AEKeyType> entry : keyTypes.object2BooleanEntrySet()) {
            if (entry.getBooleanValue()) {
                out.add(entry.getKey());
            }
        }
        return out;
    }

    public void setEnabledSet(List<AEKeyType> selected) {
        for (Object2BooleanMap.Entry<AEKeyType> entry : keyTypes.object2BooleanEntrySet()) {
            entry.setValue(selected.contains(entry.getKey()));
        }
    }

    public Predicate<AEKeyType> enabledPredicate() {
        Object2BooleanMap<AEKeyType> snapshot = new Object2BooleanOpenHashMap<>(keyTypes);
        snapshot.defaultReturnValue(false);
        return snapshot::getBoolean;
    }

    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList enabledKeyTypesTag = new NBTTagList();
        for (Object2BooleanMap.Entry<AEKeyType> entry : keyTypes.object2BooleanEntrySet()) {
            if (entry.getBooleanValue()) {
                enabledKeyTypesTag.appendTag(new NBTTagString(entry.getKey().getId().toString()));
            }
        }
        AEComponents.ENABLED_KEY_TYPES_COMPONENT.writeTo(tag, enabledKeyTypesTag);
    }

    public void readFromNBT(NBTTagCompound tag) {
        for (Object2BooleanMap.Entry<AEKeyType> entry : keyTypes.object2BooleanEntrySet()) {
            entry.setValue(false);
        }
        NBTTagList enabledKeyTypesTag = AEComponents.ENABLED_KEY_TYPES_COMPONENT.readFrom(tag);
        if (enabledKeyTypesTag == null) {
            enabledKeyTypesTag = new NBTTagList();
        }
        if (enabledKeyTypesTag.tagCount() == 0 && tag.hasKey("enabledKeyTypes", 9)) {
            enabledKeyTypesTag = tag.getTagList("enabledKeyTypes", 8);
        }
        for (int i = 0; i < enabledKeyTypesTag.tagCount(); i++) {
            try {
                var keyType = AEKeyTypes.get(new ResourceLocation(enabledKeyTypesTag.getStringTagAt(i)));
                if (keyTypes.containsKey(keyType)) {
                    keyTypes.put(keyType, true);
                }
            } catch (IllegalArgumentException e) {
                // Ignore invalid key types
            }
        }

        // Make sure that one type is always enabled
        if (enabledSet().isEmpty()) {
            for (Object2BooleanMap.Entry<AEKeyType> entry : keyTypes.object2BooleanEntrySet()) {
                entry.setValue(true);
                break;
            }
        }
    }

    @FunctionalInterface
    public interface Listener {
        void onKeyTypeSelectionChanged(KeyTypeSelection keyTypeSelection);
    }
}
