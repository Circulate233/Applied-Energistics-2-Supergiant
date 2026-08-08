/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2026, TeamAppliedEnergistics, All rights reserved.
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

package ae2.debug;

import ae2.api.crafting.IPatternDetails;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.orientation.BlockOrientation;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.core.AELog;
import ae2.core.definitions.AEBlocks;
import ae2.tile.grid.AENetworkedTile;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class TileCraftingTreeTest extends AENetworkedTile implements ICraftingProvider {

    private static final int PATTERN_COUNT = 3000;
    private static final int LOGICAL_NODE_COUNT = 15000;
    private static final int MAX_DEPTH = 95;
    private static final int ROOT_INPUT_COUNT = LOGICAL_NODE_COUNT - PATTERN_COUNT;
    private static final String SEED_TAG = "treeSeed";
    private static final String GENERATED_TAG = "treeGenerated";
    private static final long DEFAULT_SEED = 0x6A09E667F3BCC909L;

    private final ObjectList<IPatternDetails> patterns = new ObjectArrayList<>(PATTERN_COUNT);
    private long seed = DEFAULT_SEED;
    private boolean seedLoaded;
    private boolean generated;

    public TileCraftingTreeTest() {
        this.getMainNode().addService(ICraftingProvider.class, this);
    }

    @Override
    public ItemStack getItemFromTile() {
        return AEBlocks.CRAFTING_TREE_TEST.stack();
    }

    @Override
    public EnumSet<EnumFacing> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.allOf(EnumFacing.class);
    }

    @Override
    public void onReady() {
        super.onReady();
        if (this.world == null || this.world.isRemote) {
            return;
        }

        if (!this.generated || this.patterns.isEmpty()) {
            if (!this.seedLoaded) {
                this.seed = new Random().nextLong();
                this.seedLoaded = true;
            }
            this.generateTree();
        }
        ICraftingProvider.requestUpdate(this.getMainNode());
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        if (data.hasKey(SEED_TAG, Constants.NBT.TAG_LONG)) {
            this.seed = data.getLong(SEED_TAG);
            this.seedLoaded = true;
        }
        this.generated = data.getBoolean(GENERATED_TAG);
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        data.setLong(SEED_TAG, this.seed);
        data.setBoolean(GENERATED_TAG, this.generated);
    }

    @Override
    public List<? extends IPatternDetails> getAvailablePatterns() {
        return this.patterns;
    }

    public static boolean isCraftingTreeTestPattern(IPatternDetails patternDetails) {
        if (patternDetails == null || !(patternDetails.getDefinition() instanceof AEItemKey definition)) {
            return false;
        }

        if (!definition.is(Item.getItemFromBlock(Blocks.STONE))) {
            return false;
        }

        var tag = definition.getTagCompound();
        return tag != null
            && tag.getKeySet().size() == 2
            && tag.hasKey("node", Constants.NBT.TAG_INT)
            && tag.hasKey("output", Constants.NBT.TAG_BYTE)
            && tag.getInteger("node") >= 0
            && tag.getInteger("node") < PATTERN_COUNT
            && tag.getByte("output") == 0;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, int multiplier) {
        AELog.debug("Crafting tree test rejected pattern push: %s", patternDetails.getDefinition());
        return false;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    private static ItemStack markerStack(int node, boolean output) {
        ItemStack stack = new ItemStack(Blocks.STONE);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("node", node);
        tag.setBoolean("output", output);
        stack.setTagCompound(tag);
        return stack;
    }

    private static ItemStack endStack() {
        ItemStack stack = new ItemStack(Blocks.END_STONE);
        stack.setStackDisplayName("END");
        return stack;
    }

    private void generateTree() {
        this.patterns.clear();
        SplitMix64 random = new SplitMix64(this.seed);
        AEItemKey[] outputs = new AEItemKey[PATTERN_COUNT];

        for (int node = 0; node < PATTERN_COUNT; node++) {
            boolean root = node == PATTERN_COUNT - 1;
            AEItemKey definition = Objects.requireNonNull(AEItemKey.of(markerStack(node, false)));
            ObjectList<GenericStack> inputs = new ObjectArrayList<>(root ? ROOT_INPUT_COUNT : 1);
            if (root) {
                for (int reference = 0; reference < ROOT_INPUT_COUNT; reference++) {
                    inputs.add(new GenericStack(outputs[reference % (PATTERN_COUNT - 1)], 1));
                }
            } else if (node == 0) {
                inputs.add(new GenericStack(
                    Objects.requireNonNull(AEItemKey.of(new ItemStack(Blocks.COBBLESTONE))), 1));
            } else {
                int predecessor;
                if (node <= MAX_DEPTH - 2) {
                    predecessor = node - 1;
                } else {
                    int bound = MAX_DEPTH - 2;
                    predecessor = Math.floorMod(random.nextLong(), bound);
                }
                inputs.add(new GenericStack(outputs[predecessor], 1));
            }

            AEItemKey outputKey = Objects.requireNonNull(
                AEItemKey.of(root ? endStack() : markerStack(node, true)));
            GenericStack output = new GenericStack(outputKey, 1);
            this.patterns.add(new TestPattern(definition, inputs, output));
            outputs[node] = outputKey;
        }

        this.generated = true;
        this.saveChanges();
        AELog.info("Generated crafting tree test seed=%d definitions=%d logicalNodes=%d maxDepth=%d rootInputs=%d",
            this.seed, PATTERN_COUNT, LOGICAL_NODE_COUNT, MAX_DEPTH, ROOT_INPUT_COUNT);
    }

    private static final class SplitMix64 {
        private long state;

        private SplitMix64(long seed) {
            this.state = seed;
        }

        private long nextLong() {
            long value = (this.state += 0x9E3779B97F4A7C15L);
            value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
            value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
            return value ^ value >>> 31;
        }
    }

    private record TestInput(GenericStack[] possibleInputs) implements IPatternDetails.IInput {
            private TestInput(GenericStack input) {
                this(new GenericStack[]{input});
            }

            @Override
            public long getMultiplier() {
                return 1;
            }

            @Override
            public boolean isValid(AEKey input, World level) {
                return input.equals(this.possibleInputs[0].what());
            }

            @Nullable
            @Override
            public AEKey getRemainingKey(AEKey template) {
                return null;
            }
        }

    @SuppressWarnings("ClassCanBeRecord")
    private static final class TestPattern implements IPatternDetails {
        private final AEItemKey definition;
        private final IInput[] inputs;
        private final List<GenericStack> outputs;

        private TestPattern(AEItemKey definition, ObjectList<GenericStack> inputStacks, GenericStack output) {
            this.definition = definition;
            this.inputs = new IInput[inputStacks.size()];
            for (int i = 0; i < inputStacks.size(); i++) {
                this.inputs[i] = new TestInput(inputStacks.get(i));
            }
            this.outputs = List.of(output);
        }

        @Override
        public AEItemKey getDefinition() {
            return this.definition;
        }

        @Override
        public IInput[] getInputs() {
            return this.inputs;
        }

        @Override
        public List<GenericStack> getOutputs() {
            return this.outputs;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TestPattern other && this.definition.equals(other.definition);
        }

        @Override
        public int hashCode() {
            return this.definition.hashCode();
        }
    }
}
