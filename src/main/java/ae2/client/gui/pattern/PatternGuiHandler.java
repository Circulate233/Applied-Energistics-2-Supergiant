package ae2.client.gui.pattern;

import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.container.pattern.ContainerCraftingPattern;
import ae2.container.pattern.ContainerProcessingPattern;
import ae2.crafting.pattern.AECraftingPattern;
import ae2.crafting.pattern.AEProcessingPattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public final class PatternGuiHandler {

    private static final Map<Class<?> ,GuiScreenGetter> PATTERN_GUI_MAP = new HashMap<>();

    static {
        register(AECraftingPattern.class, ( pattern)->{
            Minecraft minecraft = Minecraft.getMinecraft();
            ContainerCraftingPattern container = new ContainerCraftingPattern(minecraft.player.inventory, pattern);
            return new GuiCraftingPattern(container, minecraft.player.inventory);
        });
        register(AEProcessingPattern.class, (pattern)->{
            Minecraft minecraft = Minecraft.getMinecraft();
            ContainerProcessingPattern container = new ContainerProcessingPattern(minecraft.player.inventory, pattern);
            return new GuiProcessingPattern(container, minecraft.player.inventory);
        });
    }

    public static void register(Class<?> clazz, GuiScreenGetter getter) {
        PATTERN_GUI_MAP.put(clazz, getter);
    }

    public static boolean open(ItemStack pattern) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.world == null || pattern.isEmpty()) {
            return false;
        }

        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, minecraft.world);

        if (details == null) {
            return false;
        }

        GuiScreenGetter getter = PATTERN_GUI_MAP.get(details.getClass());

        if (getter == null) {
            return false;
        }

        minecraft.displayGuiScreen(getter.getGuiScreen(pattern));
        return true;
    }

    @FunctionalInterface
    public interface GuiScreenGetter {
        GuiScreen getGuiScreen(ItemStack pattern);
    }
}
