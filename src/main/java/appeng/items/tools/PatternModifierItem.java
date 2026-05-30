package appeng.items.tools;

import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.ItemGuiHost;
import appeng.container.GuiIds;
import appeng.core.gui.GuiOpener;
import appeng.core.gui.locator.GuiHostLocators;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.items.AEBaseItem;
import appeng.items.contents.PatternModifierGuiHost;
import appeng.util.InteractionUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PatternModifierItem extends AEBaseItem implements IGuiItem {
    public static final int PATTERN_SLOTS = 27;
    public static final int BLANK_PATTERN_SLOTS = 4;

    public PatternModifierItem() {
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (!world.isRemote) {
            GuiOpener.openItemGui(player, GuiIds.GuiKey.PATTERN_MODIFIER, GuiHostLocators.forHand(player, hand));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, held);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX,
                                           float hitY, float hitZ, EnumHand hand) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return EnumActionResult.PASS;
        }

        if (!world.isRemote) {
            return GuiOpener.openItemGui(player, GuiIds.GuiKey.PATTERN_MODIFIER,
                GuiHostLocators.forItemUseContext(player, hand, pos, side, hitX, hitY, hitZ))
                ? EnumActionResult.SUCCESS
                : EnumActionResult.FAIL;
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ItemGuiHost<?> getGuiHost(EntityPlayer player, ItemGuiHostLocator locator,
                                     @Nullable RayTraceResult hitResult) {
        return new PatternModifierGuiHost(this, player, locator);
    }
}
