package appeng.api.behaviors;

import appeng.api.stacks.AEKey;
import net.minecraft.util.text.ITextComponent;

/**
 * Describes the action of emptying an item into the storage network.
 */
public record EmptyingAction(ITextComponent description, AEKey what, long maxAmount) {
}
