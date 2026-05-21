/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 */
package appeng.items.tools.powered;

import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.features.GridLinkables;
import appeng.api.features.IGridLinkableHandler;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.networking.IGrid;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableItem;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.upgrades.Upgrades;
import appeng.api.util.DimensionalBlockPos;
import appeng.api.util.IConfigManager;
import appeng.container.GuiIds;
import appeng.core.gui.GuiOpener;
import appeng.core.gui.locator.GuiHostLocators;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.core.localization.Tooltips;
import appeng.helpers.WirelessTerminalGuiHost;
import appeng.util.Platform;
import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Optional;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class WirelessTerminalItem extends PoweredContainerItem implements IGuiItem, IUpgradeableItem, IBauble {
    public static final IGridLinkableHandler LINKABLE_HANDLER = new LinkableHandler();
    private static final String LINK_TAG_DIM = "dim";
    private static final String LINK_TAG_X = "x";
    private static final String LINK_TAG_Y = "y";
    private static final String LINK_TAG_Z = "z";
    private final double powerCapacity;

    public WirelessTerminalItem(double powerCapacity) {
        super(powerCapacity);
        this.setMaxStackSize(1);
        this.powerCapacity = powerCapacity;
        GridLinkables.register(this, LINKABLE_HANDLER);
    }

    @Override
    public double getChargeRate(ItemStack stack) {
        return 800d + 800d * Upgrades.getEnergyCardMultiplier(getUpgrades(stack));
    }

    public boolean openFromInventory(EntityPlayer player, ItemGuiHostLocator locator) {
        return openFromInventory(player, locator, false);
    }

    protected boolean openFromInventory(EntityPlayer player, ItemGuiHostLocator locator, boolean returningFromSubmenu) {
        var is = locator.locateItem(player);

        if (!player.world.isRemote && checkPreconditions(is)) {
            return GuiOpener.openItemGui(player, getGuiKey(), locator, returningFromSubmenu);
        }
        return false;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World level, EntityPlayer player, EnumHand hand) {
        var is = player.getHeldItem(hand);

        if (!player.world.isRemote && checkPreconditions(is)) {
            if (GuiOpener.openItemGui(player, getGuiKey(), GuiHostLocators.forHand(player, hand))) {
                return new ActionResult<>(EnumActionResult.SUCCESS, is);
            }
        }

        return new ActionResult<>(EnumActionResult.FAIL, is);
    }

    @Override
    protected void addCheckedInformation(final ItemStack stack, final World world, final List<String> lines,
                                         final ITooltipFlag advancedTooltips) {
        super.addCheckedInformation(stack, world, lines, advancedTooltips);

        if (getLinkedPosition(stack) == null) {
            lines.add(Tooltips.of(GuiText.Unlinked, Tooltips.RED).getFormattedText());
        } else {
            lines.add(Tooltips.of(GuiText.Linked, Tooltips.GREEN).getFormattedText());
        }
    }

    @Nullable
    public DimensionalBlockPos getLinkedPosition(ItemStack item) {
        NBTTagCompound tag = item.getTagCompound();
        if (tag == null) {
            return null;
        }

        NBTTagCompound link = AEComponents.WIRELESS_LINK_TARGET_COMPONENT.readFrom(tag);
        if (link == null) {
            return null;
        }
        int dim = link.getInteger(LINK_TAG_DIM);
        WorldServer world = DimensionManager.getWorld(dim);
        if (world == null) {
            return null;
        }
        return new DimensionalBlockPos(world, link.getInteger(LINK_TAG_X), link.getInteger(LINK_TAG_Y),
            link.getInteger(LINK_TAG_Z));
    }

    @Nullable
    public IGrid getLinkedGrid(ItemStack item, World level, @Nullable Consumer<ITextComponent> errorConsumer) {
        var linkedPos = getLinkedPosition(item);
        if (linkedPos == null) {
            if (errorConsumer != null) {
                errorConsumer.accept(PlayerMessages.DeviceNotLinked.text());
            }
            return null;
        }

        if (!(linkedPos.getLevel() instanceof WorldServer linkedLevel)) {
            if (errorConsumer != null) {
                errorConsumer.accept(PlayerMessages.LinkedNetworkNotFound.text());
            }
            return null;
        }

        var be = linkedLevel.getTileEntity(linkedPos.getPos());
        if (!(be instanceof IWirelessAccessPoint accessPoint)) {
            if (errorConsumer != null) {
                errorConsumer.accept(PlayerMessages.LinkedNetworkNotFound.text());
            }
            return null;
        }

        var grid = accessPoint.getGrid();
        if (grid == null && errorConsumer != null) {
            errorConsumer.accept(PlayerMessages.LinkedNetworkNotFound.text());
        }
        return grid;
    }

    public GuiIds.GuiKey getGuiKey() {
        return GuiIds.GuiKey.WIRELESS_TERMINAL;
    }

    @Nullable
    @Override
    public WirelessTerminalGuiHost<?> getGuiHost(EntityPlayer player, ItemGuiHostLocator locator,
                                                 @Nullable RayTraceResult hitResult) {
        return new WirelessTerminalGuiHost<>(this, player, locator,
            (p, subGui) -> openFromInventory(p, locator, true));
    }

    protected boolean checkPreconditions(ItemStack item) {
        return !item.isEmpty() && item.getItem() == this;
    }

    public boolean usePower(EntityPlayer player, double amount, ItemStack is) {
        return extractAEPower(is, amount, Actionable.MODULATE) >= amount - 0.5;
    }

    public boolean hasPower(EntityPlayer player, double amt, ItemStack is) {
        return getAECurrentPower(is) >= amt;
    }

    public IConfigManager getConfigManager(Supplier<ItemStack> target) {
        return IConfigManager.builder(target)
                             .registerSetting(Settings.SORT_BY, SortOrder.NAME)
                             .registerSetting(Settings.VIEW_MODE, ViewItems.ALL)
                             .registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING)
                             .build();
    }

    @Optional.Method(modid = "baubles")
    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.TRINKET;
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        return UpgradeInventories.forItem(stack, 2, this::onUpgradesChanged);
    }

    private void onUpgradesChanged(ItemStack stack, IUpgradeInventory upgrades) {
        setAEMaxPower(stack, powerCapacity * (1 + Upgrades.getEnergyCardMultiplier(upgrades)));
    }

    private static class LinkableHandler implements IGridLinkableHandler {
        @Override
        public boolean canLink(ItemStack stack) {
            return stack.getItem() instanceof WirelessTerminalItem;
        }

        @Override
        public void link(ItemStack itemStack, World world, net.minecraft.util.math.BlockPos pos) {
            NBTTagCompound tag = itemStack.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                itemStack.setTagCompound(tag);
            }
            NBTTagCompound link = new NBTTagCompound();
            link.setInteger(LINK_TAG_DIM, world.provider.getDimension());
            link.setInteger(LINK_TAG_X, pos.getX());
            link.setInteger(LINK_TAG_Y, pos.getY());
            link.setInteger(LINK_TAG_Z, pos.getZ());
            AEComponents.WIRELESS_LINK_TARGET_COMPONENT.writeTo(tag, link);
        }

        @Override
        public void unlink(ItemStack itemStack) {
            NBTTagCompound tag = itemStack.getTagCompound();
            if (tag != null) {
                tag.removeTag(AEComponents.WIRELESS_LINK_TARGET_COMPONENT.name());
                if (Platform.isNbtEmpty(tag)) {
                    itemStack.setTagCompound(null);
                }
            }
        }
    }
}
