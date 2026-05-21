package appeng.helpers;

import appeng.api.ids.AEComponents;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.container.ISubGui;
import appeng.core.gui.locator.ItemGuiHostLocator;
import appeng.items.contents.StackDependentSupplier;
import appeng.items.tools.powered.WirelessCraftingTerminalItem;
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

public class WirelessCraftingTerminalGuiHost<T extends WirelessCraftingTerminalItem>
    extends WirelessTerminalGuiHost<T> implements ISegmentedInventory {
    private final SupplierInternalInventory<InternalInventory> craftingGrid;

    public WirelessCraftingTerminalGuiHost(T item, EntityPlayer player, ItemGuiHostLocator locator,
                                           BiConsumer<EntityPlayer, ISubGui> returnToMainGui) {
        super(item, player, locator, returnToMainGui);
        this.craftingGrid = new SupplierInternalInventory<>(
            new StackDependentSupplier<>(this::getItemStack, stack -> createCraftingInv(player, stack)));
    }

    private static InternalInventory createCraftingInv(EntityPlayer player, ItemStack stack) {
        var craftingGrid = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inv) {
                NBTTagCompound tag = stack.getTagCompound();
                if (tag == null) {
                    tag = new NBTTagCompound();
                    stack.setTagCompound(tag);
                }
                NBTTagCompound invTag = new NBTTagCompound();
                inv.writeToNBT(invTag, "items");
                AEComponents.CRAFTING_INV_COMPONENT.writeTo(tag, invTag);
            }

            @Override
            public boolean isClientSide() {
                return player.world.isRemote;
            }
        }, 9);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            NBTTagCompound invTag = AEComponents.CRAFTING_INV_COMPONENT.readFrom(tag);
            if (invTag != null) {
                craftingGrid.readFromNBT(invTag, "items");
            }
        }
        return craftingGrid;
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        return id.equals(CraftingTerminalPart.INV_CRAFTING) ? craftingGrid : null;
    }
}
