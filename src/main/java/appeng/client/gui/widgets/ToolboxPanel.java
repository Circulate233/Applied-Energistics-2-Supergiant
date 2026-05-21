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

import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Rect2i;
import appeng.client.gui.Tooltip;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.GuiStyle;
import appeng.core.localization.GuiText;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.Nullable;

public class ToolboxPanel implements ICompositeWidget {

    private final Blitter background;
    private final ITextComponent toolbeltName;
    private Rect2i bounds = new Rect2i(0, 0, 0, 0);

    public ToolboxPanel(GuiStyle style, ITextComponent toolbeltName) {
        this.background = style.getImage("toolbox");
        this.toolbeltName = toolbeltName;
    }

    @Override
    public void setPosition(Point position) {
        this.bounds = new Rect2i(position.x(), position.y(), bounds.width(), bounds.height());
    }

    @Override
    public void setSize(int width, int height) {
        this.bounds = new Rect2i(bounds.x(), bounds.y(), width, height);
    }

    @Override
    public Rect2i getBounds() {
        return bounds;
    }

    @Override
    public void drawBackgroundLayer(Rect2i bounds, Point mouse) {
        background.dest(
            bounds.x() + this.bounds.x(),
            bounds.y() + this.bounds.y(),
            this.bounds.width(),
            this.bounds.height()).blit();
    }

    @Nullable
    @Override
    public Tooltip getTooltip(int mouseX, int mouseY) {
        var hint = GuiText.UpgradeToolbelt.text();
        hint.setStyle(new Style().setColor(TextFormatting.GRAY));
        return new Tooltip(this.toolbeltName, hint);
    }
}

