package appeng.items.materials;

import appeng.api.ids.AEComponents;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public final class QuantumEntangledSingularityItem extends MaterialItem {
    private static final String QUANTUM_KEY_TOOLTIP = "gui.tooltips.ae2.QuantumKey";

    @SideOnly(Side.CLIENT)
    @Override
    protected void addCheckedInformation(ItemStack stack, World world, List<String> lines,
                                         ITooltipFlag advancedTooltips) {
        super.addCheckedInformation(stack, world, lines, advancedTooltips);

        NBTBase singularityId = AEComponents.ENTANGLED_SINGULARITY_ID_COMPONENT.readFrom(stack.getTagCompound());
        if (singularityId instanceof NBTTagLong id) {
            lines.add(I18n.format(QUANTUM_KEY_TOOLTIP, id.getLong()));
        }
    }
}
