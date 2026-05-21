package appeng.items.misc;

import appeng.client.render.model.MeteoriteCompassBakedModel;
import appeng.items.AEBaseItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MeteoriteCompassItem extends AEBaseItem {
    private static final String BEACON_TAG = "MeteoriteBeacon";

    public MeteoriteCompassItem() {
        super();
    }

    public static float getAnimatedRotation(@Nullable BlockPos playerPos, boolean prefetch, float playerRotation) {
        return MeteoriteCompassBakedModel.getAnimatedRotation(playerPos, prefetch, playerRotation);
    }

    public static boolean hasBeacon(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MeteoriteCompassItem
            && stack.hasTagCompound() && stack.getTagCompound().getBoolean(BEACON_TAG);
    }

    public static ItemStack createBeaconCompass(ItemStack baseStack) {
        ItemStack result = baseStack.copy();
        result.setCount(1);

        NBTTagCompound tag = result.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            result.setTagCompound(tag);
        }
        tag.setBoolean(BEACON_TAG, true);
        return result;
    }

    public static ItemStack createBeaconCompass() {
        return createBeaconCompass(new ItemStack(appeng.core.definitions.AEItems.METEORITE_COMPASS.item()));
    }

    @Override
    protected void addCheckedInformation(final ItemStack stack, final World world, final List<String> lines,
                                         final ITooltipFlag advancedTooltips) {
        super.addCheckedInformation(stack, world, lines, advancedTooltips);

        if (hasBeacon(stack)) {
            lines.add(TextFormatting.DARK_GRAY + I18n.format("item.ae2.meteorite_compass_beacon"));
            lines.add(TextFormatting.DARK_GRAY.toString() + TextFormatting.ITALIC
                + I18n.format("item.ae2.meteorite_compass_beacon_hint"));
        } else {
            lines.add(TextFormatting.DARK_GRAY.toString() + TextFormatting.ITALIC
                + I18n.format("item.ae2.meteorite_compass_beacon_upgrade_hint"));
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return hasBeacon(stack) || super.hasEffect(stack);
    }
}
