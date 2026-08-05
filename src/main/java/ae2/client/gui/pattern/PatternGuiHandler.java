package ae2.client.gui.pattern;

import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.container.pattern.ContainerCraftingPattern;
import ae2.container.pattern.ContainerProcessingPattern;
import ae2.crafting.pattern.AECraftingPattern;
import ae2.crafting.pattern.AEProcessingPattern;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PatternGuiHandler {

    private PatternGuiHandler() {
    }

    public static boolean open(ItemStack pattern) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.world == null || pattern.isEmpty()) {
            return false;
        }

        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, minecraft.world);
        if (details instanceof AECraftingPattern) {
            ContainerCraftingPattern container = new ContainerCraftingPattern(minecraft.player.inventory, pattern);
            minecraft.displayGuiScreen(new GuiCraftingPattern(container, minecraft.player.inventory));
            return true;
        }
        if (details instanceof AEProcessingPattern) {
            ContainerProcessingPattern container = new ContainerProcessingPattern(minecraft.player.inventory, pattern);
            minecraft.displayGuiScreen(new GuiProcessingPattern(container, minecraft.player.inventory));
            return true;
        }
        return false;
    }
}
