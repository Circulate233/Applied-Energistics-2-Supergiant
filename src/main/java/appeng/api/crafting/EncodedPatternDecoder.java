package appeng.api.crafting;

import appeng.api.stacks.AEItemKey;
import net.minecraft.world.World;

@FunctionalInterface
public interface EncodedPatternDecoder<T extends IPatternDetails> {
    T decode(AEItemKey what, World level);
}
