package appeng.client.component;

import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Base64;

public final class TextComponents {
    private TextComponents() {
    }

    public static ITextComponent of(ItemStack itemStack) {
        return TextComponentItemStack.of(itemStack);
    }

    public static void writeToPacket(PacketBuffer buffer, @Nullable ITextComponent value) {
        buffer.writeBoolean(value != null);
        if (value == null) {
            return;
        }

        buffer.writeBoolean(value instanceof ICustomTextComponent);
        if (value instanceof ICustomTextComponent customTextComponent) {
            customTextComponent.writeToByteBuf(buffer);
        } else {
            buffer.writeTextComponent(value);
        }
    }

    @Nullable
    public static ITextComponent readFromPacket(PacketBuffer buffer) {
        if (!buffer.readBoolean()) {
            return null;
        }

        if (buffer.readBoolean()) {
            ITextComponent component = ICustomTextComponent.read(buffer);
            if (component == null) {
                throw new IllegalArgumentException("Could not read custom text component");
            }
            return component;
        }

        try {
            return buffer.readTextComponent();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read text component", e);
        }
    }

    public static String componentKey(@Nullable ITextComponent component) {
        if (component == null) {
            return "null";
        }

        if (component instanceof ICustomTextComponent customTextComponent) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            customTextComponent.writeToByteBuf(buffer);
            byte[] data = new byte[buffer.readableBytes()];
            buffer.getBytes(0, data);
            return "custom:" + Base64.getEncoder().encodeToString(data);
        }

        return ITextComponent.Serializer.componentToJson(component);
    }
}
