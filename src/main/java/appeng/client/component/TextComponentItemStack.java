package appeng.client.component;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.jspecify.annotations.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class TextComponentItemStack implements ICustomTextComponent {
    private final ItemStack itemStack;
    private Style style = new Style();
    private final ObjectList<ITextComponent> siblings = new ObjectArrayList<>();

    public TextComponentItemStack() {
        this(ItemStack.EMPTY);
    }

    public TextComponentItemStack(ItemStack itemStack) {
        this.itemStack = itemStack.copy();
    }

    private TextComponentItemStack(ItemStack itemStack, @Nullable ITextComponent decorations) {
        this(itemStack);
        if (decorations != null) {
            this.setStyle(decorations.getStyle().createDeepCopy());
            for (ITextComponent sibling : decorations.getSiblings()) {
                this.appendSibling(sibling.createCopy());
            }
        }
    }

    public static ITextComponent of(ItemStack itemStack) {
        return new TextComponentItemStack(itemStack);
    }

    @Override
    public void writeToByteBuf(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, getClass().getName());
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeItemStack(this.itemStack);
        TextComponents.writeToPacket(buffer, createDecorations());
    }

    @Override
    public ICustomTextComponent readFromByteBuf(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        try {
            return new TextComponentItemStack(buffer.readItemStack(), TextComponents.readFromPacket(buffer));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read item stack text component", e);
        }
    }

    @Override
    public ITextComponent setStyle(Style style) {
        this.style = style == null ? new Style() : style;
        for (ITextComponent sibling : this.siblings) {
            sibling.getStyle().setParentStyle(this.style);
        }
        return this;
    }

    @Override
    public Style getStyle() {
        return this.style;
    }

    @Override
    public ITextComponent appendText(String text) {
        return this.appendSibling(new TextComponentString(text));
    }

    @Override
    public ITextComponent appendSibling(ITextComponent component) {
        component.getStyle().setParentStyle(this.style);
        this.siblings.add(component);
        return this;
    }

    @Override
    public String getUnformattedComponentText() {
        return resolve().getUnformattedComponentText();
    }

    @Override
    public String getUnformattedText() {
        return resolve().getUnformattedText();
    }

    @Override
    public String getFormattedText() {
        return resolve().getFormattedText();
    }

    @Override
    public List<ITextComponent> getSiblings() {
        return this.siblings;
    }

    @Override
    public ITextComponent createCopy() {
        return new TextComponentItemStack(this.itemStack, createDecorations());
    }

    @Override
    public @NonNull Iterator<ITextComponent> iterator() {
        return resolve().iterator();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TextComponentItemStack other)) {
            return false;
        }
        return ItemStack.areItemStacksEqual(this.itemStack, other.itemStack)
            && Objects.equals(this.style, other.style)
            && Objects.equals(this.siblings, other.siblings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.itemStack.isEmpty() ? null : this.itemStack.getItem(),
            this.itemStack.getMetadata(),
            this.itemStack.getCount(),
            this.itemStack.getTagCompound(),
            this.style,
            this.siblings);
    }

    private ITextComponent createDecorations() {
        TextComponentString decorations = new TextComponentString("");
        decorations.setStyle(this.style.createDeepCopy());
        for (ITextComponent sibling : this.siblings) {
            decorations.appendSibling(sibling.createCopy());
        }
        return decorations;
    }

    private ITextComponent resolve() {
        ITextComponent displayName = new TextComponentString(this.itemStack.getDisplayName());
        if (this.itemStack.hasDisplayName()) {
            displayName.getStyle().setItalic(Boolean.TRUE);
        }

        TextComponentString root = new TextComponentString("");
        root.appendText("[");
        root.appendSibling(displayName);
        root.appendText("]");

        if (!this.itemStack.isEmpty()) {
            NBTTagCompound stackTag = new NBTTagCompound();
            this.itemStack.writeToNBT(stackTag);
            root.getStyle().setHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_ITEM, new TextComponentString(stackTag.toString())));
            root.getStyle().setColor(this.itemStack.getItem().getForgeRarity(this.itemStack).getColor());
        }

        Style mergedStyle = this.style.createDeepCopy();
        mergedStyle.setParentStyle(root.getStyle());
        root.setStyle(mergedStyle);

        for (ITextComponent sibling : this.siblings) {
            root.appendSibling(sibling.createCopy());
        }

        return root;
    }
}
