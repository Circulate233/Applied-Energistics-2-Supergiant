package appeng.block.misc;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;

import java.util.List;

import appeng.core.localization.GuiText;

public class NotSoMysteriousCubeBlock extends AEDecorativeBlock {
    public NotSoMysteriousCubeBlock() {
        super(Material.IRON, 10.0F, 1000.0F);
        this.setOpaque();
        this.setFullSize();
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag advanced) {
        super.addInformation(stack, world, tooltip, advanced);
        tooltip.add(GuiText.NotSoMysteriousQuote.getLocal());
    }

    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT;
    }
}
