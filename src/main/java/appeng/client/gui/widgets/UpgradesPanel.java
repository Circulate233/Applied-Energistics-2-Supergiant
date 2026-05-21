/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.gui.widgets;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.Upgrades;
import appeng.client.Point;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Rect2i;
import appeng.client.gui.Rects;
import appeng.client.gui.Tooltip;
import appeng.client.gui.style.Blitter;
import appeng.container.slot.AppEngSlot;
import net.minecraft.client.gui.Gui;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UpgradesPanel implements ICompositeWidget {

    private static final int SLOT_SIZE = 18;
    private static final int LEFT_PADDING = 5;
    private static final int RIGHT_PADDING = 6;
    private static final int TOP_PADDING = 5;
    private static final int BOTTOM_PADDING = 7;
    private static final int MAX_ROWS = 8;
    private static final int OFFSET_X = 2;
    private static final int SLOT_X = 4;
    private static final int SLOT_Y = TOP_PADDING + 1;

    private final List<Slot> slots;
    private final Supplier<List<ITextComponent>> tooltipSupplier;
    private final Blitter background;
    private final Blitter innerCorner;

    private Point screenOrigin = Point.ZERO;
    private int x;
    private int y;

    public UpgradesPanel(List<Slot> slots, IUpgradeableObject upgradeableObject) {
        this(slots, () -> Upgrades.getTooltipLinesForMachine(upgradeableObject.getUpgrades().getUpgradableItem()));
    }

    public UpgradesPanel(List<Slot> slots, Supplier<List<ITextComponent>> tooltipSupplier) {
        this(slots, tooltipSupplier,
            Blitter.texture("guis/extra_panels.png", 128, 128),
            Blitter.texture("guis/extra_panels.png", 128, 128).copy().src(12, 33, SLOT_SIZE, SLOT_SIZE));
    }

    protected UpgradesPanel(List<Slot> slots, Supplier<List<ITextComponent>> tooltipSupplier,
                            Blitter background, Blitter innerCorner) {
        this.slots = slots;
        this.tooltipSupplier = tooltipSupplier;
        this.background = background;
        this.innerCorner = innerCorner;
    }

    private void drawSlot(int x, int y, boolean borderLeft, boolean borderTop, boolean borderRight,
                          boolean borderBottom) {
        int srcX = LEFT_PADDING;
        int srcY = TOP_PADDING;
        int srcWidth = SLOT_SIZE;
        int srcHeight = SLOT_SIZE;

        if (borderLeft) {
            x -= LEFT_PADDING;
            srcX = 0;
            srcWidth += LEFT_PADDING;
        }
        if (borderRight) {
            srcWidth += RIGHT_PADDING;
        }
        if (borderTop) {
            y -= TOP_PADDING;
            srcY = 0;
            srcHeight += TOP_PADDING;
        }
        if (borderBottom) {
            srcHeight += BOTTOM_PADDING;
        }

        this.background.copy().src(srcX, srcY, srcWidth, srcHeight).dest(x, y).blit();
    }

    @Override
    public void setPosition(Point position) {
        x = position.x() + OFFSET_X;
        y = position.y();
    }

    @Override
    public void setSize(int width, int height) {
    }

    @Override
    public Rect2i getBounds() {
        int slotCount = getUpgradeSlotCount();
        int height = TOP_PADDING + BOTTOM_PADDING + Math.min(MAX_ROWS, slotCount) * SLOT_SIZE;
        int width = LEFT_PADDING + RIGHT_PADDING + (slotCount + MAX_ROWS - 1) / MAX_ROWS * SLOT_SIZE;
        return new Rect2i(x, y, width, height);
    }

    @Override
    public void populateScreen(Consumer<net.minecraft.client.gui.GuiButton> addWidget, Rect2i bounds, AEBaseGui<?> screen) {
        this.screenOrigin = Point.fromTopLeft(bounds);
    }

    @Override
    public void updateBeforeRender() {
        int slotOriginX = this.x + SLOT_X;
        int slotOriginY = this.y + SLOT_Y;

        for (Slot slot : slots) {
            if (slot instanceof AppEngSlot appEngSlot && !appEngSlot.isSlotEnabled()) {
                continue;
            }

            slot.xPos = slotOriginX;
            slot.yPos = slotOriginY;
            slotOriginY += SLOT_SIZE;
        }
    }

    @Override
    public void drawBackgroundLayer(Rect2i bounds, Point mouse) {
        int slotCount = getUpgradeSlotCount();
        if (slotCount <= 0) {
            return;
        }

        int slotOriginX = screenOrigin.x() + this.x + LEFT_PADDING;
        int slotOriginY = screenOrigin.y() + this.y + TOP_PADDING;

        for (int i = 0; i < slotCount; i++) {
            int row = i % MAX_ROWS;
            int col = i / MAX_ROWS;

            int x = slotOriginX + col * SLOT_SIZE;
            int y = slotOriginY + row * SLOT_SIZE;

            boolean borderLeft = col == 0;
            boolean borderTop = row == 0;
            boolean lastSlot = i + 1 >= slotCount;
            boolean lastRow = row + 1 >= MAX_ROWS;
            boolean borderBottom = lastRow || lastSlot;
            boolean borderRight = i >= slotCount - MAX_ROWS;

            drawSlot(x, y, borderLeft, borderTop, borderRight, borderBottom);

            if (col > 0 && lastSlot && !lastRow) {
                this.innerCorner.dest(x, y + SLOT_SIZE).blit();
            }
        }

        int slotLeft = screenOrigin.x() + this.x + SLOT_X - 1;
        int slotTop = screenOrigin.y() + this.y + SLOT_Y - 1;
        int slotRight = slotLeft + SLOT_SIZE;
        int slotBottom = slotTop + slotCount * SLOT_SIZE;
        Gui.drawRect(slotLeft + 1, slotTop, slotRight - 1, slotTop + 1, 0xFFF2F2F2);
        Gui.drawRect(slotLeft + 1, slotBottom, slotRight - 1, slotBottom + 1, 0xFFF2F2F2);
        Gui.drawRect(slotLeft, slotTop, slotLeft + 1, slotBottom + 1, 0xFFF2F2F2);
        Gui.drawRect(slotRight - 1, slotTop, slotRight, slotBottom + 1, 0xFFF2F2F2);
    }

    @Override
    public void addExclusionZones(List<Rect2i> exclusionZones, Rect2i screenBounds) {
        int offsetX = screenBounds.x();
        int offsetY = screenBounds.y();
        int slotCount = getUpgradeSlotCount();
        int margin = 2;

        int fullCols = slotCount / MAX_ROWS;
        int rightEdge = offsetX + x;
        if (fullCols > 0) {
            int fullColWidth = LEFT_PADDING + RIGHT_PADDING + fullCols * SLOT_SIZE;
            exclusionZones.add(Rects.expand(new Rect2i(
                rightEdge,
                offsetY + y,
                fullColWidth,
                TOP_PADDING + BOTTOM_PADDING + MAX_ROWS * SLOT_SIZE), margin));
            rightEdge += fullColWidth;
        }

        int remaining = slotCount - fullCols * MAX_ROWS;
        if (remaining > 0) {
            exclusionZones.add(Rects.expand(new Rect2i(
                rightEdge,
                offsetY + y,
                SLOT_SIZE + (fullCols > 0 ? 0 : LEFT_PADDING + RIGHT_PADDING),
                TOP_PADDING + BOTTOM_PADDING + remaining * SLOT_SIZE), margin));
        }
    }

    @Nullable
    @Override
    public Tooltip getTooltip(int mouseX, int mouseY) {
        if (getUpgradeSlotCount() == 0) {
            return null;
        }

        List<ITextComponent> tooltip = this.tooltipSupplier.get();
        if (tooltip.isEmpty()) {
            return null;
        }

        return new Tooltip(tooltip);
    }

    private int getUpgradeSlotCount() {
        int count = 0;
        for (Slot slot : slots) {
            if (slot instanceof AppEngSlot appEngSlot && appEngSlot.isSlotEnabled()) {
                count++;
            }
        }
        return count;
    }
}
