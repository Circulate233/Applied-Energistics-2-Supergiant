package appeng.block.misc;

import appeng.block.AEBaseTileBlock;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;
import appeng.server.services.compass.ServerCompassService;
import appeng.tile.misc.TileMysteriousCube;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

public class MysteriousCubeBlock extends AEBaseTileBlock<TileMysteriousCube> {
    public MysteriousCubeBlock() {
        super(Material.IRON);
        this.setHardness(10.0F);
        this.setResistance(1000.0F);
        this.setTileEntity(TileMysteriousCube.class);
        this.setOpaque();
        this.setFullSize();
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        if (world instanceof WorldServer && !world.isRemote) {
            ServerCompassService.notifyBlockChange((WorldServer) world, pos);
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
        if (world instanceof WorldServer && !world.isRemote) {
            ServerCompassService.notifyBlockChange((WorldServer) world, pos);
        }
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, net.minecraft.entity.player.EntityPlayer player) {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(Item.getItemFromBlock(this));
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(AEItems.CALCULATION_PROCESSOR_PRESS.stack());
        drops.add(AEItems.ENGINEERING_PROCESSOR_PRESS.stack());
        drops.add(AEItems.LOGIC_PROCESSOR_PRESS.stack());
        drops.add(AEItems.SILICON_PRESS.stack());
    }

    @Override
    public void addInformation(ItemStack itemStack, World worldIn, List<String> toolTip,
                               net.minecraft.client.util.ITooltipFlag advancedTooltips) {
        super.addInformation(itemStack, worldIn, toolTip, advancedTooltips);
        toolTip.add(GuiText.MysteriousQuote.getLocal());
    }

    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT;
    }
}
