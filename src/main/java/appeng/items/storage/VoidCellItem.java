package appeng.items.storage;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.storage.cells.IStackTooltipDataProvider;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.localization.GuiText;
import appeng.items.AEBaseItem;
import appeng.items.contents.CellConfig;
import appeng.me.cells.VoidCellHandler;
import appeng.util.ConfigInventory;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class VoidCellItem extends AEBaseItem implements ICellWorkbenchItem, IStackTooltipDataProvider {
    private static final String STORAGE_CELL_FUZZY_MODE = "storage_cell_fuzzy_mode";

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

    @Override
    protected void addCheckedInformation(ItemStack stack, World world, List<String> lines,
                                         ITooltipFlag advancedTooltips) {
        addToTooltip(stack, lines);
    }

    @Override
    public java.util.Optional<StorageCellTooltipComponent> getStackTooltipData(ItemStack stack) {
        return VoidCellHandler.INSTANCE.getTooltipData(stack);
    }

    @Override
    public void addToTooltip(ItemStack stack, List<String> lines) {
        lines.add(createUsageLine(GuiText.BytesUsed.text(number())).getFormattedText());
        lines.add(createTypesLine().getFormattedText());
        VoidCellHandler.INSTANCE.addPartitionInformation(stack, lines);
    }
}
