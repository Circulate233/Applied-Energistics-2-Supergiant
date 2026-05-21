/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.items.tools;

import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardColors;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.inventories.InternalInventory;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.util.IConfigurableObject;
import appeng.block.AEBaseTileBlock;
import appeng.core.localization.InGameTooltip;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.IConfigInvHost;
import appeng.helpers.IPriorityHost;
import appeng.items.AEBaseItem;
import appeng.items.contents.NetworkToolGuiHost;
import appeng.util.InteractionUtil;
import appeng.util.Platform;
import appeng.util.inv.PlayerInternalInventory;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MemoryCardItem extends AEBaseItem implements IMemoryCard {
    private static final int DEFAULT_BASE_COLOR = 0x9cd3ff;
    private static final String[] EXPORTED_TAGS = {
        AEComponents.EXPORTED_SETTINGS_SOURCE,
        AEComponents.EXPORTED_CUSTOM_NAME,
        AEComponents.EXPORTED_UPGRADES,
        AEComponents.EXPORTED_SETTINGS,
        AEComponents.EXPORTED_PRIORITY,
        AEComponents.EXPORTED_P2P_TYPE,
        AEComponents.EXPORTED_P2P_FREQUENCY,
        AEComponents.MEMORY_CARD_COLORS,
        AEComponents.EXPORTED_CONFIG_INV,
        AEComponents.EXPORTED_LEVEL_EMITTER_VALUE,
        AEComponents.EXPORTED_PATTERNS,
        AEComponents.EXPORTED_PUSH_DIRECTION
    };
    private static final String[] IMPORTABLE_EXPORTED_TAGS = {
        AEComponents.EXPORTED_UPGRADES,
        AEComponents.EXPORTED_SETTINGS,
        AEComponents.EXPORTED_PRIORITY,
        AEComponents.EXPORTED_CONFIG_INV
    };

    public MemoryCardItem() {
        this.setMaxStackSize(1);
    }

    public static void exportGenericSettings(Object exportFrom, NBTTagCompound output) {
        if (exportFrom instanceof IUpgradeableObject) {
            writeUpgrades(((IUpgradeableObject) exportFrom).getUpgrades(), output);
        }

        if (exportFrom instanceof IConfigurableObject) {
            NBTTagCompound settings = new NBTTagCompound();
            Map<String, String> exported = ((IConfigurableObject) exportFrom).getConfigManager().exportSettings();
            for (Map.Entry<String, String> entry : exported.entrySet()) {
                settings.setString(entry.getKey(), entry.getValue());
            }
            if (!Platform.isNbtEmpty(settings)) {
                AEComponents.EXPORTED_SETTINGS_COMPONENT.writeTo(output, settings);
            }
        }

        if (exportFrom instanceof IPriorityHost) {
            AEComponents.EXPORTED_PRIORITY_COMPONENT.writeTo(output,
                new NBTTagInt(((IPriorityHost) exportFrom).getPriority()));
        }

        if (exportFrom instanceof IConfigInvHost) {
            NBTTagList config = ((IConfigInvHost) exportFrom).getConfig().writeToTag();
            if (config.tagCount() > 0) {
                AEComponents.EXPORTED_CONFIG_INV_COMPONENT.writeTo(output, config);
            }
        }
    }

    public static Set<String> importGenericSettings(Object importTo, NBTTagCompound input, @Nullable EntityPlayer player) {
        Set<String> imported = new ObjectOpenHashSet<>();

        if (player != null && importTo instanceof IUpgradeableObject) {
            NBTTagList upgrades = AEComponents.EXPORTED_UPGRADES_COMPONENT.readFrom(input);
            if (upgrades != null) {
                restoreUpgrades(player, upgrades, (IUpgradeableObject) importTo);
                imported.add(AEComponents.EXPORTED_UPGRADES);
            }
        }

        if (importTo instanceof IConfigurableObject && AEComponents.EXPORTED_SETTINGS_COMPONENT.isPresentIn(input)) {
            Map<String, String> settings = new Object2ObjectOpenHashMap<>();
            NBTTagCompound settingsTag = AEComponents.EXPORTED_SETTINGS_COMPONENT.readFrom(input);
            if (settingsTag == null) {
                settingsTag = new NBTTagCompound();
            }
            for (String key : settingsTag.getKeySet()) {
                settings.put(key, settingsTag.getString(key));
            }
            if (((IConfigurableObject) importTo).getConfigManager().importSettings(settings)) {
                imported.add(AEComponents.EXPORTED_SETTINGS);
            }
        }

        if (importTo instanceof IPriorityHost) {
            NBTBase priorityBase = AEComponents.EXPORTED_PRIORITY_COMPONENT.copy(input.getTag(AEComponents.EXPORTED_PRIORITY));
            NBTTagInt priorityTag = priorityBase instanceof NBTTagInt ? (NBTTagInt) priorityBase : null;
            if (priorityTag != null) {
                ((IPriorityHost) importTo).setPriority(priorityTag.getInt());
                imported.add(AEComponents.EXPORTED_PRIORITY);
            }
        }

        if (importTo instanceof IConfigInvHost) {
            NBTTagList configTag = AEComponents.EXPORTED_CONFIG_INV_COMPONENT.readFrom(input);
            if (configTag != null) {
                ((IConfigInvHost) importTo).getConfig().readFromTag(configTag);
                imported.add(AEComponents.EXPORTED_CONFIG_INV);
            }
        }

        return imported;
    }

    public static void importGenericSettingsAndNotify(Object importTo, NBTTagCompound input, @Nullable EntityPlayer player) {
        List<String> exportedSettings = getExportedSettings(input);
        Set<String> imported = importGenericSettings(importTo, input, player);
        if (player != null && !player.world.isRemote) {
            if (imported.isEmpty()) {
                player.sendStatusMessage(PlayerMessages.InvalidMachine.text(), true);
            } else {
                player.sendStatusMessage(
                    PlayerMessages.InvalidMachinePartiallyRestored.text(formatImportedSettings(exportedSettings, imported)),
                    true);
            }
        }
    }

    private static List<String> getExportedSettings(NBTTagCompound input) {
        List<String> exportedSettings = new ObjectArrayList<>(IMPORTABLE_EXPORTED_TAGS.length);
        for (String exportedTag : IMPORTABLE_EXPORTED_TAGS) {
            if (input.hasKey(exportedTag)) {
                exportedSettings.add(exportedTag);
            }
        }
        return exportedSettings;
    }

    private static String getSettingTranslationKey(String exportedSetting) {
        return "exported_setting.ae2." + exportedSetting;
    }

    private static ITextComponent getSettingComponent(String exportedSetting) {
        return new TextComponentTranslation(getSettingTranslationKey(exportedSetting));
    }

    private static ITextComponent formatImportedSettings(List<String> exportedSettings, Set<String> importedSettings) {
        ITextComponent text = new TextComponentString("");
        boolean first = true;
        for (String exportedSetting : exportedSettings) {
            if (!importedSettings.contains(exportedSetting)) {
                continue;
            }

            if (!first) {
                text.appendText(", ");
            }
            text.appendSibling(getSettingComponent(exportedSetting));
            first = false;
        }
        return text;
    }

    private static void writeUpgrades(IUpgradeInventory upgrades, NBTTagCompound output) {
        NBTTagList list = new NBTTagList();
        Reference2IntOpenHashMap<Item> counts = new Reference2IntOpenHashMap<>();
        counts.defaultReturnValue(0);

        for (ItemStack upgrade : upgrades) {
            if (upgrade.isEmpty()) {
                continue;
            }
            counts.addTo(upgrade.getItem(), upgrade.getCount());
        }

        for (Reference2IntMap.Entry<Item> entry : counts.reference2IntEntrySet()) {
            ItemStack stack = new ItemStack(entry.getKey(), entry.getIntValue());
            NBTTagCompound stackTag = new NBTTagCompound();
            stack.writeToNBT(stackTag);
            list.appendTag(stackTag);
        }

        if (list.tagCount() > 0) {
            AEComponents.EXPORTED_UPGRADES_COMPONENT.writeTo(output, list);
        }
    }

    private static void restoreUpgrades(EntityPlayer player, NBTTagList desiredUpgrades, IUpgradeableObject upgradeableObject) {
        IUpgradeInventory upgrades = upgradeableObject.getUpgrades();
        if (player.capabilities.isCreativeMode) {
            for (int i = 0; i < upgrades.size(); i++) {
                upgrades.setItemDirect(i, ItemStack.EMPTY);
            }
            for (int i = 0; i < desiredUpgrades.tagCount(); i++) {
                upgrades.addItems(new ItemStack(desiredUpgrades.getCompoundTagAt(i)));
            }
            return;
        }

        List<InternalInventory> upgradeSources = new ObjectArrayList<>();
        upgradeSources.add(new PlayerInternalInventory(player.inventory));
        NetworkToolGuiHost<?> networkTool = NetworkToolItem.findNetworkToolInv(player);
        if (networkTool != null) {
            upgradeSources.add(networkTool.getInventory());
        }

        Reference2IntOpenHashMap<Item> desiredCounts = new Reference2IntOpenHashMap<>();
        desiredCounts.defaultReturnValue(0);
        for (int i = 0; i < desiredUpgrades.tagCount(); i++) {
            ItemStack desired = new ItemStack(desiredUpgrades.getCompoundTagAt(i));
            if (!desired.isEmpty()) {
                desiredCounts.addTo(desired.getItem(), desired.getCount());
            }
        }

        for (int i = 0; i < upgrades.size(); i++) {
            ItemStack current = upgrades.getStackInSlot(i);
            if (current.isEmpty()) {
                continue;
            }

            int target = desiredCounts.getInt(current.getItem());
            int installed = upgradeableObject.getInstalledUpgrades(current.getItem());
            int toRemove = installed - target;
            if (toRemove > 0) {
                ItemStack removed = upgrades.extractItem(i, toRemove, false);
                for (InternalInventory source : upgradeSources) {
                    if (!removed.isEmpty()) {
                        removed = source.addItems(removed);
                    }
                }
                if (!removed.isEmpty()) {
                    player.dropItem(removed, false);
                }
            }
        }

        for (Reference2IntMap.Entry<Item> entry : desiredCounts.reference2IntEntrySet()) {
            int missing = entry.getIntValue() - upgradeableObject.getInstalledUpgrades(entry.getKey());
            if (missing <= 0) {
                continue;
            }

            ItemStack desired = new ItemStack(entry.getKey(), missing);
            ItemStack overflow = upgrades.addItems(desired, true);
            if (!overflow.isEmpty()) {
                missing -= overflow.getCount();
            }

            for (InternalInventory source : upgradeSources) {
                if (missing <= 0) {
                    break;
                }
                ItemStack extracted = source.removeItems(missing, desired, null);
                if (!extracted.isEmpty()) {
                    overflow = upgrades.addItems(extracted);
                    if (!overflow.isEmpty()) {
                        player.inventory.placeItemBackInInventory(player.world, overflow);
                    }
                    missing -= extracted.getCount();
                }
            }

            if (missing > 0 && !player.world.isRemote) {
                player.sendStatusMessage(PlayerMessages.MissingUpgrades.text(new ItemStack(entry.getKey()).getDisplayName(), missing),
                    true);
            }
        }
    }

    public static void clearCard(ItemStack card) {
        NBTTagCompound tag = card.getTagCompound();
        if (tag == null) {
            return;
        }

        for (String exportedTag : EXPORTED_TAGS) {
            tag.removeTag(exportedTag);
        }

        if (Platform.isNbtEmpty(tag)) {
            card.setTagCompound(null);
        }
    }

    public static void setMemoryCardColors(NBTTagCompound tag, MemoryCardColors colors) {
        tag.setIntArray(AEComponents.MEMORY_CARD_COLORS, colors.toArray());
    }

    public static MemoryCardColors getMemoryCardColors(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? MemoryCardColors.DEFAULT : MemoryCardColors.fromTag(tag, AEComponents.MEMORY_CARD_COLORS);
    }

    public static int getTintColor(ItemStack stack, int tintIndex) {
        if (tintIndex == 1 && stack.getItem() instanceof MemoryCardItem) {
            return ((MemoryCardItem) stack.getItem()).getColor(stack);
        }
        return 0xFFFFFF;
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected void addCheckedInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag advancedTooltips) {
        super.addCheckedInformation(stack, world, lines, advancedTooltips);

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return;
        }

        if (AEComponents.EXPORTED_SETTINGS_SOURCE_COMPONENT.isPresentIn(tag)) {
            var source = AEComponents.EXPORTED_SETTINGS_SOURCE_COMPONENT.readFrom(tag);
            if (source != null) {
                lines.add(source.getString());
            }
        }

        NBTBase frequencyBase = AEComponents.EXPORTED_P2P_FREQUENCY_COMPONENT.copy(tag.getTag(AEComponents.EXPORTED_P2P_FREQUENCY));
        NBTTagShort frequencyTag = frequencyBase instanceof NBTTagShort ? (NBTTagShort) frequencyBase : null;
        if (frequencyTag != null) {
            ITextComponent freq = Platform.p2p().toColoredHexString(frequencyTag.getShort());
            lines.add(InGameTooltip.P2PFrequency.getLocal(freq.getFormattedText()));
        }
    }

    @Override
    public void notifyUser(EntityPlayer player, MemoryCardMessages msg) {
        if (player.world.isRemote) {
            return;
        }

        switch (msg) {
            case SETTINGS_CLEARED:
                player.sendStatusMessage(PlayerMessages.SettingCleared.text(), true);
                break;
            case INVALID_MACHINE:
                player.sendStatusMessage(PlayerMessages.InvalidMachine.text(), true);
                break;
            case SETTINGS_LOADED:
                player.sendStatusMessage(PlayerMessages.LoadedSettings.text(), true);
                break;
            case SETTINGS_SAVED:
                player.sendStatusMessage(PlayerMessages.SavedSettings.text(), true);
                break;
            case SETTINGS_RESET:
                player.sendStatusMessage(PlayerMessages.ResetSettings.text(), true);
                break;
            default:
                break;
        }
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX,
                                           float hitY, float hitZ, EnumHand hand) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            Block block = world.getBlockState(pos).getBlock();
            if (block instanceof AEBaseTileBlock<?>) {
                return EnumActionResult.PASS;
            }
            if (!world.isRemote) {
                clearCard(player, hand);
            }
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (InteractionUtil.isInAlternateUseMode(player) && !world.isRemote) {
            clearCard(player, hand);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess level, BlockPos pos, EntityPlayer player) {
        return true;
    }

    private void clearCard(EntityPlayer player, EnumHand hand) {
        IMemoryCard memoryCard = (IMemoryCard) player.getHeldItem(hand).getItem();
        memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_CLEARED);
        clearCard(player.getHeldItem(hand));
    }

    public int getColor(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("display", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound display = tag.getCompoundTag("display");
            if (display.hasKey("color", Constants.NBT.TAG_ANY_NUMERIC)) {
                return display.getInteger("color");
            }
        }
        return DEFAULT_BASE_COLOR;
    }
}
