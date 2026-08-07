/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package ae2.me.cells;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorageChangeListener;
import ae2.api.storage.cells.CellState;
import ae2.api.storage.cells.StorageCell;
import ae2.items.contents.CellConfig;
import ae2.text.TextComponentItemStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import java.util.Objects;

public class CreativeCellInventory implements StorageCell {
    private final ObjectSet<AEKey> configured;
    private final ItemStack stack;
    private final ObjectList<ListenerRegistration> listeners = new ObjectArrayList<>();

    protected CreativeCellInventory(ItemStack stack) {
        this.stack = stack;
        this.configured = new ObjectOpenHashSet<>(CellConfig.create(stack).keySet());
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (what == null || amount <= 0) {
            return 0;
        }
        return configured.contains(what) ? amount : 0;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (what == null || amount <= 0) {
            return 0;
        }
        return configured.contains(what) ? amount : 0;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        removeInvalidListeners();
        for (AEKey key : this.configured) {
            out.add(key, Long.MAX_VALUE);
        }
    }

    @Override
    public boolean isPreferredStorageFor(AEKey input, IActionSource source) {
        return this.configured.contains(input);
    }

    @Override
    public CellState getStatus() {
        return CellState.TYPES_FULL;
    }

    @Override
    public double getIdleDrain() {
        return 0.0d;
    }

    @Override
    public boolean canFitInsideCell() {
        return configured.isEmpty();
    }

    @Override
    public ITextComponent getDescription() {
        return TextComponentItemStack.of(stack);
    }

    @Override
    public void persist() {
    }

    @Override
    public void addListener(MEStorageChangeListener listener, Object verificationToken) {
        Objects.requireNonNull(listener, "listener");
        for (int i = 0; i < this.listeners.size(); i++) {
            if (this.listeners.get(i).listener == listener) {
                throw new IllegalStateException("The storage listener is already registered.");
            }
        }
        this.listeners.add(new ListenerRegistration(listener, verificationToken));
    }

    @Override
    public void removeListener(MEStorageChangeListener listener) {
        for (int i = this.listeners.size() - 1; i >= 0; i--) {
            if (this.listeners.get(i).listener == listener) {
                this.listeners.remove(i);
            }
        }
    }

    private void removeInvalidListeners() {
        for (int i = this.listeners.size() - 1; i >= 0; i--) {
            var registration = this.listeners.get(i);
            if (!registration.listener.isValid(registration.verificationToken)) {
                this.listeners.remove(i);
            }
        }
    }

    private record ListenerRegistration(MEStorageChangeListener listener, Object verificationToken) {
    }
}
