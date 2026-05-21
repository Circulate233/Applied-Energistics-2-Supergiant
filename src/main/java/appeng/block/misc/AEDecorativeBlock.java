package appeng.block.misc;

import appeng.block.AEBaseBlock;
import net.minecraft.block.material.Material;

public class AEDecorativeBlock extends AEBaseBlock {
    public AEDecorativeBlock(Material material, float hardness, float resistance) {
        super(material);
        this.setHardness(hardness);
        this.setResistance(resistance);
    }
}

