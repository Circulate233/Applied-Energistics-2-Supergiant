package appeng.crafting.pattern;

import appeng.api.crafting.EncodedPatternDecoder;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.InvalidPatternTooltipStrategy;
import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.core.DebugCreativeTab;
import appeng.core.definitions.AEItems;
import appeng.items.AEBaseItem;
import appeng.items.misc.MissingContentItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class EncodedPatternItem<T extends IPatternDetails> extends AEBaseItem {
    private final EncodedPatternDecoder<T> decoder;
    @Nullable
    private final InvalidPatternTooltipStrategy invalidPatternTooltip;

    public EncodedPatternItem(EncodedPatternDecoder<T> decoder,
                              @Nullable InvalidPatternTooltipStrategy invalidPatternTooltip,
                              int maxStackSize) {
        super();
        this.decoder = decoder;
        this.invalidPatternTooltip = invalidPatternTooltip;
        this.setMaxStackSize(maxStackSize);
    }

    private static ITextComponent formatLine(ITextComponent label, ITextComponent value) {
        return new TextComponentString("")
            .appendSibling(label.createCopy())
            .appendText(": ")
            .appendSibling(value.createCopy());
    }

    protected static ITextComponent getTooltipEntryLine(GenericStack stack) {
        if (stack.what() instanceof AEItemKey itemKey) {
            if (itemKey.getReadOnlyStack().getItem() instanceof MissingContentItem missingContentItem) {
                MissingContentItem.BrokenStackInfo brokenStackInfo = missingContentItem
                    .getBrokenStackInfo(itemKey.getReadOnlyStack());
                if (brokenStackInfo != null) {
                    ITextComponent displayName = brokenStackInfo.displayName().createCopy();
                    displayName.getStyle().setColor(TextFormatting.RED);
                    return getTooltipEntryLine(displayName, brokenStackInfo.keyType(), brokenStackInfo.amount());
                }
            }
        }

        return getTooltipEntryLine(stack.what().getDisplayName(), stack.what().getType(), stack.amount());
    }

    protected static ITextComponent getTooltipEntryLine(ITextComponent displayName, @Nullable AEKeyType amountType,
                                                        long amount) {
        if (amount > 0) {
            String amountText = amountType != null ? amountType.formatAmount(amount, AmountFormat.FULL)
                : String.valueOf(amount);
            return new TextComponentString(amountText)
                .appendText(" x ")
                .appendSibling(displayName.createCopy());
        }
        return displayName.createCopy();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        var stack = player.getHeldItem(hand);
        if (clearPattern(stack, player)) {
            return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    @Override
    protected void getCheckedSubItems(final CreativeTabs creativeTab, final NonNullList<ItemStack> itemStacks) {
        if (creativeTab == DebugCreativeTab.INSTANCE) {
            itemStacks.add(new ItemStack(this));
        }
    }

    @SuppressWarnings("unused")
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                           EnumFacing side, float hitX, float hitY, float hitZ) {
        return clearPattern(player.getHeldItem(hand), player) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    private boolean clearPattern(ItemStack stack, EntityPlayer player) {
        if (!player.isSneaking() || player.world.isRemote) {
            return false;
        }

        Item blankPattern = AEItems.BLANK_PATTERN.item();
        if (blankPattern == null) {
            return false;
        }

        ItemStack replacement = new ItemStack(blankPattern, stack.getCount());
        for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
            if (player.inventory.getStackInSlot(slot) == stack) {
                player.inventory.setInventorySlotContents(slot, replacement);
                return true;
            }
        }

        return false;
    }

    @Override
    protected void addCheckedInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flags) {
        super.addCheckedInformation(stack, world, lines, flags);

        var level = world != null ? world : net.minecraft.client.Minecraft.getMinecraft().world;
        if (level == null) {
            return;
        }

        PatternDetailsTooltip tooltip;
        try {
            var what = AEItemKey.of(stack);
            var details = Objects.requireNonNull(what == null ? null : decoder.decode(what, level),
                "decoder returned null");
            tooltip = details.getTooltip(level, flags);
        } catch (Exception e) {
            lines.add(new TextComponentTranslation("ae2.guitext.invalid_pattern").getUnformattedText());
            tooltip = invalidPatternTooltip == null ? null : invalidPatternTooltip.getTooltip(stack, level, e, flags);
        }

        if (tooltip == null) {
            return;
        }

        for (var output : tooltip.getOutputs()) {
            lines.add(formatLine(tooltip.getOutputMethod(), getTooltipEntryLine(output)).getUnformattedText());
        }

        var withText = new TextComponentTranslation("ae2.guitext.with");
        for (var input : tooltip.getInputs()) {
            lines.add(formatLine(withText, getTooltipEntryLine(input)).getUnformattedText());
        }

        for (var property : tooltip.getProperties()) {
            if (property.value() != null) {
                lines.add(formatLine(property.name(), property.value()).getUnformattedText());
            } else {
                lines.add(property.name().getUnformattedText());
            }
        }
    }

    public ItemStack getOutput(ItemStack item, World level) {
        var details = decode(item, level);
        if (details == null) {
            return ItemStack.EMPTY;
        }

        var output = details.getPrimaryOutput();
        if (output.what() instanceof AEItemKey itemKey) {
            return itemKey.toStack();
        }
        return GenericStack.wrapInItemStack(output);
    }

    @Nullable
    public IPatternDetails decode(ItemStack stack, World level) {
        if (stack.getItem() != this || level == null) {
            return null;
        }

        var what = AEItemKey.of(stack);
        try {
            return what == null ? null : Objects.requireNonNull(decoder.decode(what, level), "decoder returned null");
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public IPatternDetails decode(AEItemKey what, World level) {
        if (what == null || level == null) {
            return null;
        }

        try {
            return decoder.decode(what, level);
        } catch (Exception e) {
            return null;
        }
    }
}
