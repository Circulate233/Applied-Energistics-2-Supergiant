package ae2.mixins.hei;

import mezz.jei.bookmarks.BookmarkItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BookmarkItem.class, remap = false)
public interface AccessorBookmarkItem<I> {
    @Accessor("ingredient")
    I i_getIngredient();

    @Accessor("amount")
    long i_getAmount();

    @Accessor("amount")
    void i_setAmount(long amount);
}
