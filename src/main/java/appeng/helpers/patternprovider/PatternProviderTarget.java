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

package appeng.helpers.patternprovider;

import appeng.api.AECapabilities;
import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.config.Actionable;
import appeng.api.config.PatternProviderInsertionMode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.MEStorage;
import appeng.me.storage.CompositeStorage;
import appeng.parts.automation.StackWorldBehaviors;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public interface PatternProviderTarget {
    @Nullable
    static PatternProviderTarget get(World level, BlockPos pos, EnumFacing side, IActionSource src) {
        if (!(level instanceof WorldServer)) {
            return null;
        }

        TileEntity blockEntity = level.getTileEntity(pos);
        MEStorage storage = null;
        if (blockEntity != null && blockEntity.hasCapability(AECapabilities.ME_STORAGE, side)) {
            storage = blockEntity.getCapability(AECapabilities.ME_STORAGE, side);
        }

        if (storage != null) {
            return wrapMeStorage(storage, src);
        }

        Map<AEKeyType, ExternalStorageStrategy> strategies = StackWorldBehaviors.createExternalStorageStrategies(
            (WorldServer) level,
            pos,
            side);
        Reference2ObjectMap<AEKeyType, MEStorage> externalStorages = new Reference2ObjectOpenHashMap<>(strategies.size());
        for (Entry<AEKeyType, ExternalStorageStrategy> entry : strategies.entrySet()) {
            MEStorage wrapper = entry.getValue().createWrapper(false, () -> {
            });
            if (wrapper != null) {
                externalStorages.put(entry.getKey(), wrapper);
            }
        }

        if (!externalStorages.isEmpty()) {
            return wrapMeStorage(new CompositeStorage(externalStorages), src);
        }

        return null;
    }

    static PatternProviderTarget wrapMeStorage(MEStorage storage, IActionSource src) {
        return new PatternProviderTarget() {
            @Override
            public long insert(AEKey what, long amount, Actionable type) {
                return insert(what, amount, type, PatternProviderInsertionMode.DEFAULT);
            }

            @Override
            public long insert(AEKey what, long amount, Actionable type, PatternProviderInsertionMode insertionMode) {
                return storage.insert(what, amount, type, src);
            }

            @Override
            public boolean containsPatternInput(Set<AEKey> patternInputs) {
                for (Object2LongMap.Entry<AEKey> stack : storage.getAvailableStacks()) {
                    if (patternInputs.contains(stack.getKey().dropSecondary())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean containsAnyStack() {
                return !storage.getAvailableStacks().isEmpty();
            }
        };
    }

    long insert(AEKey what, long amount, Actionable type);

    long insert(AEKey what, long amount, Actionable type, PatternProviderInsertionMode insertionMode);

    boolean containsPatternInput(Set<AEKey> patternInputs);

    boolean containsAnyStack();
}
