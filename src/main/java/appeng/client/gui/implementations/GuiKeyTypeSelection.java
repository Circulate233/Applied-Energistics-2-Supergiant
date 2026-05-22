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

package appeng.client.gui.implementations;

import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.storage.ISubGuiHost;
import appeng.client.Point;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Icon;
import appeng.client.gui.Rect2i;
import appeng.client.gui.style.GeneratedBackground;
import appeng.client.gui.style.GuiStyleManager;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.TabButton;
import appeng.container.AEBaseContainer;
import appeng.container.interfaces.IKeyTypeSelectionContainer;
import appeng.text.TextComponentItemStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.ITextComponent;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class GuiKeyTypeSelection<C extends AEBaseContainer & IKeyTypeSelectionContainer> extends AEBaseGui<C> {
    private final AEBaseGui<C> parent;
    private final KeyTypeCheckboxes keyTypesWidget = new KeyTypeCheckboxes();

    public GuiKeyTypeSelection(AEBaseGui<C> parent, ISubGuiHost subGuiHost, ITextComponent dialogTitle) {
        super(parent.getContainer(), parent.getContainer().getPlayerInventory(),
            GuiStyleManager.loadStyleDoc("/screens/key_type_selection.json"));
        this.parent = parent;

        widgets.add("back", new TabButton(Icon.BACK, TextComponentItemStack.of(subGuiHost.getMainContainerIcon()), this::returnToParent));
        widgets.add("keytypes", keyTypesWidget);
        setTextContent("dialog_title", dialogTitle);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        int selectedEntryCount = 0;
        AECheckbox selectedEntry = null;

        for (Map.Entry<AEKeyType, AECheckbox> entry : keyTypesWidget.checkboxes.entrySet()) {
            boolean selected = getContainer().getClientKeyTypeSelection().keyTypes().getBoolean(entry.getKey());
            entry.getValue().setSelected(selected);
            entry.getValue().enabled = true;

            if (selected) {
                selectedEntryCount++;
                selectedEntry = entry.getValue();
            }
        }

        if (selectedEntryCount == 1 && selectedEntry != null) {
            selectedEntry.enabled = false;
        }
    }

    private void returnToParent() {
        switchToScreen(parent);
        parent.returnFromSubScreen(this);
    }

    private void setHeight(int height) {
        if (this.style == null) {
            throw new IllegalStateException("GUI style has not been initialized");
        }
        GeneratedBackground generatedBackground = this.style.getGeneratedBackground();
        if (generatedBackground == null) {
            throw new IllegalStateException("GUI style is missing generated background");
        }
        generatedBackground.setHeight(height);
        this.ySize = height;
    }

    private class KeyTypeCheckboxes implements ICompositeWidget {
        private static final int PADDING = 6;
        private static final int KEY_TYPE_SPACING = AECheckbox.SIZE + PADDING;
        private final Object2ObjectLinkedOpenHashMap<AEKeyType, AECheckbox> checkboxes = new Object2ObjectLinkedOpenHashMap<>();
        private Rect2i bounds = new Rect2i(0, 0, 0, 0);

        @Override
        public void setPosition(Point position) {
            bounds = new Rect2i(position.x(), position.y(), bounds.width(), bounds.height());
        }

        @Override
        public void setSize(int width, int height) {
            bounds = new Rect2i(bounds.x(), bounds.y(), width, height);
        }

        @Override
        public Rect2i getBounds() {
            return bounds;
        }

        @Override
        public void populateScreen(Consumer<GuiButton> addWidget, Rect2i bounds, AEBaseGui<?> screen) {
            int xPos = this.bounds.x() + bounds.x();
            int yPos = this.bounds.y() + bounds.y();

            checkboxes.clear();

            for (AEKeyType keyType : getContainer().getClientKeyTypeSelection().keyTypes().keySet()) {
                ITextComponent text = keyType.getDescription();
                int textboxWidth = 24 + Minecraft.getMinecraft().fontRenderer.getStringWidth(text.getFormattedText());

                AECheckbox checkbox = new AECheckbox(xPos, yPos, textboxWidth, AECheckbox.SIZE,
                    Objects.requireNonNull(screen.getStyle()), text);
                checkbox.setChangeListener(() -> getContainer().selectKeyType(keyType, checkbox.isSelected()));
                addWidget.accept(checkbox);
                checkboxes.put(keyType, checkbox);

                yPos += KEY_TYPE_SPACING;
            }

            int height = this.bounds.y() + AEKeyTypes.getAll().size() * KEY_TYPE_SPACING + PADDING;
            GuiKeyTypeSelection.this.setHeight(height);
        }
    }
}
