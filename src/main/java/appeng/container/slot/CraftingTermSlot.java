/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.container.slot;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.container.interfaces.ICraftingGridContainer;
import appeng.container.me.items.ContainerCraftingTerm;
import appeng.helpers.InventoryAction;
import appeng.items.storage.ViewCellItem;
import appeng.util.Platform;
import appeng.util.inv.CarriedItemInventory;
import appeng.util.inv.PlayerInternalInventory;
import appeng.util.prioritylist.IPartitionList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

/**
 * This is the crafting result slot of the crafting terminal, which also handles performing the actual crafting when a
 * player clicks it.
 */
public class CraftingTermSlot extends AppEngCraftingSlot {

    private static final Container DUMMY_CONTAINER = new Container() {
        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return false;
        }
    };

    private final InternalInventory craftInv;
    private final InternalInventory pattern;
    private final IActionSource actionSource;
    private final IEnergySource energySource;
    private final MEStorage storage;
    private final ICraftingGridContainer container;

    public CraftingTermSlot(EntityPlayer player, IActionSource actionSource, IEnergySource energySource,
                            MEStorage storage, InternalInventory craftingMatrix, InternalInventory craftInventory,
                            ICraftingGridContainer container, int x, int y) {
        super(player, craftingMatrix, x, y);
        this.actionSource = actionSource;
        this.energySource = energySource;
        this.storage = storage;
        this.pattern = craftingMatrix;
        this.craftInv = craftInventory;
        this.container = container;
    }

    static void extractRecipeInputs(IEnergySource energySource, IActionSource actionSource,
                                    InternalInventory pattern, MEStorage storage, World world, IRecipe recipe, ItemStack output,
                                    List<ItemStack> craftingItems, ItemStack[] extractedInputs, KeyCounter availableStacks,
                                    IPartitionList filter) {
        for (int slot = 0; slot < pattern.size(); slot++) {
            ItemStack template = pattern.getStackInSlot(slot);
            if (!template.isEmpty()) {
                extractedInputs[slot] = extractItemsByRecipe(energySource, actionSource, storage, world, recipe,
                    output, craftingItems, template, slot, availableStacks, filter);
                craftingItems.set(slot, extractedInputs[slot]);
            }
        }
    }

    static ItemStack extractItemsByRecipe(IEnergySource energySource, IActionSource actionSource,
                                          MEStorage storage, World world, IRecipe recipe, ItemStack output, List<ItemStack> craftingItems,
                                          ItemStack providedTemplate, int slot, KeyCounter availableStacks, IPartitionList filter) {
        if (energySource.extractAEPower(1, Actionable.SIMULATE, PowerMultiplier.CONFIG) <= 0.9) {
            return ItemStack.EMPTY;
        }

        AEItemKey requested = AEItemKey.of(providedTemplate);
        if (requested == null) {
            return ItemStack.EMPTY;
        }

        if (filter == null || filter.isListed(requested)) {
            long extracted = storage.extract(requested, 1, Actionable.MODULATE, actionSource);
            if (extracted > 0) {
                energySource.extractAEPower(1, Actionable.MODULATE, PowerMultiplier.CONFIG);
                return requested.toStack();
            }
        }

        boolean shouldCheckFuzzy = providedTemplate.hasTagCompound() || providedTemplate.isItemStackDamageable();
        if (!shouldCheckFuzzy || availableStacks == null) {
            return ItemStack.EMPTY;
        }

        List<ItemStack> candidateInputs = new ObjectArrayList<>(craftingItems);

        for (Object2LongMap.Entry<AEKey> entry : availableStacks) {
            if (!(entry.getKey() instanceof AEItemKey itemKey)) {
                continue;
            }
            if (providedTemplate.getItem() != itemKey.getItem() || itemKey.matches(output)) {
                continue;
            }

            candidateInputs.set(slot, itemKey.toStack());
            InventoryCrafting adjustedInput = createRecipeInput(candidateInputs);
            if (!recipe.matches(adjustedInput, world)) {
                continue;
            }

            ItemStack adjustedOutput = recipe.getCraftingResult(adjustedInput);
            if (!areStacksEqual(adjustedOutput, output)) {
                continue;
            }

            if (filter != null && !filter.isListed(itemKey)) {
                continue;
            }

            long extracted = storage.extract(itemKey, 1, Actionable.MODULATE, actionSource);
            if (extracted > 0) {
                energySource.extractAEPower(1, Actionable.MODULATE, PowerMultiplier.CONFIG);
                return itemKey.toStack();
            }
        }

        return ItemStack.EMPTY;
    }

    private static InventoryCrafting createRecipeInput(List<ItemStack> stacks) {
        InventoryCrafting recipeInput = new InventoryCrafting(DUMMY_CONTAINER, 3, 3);
        for (int slot = 0; slot < stacks.size(); slot++) {
            recipeInput.setInventorySlotContents(slot, stacks.get(slot));
        }
        return recipeInput;
    }

    private static boolean areStacksEqual(ItemStack first, ItemStack second) {
        return ItemStack.areItemsEqual(first, second)
            && ItemStack.areItemStackTagsEqual(first, second)
            && first.getCount() == second.getCount();
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return false;
    }

    @Override
    public ItemStack onTake(EntityPlayer player, ItemStack stack) {
        return stack;
    }

    public void doClick(InventoryAction action, EntityPlayer who) {
        if (this.getStack().isEmpty() || isRemote()) {
            return;
        }

        final int craftedPerOperation = this.getStack().getCount();

        int maxTimesToCraft;
        InternalInventory target;
        if (action == InventoryAction.CRAFT_SHIFT || action == InventoryAction.CRAFT_ALL) {
            target = new PlayerInternalInventory(who.inventory);
            if (action == InventoryAction.CRAFT_SHIFT) {
                maxTimesToCraft = (int) Math.floor((double) this.getStack().getMaxStackSize() / craftedPerOperation);
            } else {
                maxTimesToCraft = (int) Math.floor((double) this.getStack().getMaxStackSize() / craftedPerOperation
                    * who.inventory.mainInventory.size());
            }
        } else if (action == InventoryAction.CRAFT_STACK) {
            target = new CarriedItemInventory(getContainer());
            maxTimesToCraft = (int) Math.floor((double) this.getStack().getMaxStackSize() / craftedPerOperation);
        } else {
            if (getContainer().getPlayerInventory().getItemStack().isEmpty()) {
                getContainer().getPlayerInventory().setItemStack(craftItem(who, this.storage, this.storage.getAvailableStacks()));
                return;
            }

            target = new CarriedItemInventory(getContainer());
            maxTimesToCraft = 1;
        }

        ItemStack itemAtStart = this.getStack().copy();
        if (itemAtStart.isEmpty()) {
            return;
        }

        for (int crafted = 0; crafted < maxTimesToCraft; crafted++) {
            if (!areStacksEqual(itemAtStart, getStack())) {
                return;
            }

            if (!target.simulateAdd(itemAtStart).isEmpty()) {
                return;
            }

            KeyCounter available = this.storage.getAvailableStacks();
            ItemStack extra = target.addItems(craftItem(who, this.storage, available));
            if (!extra.isEmpty()) {
                Platform.spawnDrops(who.world, who.getPosition(), List.of(extra));
                return;
            }
        }
    }

    @Override
    protected NonNullList<ItemStack> getRemainingItems(InventoryCrafting crafting, World world) {
        IRecipe recipe = findRecipe(crafting, world);
        if (recipe != null && recipe.matches(crafting, world)) {
            return recipe.getRemainingItems(crafting);
        }
        return super.getRemainingItems(crafting, world);
    }

    private ItemStack craftItem(EntityPlayer player, MEStorage inventory, KeyCounter availableStacks) {
        ItemStack crafted = this.getStack().copy();
        if (crafted.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ServerCraftResult serverCraft = null;

        if (!player.world.isRemote) {
            serverCraft = craftItemServerSide(player.world, inventory, availableStacks);
            if (serverCraft.crafted().isEmpty()) {
                return ItemStack.EMPTY;
            }
            crafted = serverCraft.crafted();
        }

        finishCraft(player, inventory, crafted, serverCraft);

        if (getContainer() != null) {
            getContainer().onCraftMatrixChanged(this.craftInv.toContainer());
        }

        return crafted;
    }

    ServerCraftResult craftItemServerSide(World world, MEStorage inventory, KeyCounter availableStacks) {
        List<ItemStack> craftingItems = new ObjectArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            craftingItems.add(this.pattern.getStackInSlot(slot));
        }

        InventoryCrafting recipeInput = createRecipeInput(craftingItems);
        IRecipe recipe = findRecipe(recipeInput, world);
        setRecipeUsed(recipe);

        if (recipe == null) {
            return new ServerCraftResult(ItemStack.EMPTY, new ItemStack[this.pattern.size()]);
        }

        ItemStack crafted = recipe.getCraftingResult(recipeInput);
        ItemStack[] extractedInputs = new ItemStack[this.pattern.size()];
        Arrays.fill(extractedInputs, ItemStack.EMPTY);

        if (inventory != null) {
            IPartitionList filter = ViewCellItem.createItemFilter(this.container.getViewCells());
            extractRecipeInputs(this.energySource, this.actionSource, this.pattern, inventory, world,
                recipe, crafted, craftingItems, extractedInputs, availableStacks, filter);
        }

        return new ServerCraftResult(crafted, extractedInputs);
    }

    void finishCraft(EntityPlayer player, MEStorage inventory, ItemStack crafted, ServerCraftResult serverCraft) {
        makeItem(player, crafted);
        if (serverCraft != null) {
            postCraft(player, inventory, serverCraft.extractedInputs());
        }
    }

    void makeItem(EntityPlayer player, ItemStack crafted) {
        super.onTake(player, crafted);
    }

    private List<ItemStack> writeCraftingGridInputs(MEStorage inventory, ItemStack[] extractedInputs) {
        List<ItemStack> drops = new ObjectArrayList<>();

        for (int slot = 0; slot < this.craftInv.size(); slot++) {
            if (this.craftInv.getStackInSlot(slot).isEmpty()) {
                this.craftInv.setItemDirect(slot, extractedInputs[slot]);
            } else if (!extractedInputs[slot].isEmpty()) {
                AEItemKey what = AEItemKey.of(extractedInputs[slot]);
                if (what == null) {
                    continue;
                }

                long amount = extractedInputs[slot].getCount();
                long inserted = inventory.insert(what, amount, Actionable.MODULATE, this.actionSource);
                if (inserted < amount) {
                    drops.add(what.toStack((int) (amount - inserted)));
                }
            }
        }

        return drops;
    }

    void postCraft(EntityPlayer player, MEStorage inventory, ItemStack[] extractedInputs) {
        List<ItemStack> drops = writeCraftingGridInputs(inventory, extractedInputs);

        if (!drops.isEmpty()) {
            Platform.spawnDrops(player.world,
                new BlockPos((int) player.posX, (int) player.posY, (int) player.posZ),
                drops);
        }
    }

    IRecipe findRecipe(InventoryCrafting recipeInput, World world) {
        if (this.container instanceof ContainerCraftingTerm craftingTermContainer) {
            IRecipe recipe = craftingTermContainer.getCurrentRecipe();
            if (recipe != null && recipe.matches(recipeInput, world)) {
                return recipe;
            }
        }

        return net.minecraft.item.crafting.CraftingManager.findMatchingRecipe(recipeInput, world);
    }

    record ServerCraftResult(ItemStack crafted, ItemStack[] extractedInputs) {
    }
}
