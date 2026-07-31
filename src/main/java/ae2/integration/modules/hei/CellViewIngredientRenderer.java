package ae2.integration.modules.hei;

import ae2.api.client.AEKeyRendering;
import ae2.api.stacks.AmountFormat;
import ae2.api.stacks.GenericStack;
import ae2.client.gui.me.common.StackSizeRenderer;
import ae2.core.localization.GuiText;
import ae2.util.Platform;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
final class CellViewIngredientRenderer implements IIngredientRenderer<ItemStack> {
    @Nullable
    private final GenericStack stack;

    CellViewIngredientRenderer(@Nullable GenericStack stack) {
        this.stack = stack;
    }

    @Override
    public void render(Minecraft minecraft, int xPosition, int yPosition, @Nullable ItemStack ingredient) {
        GenericStack stack = this.stack;
        if (stack == null) {
            return;
        }

        GlStateManager.pushMatrix();
        try {
            AEKeyRendering.drawInGui(minecraft, xPosition, yPosition, stack.what());
            if (stack.amount() > 1) {
                StackSizeRenderer.renderSizeLabel(minecraft.fontRenderer, xPosition, yPosition,
                    stack.what().formatAmount(stack.amount(), AmountFormat.SLOT), false);
            }
        } finally {
            GlStateManager.popMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public List<String> getTooltip(Minecraft minecraft, ItemStack ingredient, ITooltipFlag tooltipFlag) {
        return new ObjectArrayList<>();
    }

    static List<String> buildTooltip(GenericStack stack) {
        List<ITextComponent> keyTooltip = AEKeyRendering.getTooltip(stack.what());
        var result = new ObjectArrayList<String>(keyTooltip.size() + 2);
        for (int i = 0; i < keyTooltip.size(); i++) {
            result.add(keyTooltip.get(i).getFormattedText());
        }
        result.add(GuiText.CellViewAmount.getLocal(
            stack.what().formatAmount(stack.amount(), AmountFormat.FULL)));

        String modName = Platform.getModName(stack.what().getModId());
        if (!modName.isEmpty() && !result.contains(modName)) {
            result.add(GuiText.CellViewMod.getLocal(modName));
        }
        return result;
    }
}
