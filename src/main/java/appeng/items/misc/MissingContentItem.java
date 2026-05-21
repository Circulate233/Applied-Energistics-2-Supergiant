package appeng.items.misc;

import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypesInternal;
import appeng.api.stacks.GenericStack;
import appeng.items.AEBaseItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MissingContentItem extends AEBaseItem {
    public MissingContentItem() {
        super();
    }

    @Nullable
    public BrokenStackInfo getBrokenStackInfo(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return null;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return null;
        }

        NBTTagCompound itemStackData = AEComponents.MISSING_CONTENT_ITEMSTACK_DATA_COMPONENT.readFrom(tag);
        NBTTagCompound genericStackData = AEComponents.MISSING_CONTENT_AEKEY_DATA_COMPONENT.readFrom(tag);

        if (itemStackData != null
            && itemStackData.hasKey("id", Constants.NBT.TAG_STRING)) {
            String missingId = itemStackData.getString("id");
            long amount = Math.max(1, itemStackData.getByte("Count") & 255);
            return new BrokenStackInfo(new TextComponentString(missingId), AEKeyType.items(), amount);
        }

        if (genericStackData != null) {
            String missingName = null;
            if (genericStackData.hasKey("id", Constants.NBT.TAG_STRING)) {
                missingName = genericStackData.getString("id");
            } else if (genericStackData.hasKey("FluidName", Constants.NBT.TAG_STRING)) {
                missingName = genericStackData.getString("FluidName");
            }

            if (missingName != null) {
                ITextComponent missingId = new TextComponentString(missingName);
                AEKeyType keyType = null;

                try {
                    String keyTypeString = genericStackData.getString(AEKey.TYPE_FIELD);
                    if (!keyTypeString.isEmpty()) {
                        ResourceLocation keyTypeId = new ResourceLocation(keyTypeString);
                        keyType = AEKeyTypesInternal.get(keyTypeId);
                        if (keyType == null) {
                            missingId = new TextComponentString(missingId.getFormattedText() + " (" + keyTypeString + ")");
                        }
                    }
                } catch (RuntimeException ignored) {
                }

                long amount = Math.max(1, genericStackData.getLong(GenericStack.AMOUNT_FIELD));
                return new BrokenStackInfo(missingId, keyType, amount);
            }
        }

        return null;
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected void addCheckedInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag advancedTooltips) {
        super.addCheckedInformation(stack, world, lines, advancedTooltips);

        NBTTagCompound tag = stack.getTagCompound();
        NBTTagString error = AEComponents.MISSING_CONTENT_ERROR_COMPONENT.readFrom(tag);
        if (error != null) {
            lines.add(TextFormatting.GRAY + error.getString());
        }
    }

    public record BrokenStackInfo(ITextComponent displayName, @Nullable AEKeyType keyType, long amount) {
    }
}
