package appeng.client.component;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Function;

public interface ICustomTextComponent extends ITextComponent {

    Reference2ObjectMap<Class<?>, Function<ByteBuf, ICustomTextComponent>> map = new Reference2ObjectOpenHashMap<>();

    void writeToByteBuf(ByteBuf buf);

    ICustomTextComponent readFromByteBuf(ByteBuf buf);

    @Nullable
    static ICustomTextComponent read(ByteBuf buf) {
        var aClass = ByteBufUtils.readUTF8String(buf);
        try {
            Class<?> c = Class.forName(aClass);
            var s = map.get(c);
            if (s == null) {
                var i = c.getConstructor().newInstance();
                var f = c.getMethod("readFromByteBuf", ByteBuf.class);
                MethodHandle ff = MethodHandles.lookup().unreflect(f);
                map.put(c, s = b -> {
                    try {
                        return (ICustomTextComponent) ff.invoke(i, b);
                    } catch (Throwable e) {
                        return null;
                    }
                });
            }
            return s.apply(buf);
        } catch (Exception e) {
            return null;
        }
    }
}