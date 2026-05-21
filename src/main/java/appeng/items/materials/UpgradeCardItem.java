/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.items.materials;

import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.Upgrades;
import appeng.core.localization.InGameTooltip;
import appeng.core.localization.PlayerMessages;
import appeng.items.AEBaseItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class UpgradeCardItem extends AEBaseItem {

    @SideOnly(Side.CLIENT)
    @Override
    protected void addCheckedInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag advancedTooltips) {
        super.addCheckedInformation(stack, world, lines, advancedTooltips);

        var supportedBy = Upgrades.getTooltipLinesForCard(this);
        if (!supportedBy.isEmpty()) {
            lines.add(InGameTooltip.supported_by.getLocal());
            for (var line : supportedBy) {
                lines.add(line.getFormattedText());
            }
        }
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX,
                                           float hitY, float hitZ, EnumHand hand) {
        if (player.isSneaking()) {
            TileEntity tileEntity = world.getTileEntity(pos);
            IUpgradeInventory upgrades = null;

            if (tileEntity instanceof IPartHost partHost) {
                SelectedPart selectedPart = partHost.selectPartLocal(new Vec3d(hitX, hitY, hitZ));
                if (selectedPart.part instanceof IUpgradeableObject upgradeableObject) {
                    upgrades = upgradeableObject.getUpgrades();
                }
            } else if (tileEntity instanceof IUpgradeableObject upgradeableObject) {
                upgrades = upgradeableObject.getUpgrades();
            }

            if (upgrades != null && upgrades.size() > 0) {
                ItemStack heldStack = player.getHeldItem(hand);

                boolean isFull = true;
                for (int i = 0; i < upgrades.size(); i++) {
                    if (upgrades.getStackInSlot(i).isEmpty()) {
                        isFull = false;
                        break;
                    }
                }

                if (isFull) {
                    player.sendStatusMessage(PlayerMessages.MaxUpgradesInstalled.text(), true);
                    return EnumActionResult.FAIL;
                }

                int maxInstalled = upgrades.getMaxInstalled(heldStack.getItem());
                int installed = upgrades.getInstalledUpgrades(heldStack.getItem());
                if (maxInstalled <= 0) {
                    player.sendStatusMessage(PlayerMessages.UnsupportedUpgrade.text(), true);
                    return EnumActionResult.FAIL;
                } else if (installed >= maxInstalled) {
                    player.sendStatusMessage(PlayerMessages.MaxUpgradesOfTypeInstalled.text(), true);
                    return EnumActionResult.FAIL;
                }

                if (world.isRemote) {
                    return EnumActionResult.PASS;
                }

                player.setHeldItem(hand, upgrades.addItems(heldStack));
                return EnumActionResult.SUCCESS;
            }
        }

        return super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand);
    }
}
