package appeng.helpers;

import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.container.ISubGui;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.items.contents.StackDependentSupplier;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.items.tools.powered.WirelessTerminals;
import appeng.parts.reporting.CraftingTerminalPart;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.SupplierInternalInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class WirelessCraftingTerminalGuiHost extends WirelessTerminalGuiHost<WirelessTerminalItem>
    implements ISegmentedInventory {
    private final SupplierInternalInventory<InternalInventory> craftingGrid;

    public WirelessCraftingTerminalGuiHost(WirelessTerminalItem stackItem,
                                           WirelessTerminalItem terminalItem, EntityPlayer player,
                                           ItemGuiHostLocator locator,
                                           BiConsumer<EntityPlayer, ISubGui> returnToMainGui) {
        super(stackItem, terminalItem, player, locator, returnToMainGui);
        this.craftingGrid = new SupplierInternalInventory<>(
            new StackDependentSupplier<>(this::getItemStack, stack -> createCraftingInv(player, stack, terminalItem)));
    }

    private static InternalInventory createCraftingInv(EntityPlayer player, ItemStack stack,
                                                       WirelessTerminalItem terminal) {
        var craftingGrid = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inv) {
                NBTTagCompound invTag = new NBTTagCompound();
                inv.writeToNBT(invTag, "items");
                WirelessTerminals.getTerminalData(stack, terminal)
                                 .setTag(WirelessTerminals.TAG_CRAFTING_GRID, invTag.copy());
            }

            @Override
            public boolean isClientSide() {
                return player.world.isRemote;
            }
        }, 9);
        NBTTagCompound tag = WirelessTerminals.getExistingTerminalData(stack, terminal);
        if (tag != null && tag.hasKey(WirelessTerminals.TAG_CRAFTING_GRID, 10)) {
            craftingGrid.readFromNBT(tag.getCompoundTag(WirelessTerminals.TAG_CRAFTING_GRID), "items");
        }
        return craftingGrid;
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        return id.equals(CraftingTerminalPart.INV_CRAFTING) ? craftingGrid : null;
    }
}
