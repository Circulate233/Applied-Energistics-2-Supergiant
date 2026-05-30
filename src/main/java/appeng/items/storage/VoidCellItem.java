package appeng.items.storage;

import appeng.api.config.CondenserOutput;
import appeng.api.config.FuzzyMode;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.ItemGuiHost;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.storage.cells.IStackTooltipDataProvider;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.container.GuiIds;
import appeng.core.gui.GuiOpener;
import appeng.core.gui.locator.GuiHostLocators;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.core.localization.GuiText;
import appeng.items.AEBaseItem;
import appeng.items.contents.CellConfig;
import appeng.items.contents.VoidCellGuiHost;
import appeng.me.cells.VoidCellHandler;
import appeng.util.ConfigInventory;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class VoidCellItem extends AEBaseItem implements ICellWorkbenchItem, IStackTooltipDataProvider, IGuiItem {
    private static final String STORAGE_CELL_FUZZY_MODE = "storage_cell_fuzzy_mode";
    public static final String VOID_CELL_MODE = "void_cell_mode";
    public static final String VOID_CELL_ENERGY = "void_cell_energy";

    public VoidCellItem() {
        this.setMaxStackSize(1);
    }

    private static ITextComponent createUsageLine(ITextComponent prefix) {
        return prefix.appendText(" / ").appendSibling(obfuscatedMax());
    }

    private static ITextComponent createTypesLine() {
        return number()
            .appendText(" ")
            .appendSibling(new TextComponentString(GuiText.Of.getLocal()))
            .appendText(" ")
            .appendSibling(obfuscatedMax())
            .appendText(" ")
            .appendSibling(new TextComponentString(GuiText.Types.getLocal()));
    }

    private static ITextComponent number() {
        return new TextComponentString("0").setStyle(
            new Style().setColor(TextFormatting.LIGHT_PURPLE).setItalic(false));
    }

    private static ITextComponent obfuscatedMax() {
        return new TextComponentString("9999").setStyle(
            new Style().setColor(TextFormatting.DARK_RED).setObfuscated(true).setItalic(false));
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack is) {
        return UpgradeInventories.forItem(is, 2);
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack is) {
        return CellConfig.create(is);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack is) {
        NBTTagCompound tag = is.getTagCompound();
        if (tag != null && tag.hasKey(STORAGE_CELL_FUZZY_MODE, 8)) {
            try {
                return FuzzyMode.valueOf(tag.getString(STORAGE_CELL_FUZZY_MODE));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            is.setTagCompound(tag);
        }
        tag.setString(STORAGE_CELL_FUZZY_MODE, fzMode.name());
    }

    public CondenserOutput getMode(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(VOID_CELL_MODE, 8)) {
            try {
                return CondenserOutput.valueOf(tag.getString(VOID_CELL_MODE));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return CondenserOutput.TRASH;
    }

    public void setMode(ItemStack stack, CondenserOutput mode) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }

        if (mode == CondenserOutput.TRASH) {
            tag.removeTag(VOID_CELL_MODE);
        } else {
            tag.setString(VOID_CELL_MODE, mode.name());
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!world.isRemote) {
            GuiOpener.openItemGui(player, GuiIds.GuiKey.VOID_CELL, GuiHostLocators.forHand(player, hand));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Nullable
    @Override
    public ItemGuiHost<?> getGuiHost(EntityPlayer player, ItemGuiHostLocator locator,
                                     @Nullable RayTraceResult hitResult) {
        return new VoidCellGuiHost(this, player, locator);
    }

    @Override
    protected void addCheckedInformation(ItemStack stack, World world, List<String> lines,
                                         ITooltipFlag advancedTooltips) {
        addToTooltip(stack, lines);
    }

    @Override
    public Optional<StorageCellTooltipComponent> getStackTooltipData(ItemStack stack) {
        return VoidCellHandler.INSTANCE.getTooltipData(stack);
    }

    @Override
    public void addToTooltip(ItemStack stack, List<String> lines) {
        CondenserOutput mode = getMode(stack);
        lines.add(new TextComponentTranslation("gui.ae2.VoidCell.mode." + mode.ordinal())
            .setStyle(new Style().setColor(TextFormatting.GREEN).setItalic(false))
            .getFormattedText());
        lines.add(new TextComponentTranslation("gui.ae2.VoidCell.OpenGui")
            .setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(false))
            .getFormattedText());
        lines.add(createUsageLine(GuiText.BytesUsed.text(number())).getFormattedText());
        lines.add(createTypesLine().getFormattedText());
        VoidCellHandler.INSTANCE.addPartitionInformation(stack, lines);
    }
}
