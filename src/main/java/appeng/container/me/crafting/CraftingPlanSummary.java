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

package appeng.container.me.crafting;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.PacketBuffer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record CraftingPlanSummary(long usedBytes, boolean simulation, List<CraftingPlanSummaryEntry> entries) {

    public static CraftingPlanSummary read(PacketBuffer buffer) {
        long bytesUsed = buffer.readVarLong();
        boolean simulation = buffer.readBoolean();
        int entryCount = buffer.readVarInt();
        ImmutableList.Builder<CraftingPlanSummaryEntry> entries = ImmutableList.builder();
        for (int i = 0; i < entryCount; i++) {
            entries.add(CraftingPlanSummaryEntry.read(buffer));
        }
        return new CraftingPlanSummary(bytesUsed, simulation, entries.build());
    }

    public static CraftingPlanSummary fromJob(IGrid grid, IActionSource actionSource, ICraftingPlan job) {
        Object2ObjectMap<AEKey, KeyStats> plan = new Object2ObjectOpenHashMap<>();

        for (var used : job.usedItems()) {
            mapping(plan, used.getKey()).stored += used.getLongValue();
        }
        for (var missing : job.missingItems()) {
            mapping(plan, missing.getKey()).stored += missing.getLongValue();
        }
        for (var emitted : job.emittedItems()) {
            var entry = mapping(plan, emitted.getKey());
            entry.stored += emitted.getLongValue();
            entry.crafting += emitted.getLongValue();
        }
        for (Object2LongMap.Entry<appeng.api.crafting.IPatternDetails> entry : job.patternTimes().object2LongEntrySet()) {
            for (var out : entry.getKey().getOutputs()) {
                mapping(plan, out.what()).crafting += out.amount() * entry.getLongValue();
            }
        }

        List<CraftingPlanSummaryEntry> entries = new ObjectArrayList<>();
        var storage = grid.getStorageService().getInventory();
        var crafting = grid.getCraftingService();

        for (var out : plan.entrySet()) {
            long missingAmount;
            long storedAmount;
            if (job.simulation() && !crafting.canEmitFor(out.getKey())) {
                storedAmount = storage.extract(out.getKey(), out.getValue().stored, Actionable.SIMULATE, actionSource);
                missingAmount = out.getValue().stored - storedAmount;
            } else {
                storedAmount = out.getValue().stored;
                missingAmount = 0;
            }
            long craftAmount = out.getValue().crafting;
            entries.add(new CraftingPlanSummaryEntry(out.getKey(), missingAmount, storedAmount, craftAmount));
        }

        Collections.sort(entries);
        return new CraftingPlanSummary(job.bytes(), job.simulation(), List.copyOf(entries));
    }

    private static KeyStats mapping(Object2ObjectMap<AEKey, KeyStats> plan, AEKey key) {
        Objects.requireNonNull(key, "Key may not be null");
        return plan.computeIfAbsent(key, ignored -> new KeyStats());
    }

    public void write(PacketBuffer buffer) {
        buffer.writeVarLong(this.usedBytes);
        buffer.writeBoolean(this.simulation);
        buffer.writeVarInt(this.entries.size());
        for (CraftingPlanSummaryEntry entry : this.entries) {
            entry.write(buffer);
        }
    }

    private static class KeyStats {
        private long stored;
        private long crafting;
    }
}
