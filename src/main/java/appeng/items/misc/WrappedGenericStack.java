/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2018, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.items.misc;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.items.AEBaseItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Wraps a {@link GenericStack} in an {@link ItemStack}. Even stacks that actually represent vanilla {@link Item items}
 * will be wrapped in this item, to allow items with amount 0 to be represented as itemstacks without becoming the empty
 * item.
 */
public class WrappedGenericStack extends AEBaseItem {
    public WrappedGenericStack() {
        super();
        setMaxStackSize(1);
    }

    public static ItemStack wrap(GenericStack stack) {
        Objects.requireNonNull(stack, "stack");
        var item = AEItems.WRAPPED_GENERIC_STACK.asItem();
        var result = new ItemStack(item);
        result.setTagInfo(AEComponents.WRAPPED_STACK_COMPONENT.name(), GenericStack.writeTag(stack));
        return result;
    }

    public static ItemStack wrap(AEKey what, long amount) {
        Objects.requireNonNull(what, "what");
        return wrap(new GenericStack(what, amount));
    }

    @Nullable
    public AEKey unwrapWhat(ItemStack stack) {
        var wrapped = unwrap(stack);
        return wrapped == null ? null : wrapped.what();
    }

    public long unwrapAmount(ItemStack stack) {
        var wrapped = unwrap(stack);
        return wrapped == null ? 0 : wrapped.amount();
    }

    @Override
    public boolean onOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, EntityPlayer player) {
        var what = unwrapWhat(stack);
        if (what == null && slot.getStack() == stack) {
            slot.putStack(ItemStack.EMPTY);
            return true;
        }

        if (what == null) {
            return true;
        }

        var heldContainer = ContainerItemStrategies.findCarriedContextForKey(what, player, player.openContainer);
        if (heldContainer != null) {
            long amount = unwrapAmount(stack);
            long inserted = heldContainer.insert(what, amount, Actionable.MODULATE);
            heldContainer.playFillSound(player, what);

            if (inserted >= amount) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.putStack(wrap(what, amount - inserted));
            }
        }

        return true;
    }

    @Nullable
    private GenericStack unwrap(ItemStack stack) {
        if (stack.getItem() != this) {
            return null;
        }

        var tag = stack.getTagCompound();
        if (!AEComponents.WRAPPED_STACK_COMPONENT.isPresentIn(tag)) {
            return null;
        }

        NBTTagCompound wrapped = AEComponents.WRAPPED_STACK_COMPONENT.readFrom(tag);
        if (wrapped == null) {
            return null;
        }
        return GenericStack.readTag(wrapped);
    }
}
