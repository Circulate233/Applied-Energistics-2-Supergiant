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
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class PatternProviderTargetCache {
    private final WorldServer level;
    private final BlockPos pos;
    private final EnumFacing side;
    private final IActionSource src;
    private final Map<AEKeyType, ExternalStorageStrategy> strategies;

    PatternProviderTargetCache(WorldServer level, BlockPos pos, EnumFacing side, IActionSource src) {
        this.level = level;
        this.pos = pos;
        this.side = side;
        this.src = src;
        this.strategies = StackWorldBehaviors.createExternalStorageStrategies(level, pos, side);
    }

    @Nullable
    PatternProviderTarget find() {
        TileEntity blockEntity = this.level.getTileEntity(this.pos);
        MEStorage storage = null;
        if (blockEntity != null && blockEntity.hasCapability(AECapabilities.ME_STORAGE, this.side)) {
            storage = blockEntity.getCapability(AECapabilities.ME_STORAGE, this.side);
        }

        if (storage != null) {
            return wrapMeStorage(storage);
        }

        Reference2ObjectMap<AEKeyType, MEStorage> externalStorages = new Reference2ObjectOpenHashMap<>(this.strategies.size());
        for (Entry<AEKeyType, ExternalStorageStrategy> entry : this.strategies.entrySet()) {
            MEStorage wrapper = entry.getValue().createWrapper(false, () -> {
            });
            if (wrapper != null) {
                externalStorages.put(entry.getKey(), wrapper);
            }
        }

        if (!externalStorages.isEmpty()) {
            return wrapMeStorage(new CompositeStorage(externalStorages));
        }

        return null;
    }

    private PatternProviderTarget wrapMeStorage(MEStorage storage) {
        return new PatternProviderTarget() {
            @Override
            public long insert(AEKey what, long amount, Actionable type) {
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
}
