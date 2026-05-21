package appeng.crafting.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.stacks.AEItemKey;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class AEPatternDecoder implements IPatternDetailsDecoder {
    public static final AEPatternDecoder INSTANCE = new AEPatternDecoder();

    @Override
    public boolean isEncodedPattern(ItemStack stack) {
        return stack.getItem() instanceof EncodedPatternItem;
    }

    @Nullable
    @Override
    public IPatternDetails decodePattern(AEItemKey what, World level) {
        if (level == null || what == null || !(what.getItem() instanceof EncodedPatternItem<?> encodedPatternItem)) {
            return null;
        }

        return encodedPatternItem.decode(what, level);
    }
}
