package appeng.client.gui.implementations;

import appeng.api.client.AEKeyRendering;
import appeng.api.config.LockCraftingMode;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Icon;
import appeng.client.gui.Rect2i;
import appeng.client.gui.Tooltip;
import appeng.container.implementations.ContainerPatternProvider;
import appeng.core.localization.GuiText;
import appeng.core.localization.InGameTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.jetbrains.annotations.Nullable;

public class PatternProviderLockReason implements ICompositeWidget {
    private final GuiPatternProvider screen;
    private boolean visible;
    private int x;
    private int y;

    public PatternProviderLockReason(GuiPatternProvider screen) {
        this.screen = screen;
    }

    @Override
    public void setPosition(Point position) {
        this.x = position.x();
        this.y = position.y();
    }

    @Override
    public void setSize(int width, int height) {
    }

    @Override
    public Rect2i getBounds() {
        return new Rect2i(this.x, this.y, 126, 16);
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void drawForegroundLayer(Rect2i bounds, Point mouse) {
        ContainerPatternProvider container = this.screen.getContainer();
        Icon icon;
        ITextComponent text;
        int color;

        if (container.getCraftingLockedReason() == LockCraftingMode.NONE) {
            icon = Icon.UNLOCKED;
            text = GuiText.CraftingLockIsUnlocked.text();
            color = 0x7DA9D2;
        } else {
            icon = Icon.LOCKED;
            text = GuiText.CraftingLockIsLocked.text();
            color = 0xC1424B;
        }

        icon.getBlitter().dest(this.x, this.y).blit();
        Minecraft.getMinecraft().fontRenderer.drawString(text.getFormattedText(), this.x + 15, this.y + 5, color);
    }

    @Nullable
    @Override
    public Tooltip getTooltip(int mouseX, int mouseY) {
        ContainerPatternProvider container = this.screen.getContainer();
        LockCraftingMode reason = container.getCraftingLockedReason();
        return switch (reason) {
            case LOCK_UNTIL_PULSE -> new Tooltip(InGameTooltip.CraftingLockedUntilPulse.text());
            case LOCK_WHILE_HIGH -> new Tooltip(InGameTooltip.CraftingLockedByRedstoneSignal.text());
            case LOCK_WHILE_LOW -> new Tooltip(InGameTooltip.CraftingLockedByLackOfRedstoneSignal.text());
            case LOCK_UNTIL_RESULT -> {
                GenericStack unlockStack = container.getUnlockStack();
                ITextComponent stackName = unlockStack != null
                    ? AEKeyRendering.getDisplayName(unlockStack.what())
                    : GuiText.Error.text();
                ITextComponent stackAmount = unlockStack != null
                    ? new TextComponentString(unlockStack.what().formatAmount(unlockStack.amount(), AmountFormat.FULL))
                    : GuiText.Error.text();
                yield new Tooltip(InGameTooltip.CraftingLockedUntilResult.text(stackName, stackAmount));
            }
            case NONE -> null;
        };
    }
}

