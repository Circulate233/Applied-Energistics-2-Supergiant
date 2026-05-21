package appeng.core.network.serverbound;

import appeng.api.config.FuzzyMode;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.container.interfaces.ICraftingGridContainer;
import appeng.core.network.ServerboundPacket;
import appeng.items.storage.ViewCellItem;
import appeng.me.storage.NullInventory;
import appeng.util.CraftingRecipeUtil;
import appeng.util.prioritylist.IPartitionList;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.AbstractObject2LongMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class FillCraftingGridFromRecipePacket extends ServerboundPacket {
    private static final int CRAFTING_GRID_SIZE = 9;

    private ResourceLocation recipeId;
    private List<List<ItemStack>> ingredientTemplates = createEmptyIngredientTemplates();
    private boolean craftMissing;

    public FillCraftingGridFromRecipePacket() {
    }

    public FillCraftingGridFromRecipePacket(ResourceLocation recipeId, List<List<ItemStack>> ingredientTemplates,
                                            boolean craftMissing) {
        this.recipeId = recipeId;
        this.ingredientTemplates = copyIngredientTemplates(ingredientTemplates);
        this.craftMissing = craftMissing;
    }

    private static List<List<ItemStack>> createEmptyIngredientTemplates() {
        List<List<ItemStack>> ingredientTemplates = new ObjectArrayList<>(CRAFTING_GRID_SIZE);
        for (int i = 0; i < CRAFTING_GRID_SIZE; i++) {
            ingredientTemplates.add(new ObjectArrayList<>());
        }
        return ingredientTemplates;
    }

    private static List<List<ItemStack>> convertSingleTemplates(NonNullList<ItemStack> ingredientTemplates) {
        Preconditions.checkArgument(ingredientTemplates.size() == CRAFTING_GRID_SIZE,
            "Got %s ingredient templates from client, expected %s",
            ingredientTemplates.size(), CRAFTING_GRID_SIZE);
        List<List<ItemStack>> result = createEmptyIngredientTemplates();
        for (int i = 0; i < ingredientTemplates.size(); i++) {
            ItemStack stack = ingredientTemplates.get(i);
            if (!stack.isEmpty()) {
                result.get(i).add(stack.copy());
            }
        }
        return result;
    }

    private static List<List<ItemStack>> copyIngredientTemplates(List<List<ItemStack>> ingredientTemplates) {
        Preconditions.checkArgument(ingredientTemplates.size() == CRAFTING_GRID_SIZE,
            "Got %s ingredient template slots from client, expected %s",
            ingredientTemplates.size(), CRAFTING_GRID_SIZE);
        List<List<ItemStack>> result = createEmptyIngredientTemplates();
        for (int i = 0; i < ingredientTemplates.size(); i++) {
            List<ItemStack> slotTemplates = ingredientTemplates.get(i);
            if (slotTemplates == null) {
                continue;
            }
            List<ItemStack> copiedSlotTemplates = result.get(i);
            for (ItemStack stack : slotTemplates) {
                if (stack != null && !stack.isEmpty()) {
                    copiedSlotTemplates.add(stack.copy());
                }
            }
        }
        return result;
    }

    @Override
    protected void read(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        this.recipeId = packetBuffer.readBoolean() ? packetBuffer.readResourceLocation() : null;
        int size = packetBuffer.readInt();
        Preconditions.checkArgument(size == CRAFTING_GRID_SIZE,
            "Got %s ingredient template slots from client, expected %s",
            size, CRAFTING_GRID_SIZE);
        this.ingredientTemplates = createEmptyIngredientTemplates();
        for (int i = 0; i < size; i++) {
            int alternatives = packetBuffer.readInt();
            List<ItemStack> slotTemplates = this.ingredientTemplates.get(i);
            for (int j = 0; j < alternatives; j++) {
                try {
                    slotTemplates.add(packetBuffer.readItemStack());
                } catch (IOException e) {
                    throw new IllegalArgumentException("Could not read recipe transfer ingredient", e);
                }
            }
        }
        this.craftMissing = packetBuffer.readBoolean();
    }

    @Override
    protected void write(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        packetBuffer.writeBoolean(this.recipeId != null);
        if (this.recipeId != null) {
            packetBuffer.writeResourceLocation(this.recipeId);
        }
        packetBuffer.writeInt(this.ingredientTemplates.size());
        for (List<ItemStack> slotTemplates : this.ingredientTemplates) {
            packetBuffer.writeInt(slotTemplates.size());
            for (ItemStack stack : slotTemplates) {
                packetBuffer.writeItemStack(stack);
            }
        }
        packetBuffer.writeBoolean(this.craftMissing);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (!(player.openContainer instanceof ICraftingGridContainer container)) {
            return;
        }
        IEnergySource energy = container.getEnergySource();
        ICraftingService craftingService;
        IStorageService storageService;
        MEStorage networkStorage;
        KeyCounter cachedStorage;

        IGridNode node = container.getGridNode();
        if (node != null && container.getLinkStatus().connected()) {
            craftingService = node.getGrid().getCraftingService();
            storageService = node.getGrid().getStorageService();
            networkStorage = storageService.getInventory();
            cachedStorage = storageService.getCachedInventory();
        } else {
            craftingService = null;
            storageService = null;
            networkStorage = NullInventory.of();
            cachedStorage = new KeyCounter();
        }

        InternalInventory craftMatrix = container.getCraftingMatrix();
        IPartitionList filter = ViewCellItem.createItemFilter(container.getViewCells());
        NonNullList<Ingredient> ingredients = getDesiredIngredients();

        Object2ObjectMap<AEItemKey, IntList> toAutoCraft = new Object2ObjectLinkedOpenHashMap<>();
        boolean touchedGridStorage = false;

        int slotsToFill = Math.min(craftMatrix.size(), ingredients.size());
        for (int slot = 0; slot < slotsToFill; slot++) {
            ItemStack currentItem = craftMatrix.getStackInSlot(slot);
            Ingredient ingredient = ingredients.get(slot);

            if (!currentItem.isEmpty()) {
                if (ingredient.apply(currentItem)) {
                    continue;
                }

                AEItemKey in = AEItemKey.of(currentItem);
                long inserted = in != null
                    ? StorageHelper.poweredInsert(energy, networkStorage, in, currentItem.getCount(),
                    container.getActionSource())
                    : 0;
                if (inserted > 0) {
                    touchedGridStorage = true;
                }
                if (inserted < currentItem.getCount()) {
                    currentItem = currentItem.copy();
                    currentItem.shrink((int) inserted);
                } else {
                    currentItem = ItemStack.EMPTY;
                }
                if (!currentItem.isEmpty()) {
                    player.inventory.addItemStackToInventory(currentItem);
                }
                craftMatrix.setItemDirect(slot, currentItem.isEmpty() ? ItemStack.EMPTY : currentItem);
            }

            if (ingredient == Ingredient.EMPTY) {
                continue;
            }

            if (currentItem.isEmpty()) {
                for (AEItemKey what : findBestMatchingItemStack(ingredient, filter, cachedStorage)) {
                    long extracted = StorageHelper.poweredExtraction(energy, networkStorage, what, 1,
                        container.getActionSource());
                    if (extracted > 0) {
                        touchedGridStorage = true;
                        currentItem = what.toStack(Ints.saturatedCast(extracted));
                        break;
                    }
                }
            }

            if (currentItem.isEmpty()) {
                currentItem = takeIngredientFromPlayer(container, player, ingredient);
            }

            craftMatrix.setItemDirect(slot, currentItem);

            if (currentItem.isEmpty() && this.craftMissing && craftingService != null) {
                final int craftSlot = slot;
                findCraftableKey(ingredient, craftingService).ifPresent(key -> {
                    IntList slots = toAutoCraft.computeIfAbsent(key, ignored -> new IntArrayList());
                    slots.add(craftSlot);
                });
            }
        }

        player.openContainer.onCraftMatrixChanged(craftMatrix.toContainer());

        if (!toAutoCraft.isEmpty()) {
            if (touchedGridStorage && storageService != null) {
                storageService.invalidateCache();
            }

            List<ICraftingGridContainer.AutoCraftEntry> stacks = new ObjectArrayList<>(
                toAutoCraft.size());
            for (Object2ObjectMap.Entry<AEItemKey, IntList> entry : toAutoCraft.object2ObjectEntrySet()) {
                stacks.add(new ICraftingGridContainer.AutoCraftEntry(entry.getKey(), entry.getValue()));
            }
            container.startAutoCrafting(stacks);
        }
    }

    private ItemStack takeIngredientFromPlayer(ICraftingGridContainer container, EntityPlayerMP player, Ingredient ingredient) {
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            if (container.isPlayerInventorySlotLocked(i)) {
                continue;
            }

            ItemStack item = player.inventory.mainInventory.get(i);
            if (!item.isEmpty() && ingredient.apply(item)) {
                ItemStack result = item.splitStack(1);
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private NonNullList<Ingredient> getDesiredIngredients() {
        if (this.recipeId != null) {
            IRecipe recipe = CraftingManager.REGISTRY.getObject(this.recipeId);
            if (recipe != null) {
                return CraftingRecipeUtil.ensure3by3CraftingMatrix(recipe);
            }
        }

        NonNullList<Ingredient> ingredients = NonNullList.withSize(CRAFTING_GRID_SIZE, Ingredient.EMPTY);
        Preconditions.checkArgument(ingredients.size() == this.ingredientTemplates.size(),
            "Got %s ingredient template slots from client, expected %s",
            this.ingredientTemplates.size(), ingredients.size());
        for (int i = 0; i < ingredients.size(); i++) {
            List<ItemStack> slotTemplates = this.ingredientTemplates.get(i);
            if (!slotTemplates.isEmpty()) {
                ingredients.set(i, Ingredient.fromStacks(slotTemplates.toArray(new ItemStack[0])));
            }
        }
        return ingredients;
    }

    private List<AEItemKey> findBestMatchingItemStack(Ingredient ingredient, IPartitionList filter, KeyCounter storage) {
        List<Object2LongMap.Entry<AEItemKey>> result = new ObjectArrayList<>();
        for (ItemStack stack : ingredient.getMatchingStacks()) {
            AEItemKey key = AEItemKey.of(stack);
            if (key == null || filter != null && !filter.isListed(key)) {
                continue;
            }
            for (Object2LongMap.Entry<AEKey> entry : storage.findFuzzy(key, FuzzyMode.IGNORE_ALL)) {
                AEKey foundKey = entry.getKey();
                if (foundKey instanceof AEItemKey && ((AEItemKey) foundKey).matches(ingredient)) {
                    result.add(new AbstractObject2LongMap.BasicEntry<>((AEItemKey) foundKey, entry.getLongValue()));
                }
            }
        }
        result.sort((a, b) -> Long.compare(b.getLongValue(), a.getLongValue()));

        List<AEItemKey> keys = new ObjectArrayList<>(result.size());
        for (Object2LongMap.Entry<AEItemKey> entry : result) {
            keys.add(entry.getKey());
        }
        return keys;
    }

    private Optional<AEItemKey> findCraftableKey(Ingredient ingredient, ICraftingService craftingService) {
        for (ItemStack stack : ingredient.getMatchingStacks()) {
            AEItemKey key = AEItemKey.of(stack);
            if (key == null) {
                continue;
            }
            AEKey craftable = craftingService.getFuzzyCraftable(key,
                candidate -> candidate instanceof AEItemKey && ((AEItemKey) candidate).matches(ingredient));
            if (craftable instanceof AEItemKey) {
                return Optional.of((AEItemKey) craftable);
            }
        }
        return Optional.empty();
    }
}
