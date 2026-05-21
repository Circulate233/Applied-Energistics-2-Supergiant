package appeng.api.behaviors;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.stacks.AEKeyType;
import appeng.parts.automation.StackWorldBehaviors;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Pickup strategies are used to pick up various types of game objects from within the world and convert them into a
 * subtype of {@link appeng.api.stacks.AEKey}.
 * <p/>
 * This is used by the annihilation plane to pick up in-world fluids, blocks or item entities.
 */
@ApiStatus.Experimental
public interface PickupStrategy {
    static void register(AEKeyType type, Factory factory) {
        StackWorldBehaviors.registerPickupStrategy(type, factory);
    }

    /**
     * Resets any lock-out caused by throttling of the pickup strategy. It is called at least once per tick by the
     * annihilation plane to allow the strategy to reset its lockout timer.
     */
    void reset();

    /**
     * Tests if this strategy can pick up the given entity.
     */
    boolean canPickUpEntity(Entity entity);

    /**
     * Pick up a given entity and place the result into the given pickup sink. Returns true if the entity was picked up
     * successfully. The strategy has to handle removal or modification of the entity itself.
     */
    boolean pickUpEntity(IEnergySource energySource, PickupSink sink, Entity entity);

    Result tryPickup(IEnergySource energySource, PickupSink sink);

    enum Result {
        /**
         * There is nothing this strategy can pick up.
         */
        CANT_PICKUP,
        /**
         * There is something the strategy could pick up, but the storage is full.
         */
        CANT_STORE,
        /**
         * The strategy picked something up successfully.
         */
        PICKED_UP
    }

    @FunctionalInterface
    interface Factory {
        PickupStrategy create(WorldServer level, BlockPos fromPos, EnumFacing fromSide, TileEntity host,
                              Object2IntMap<Enchantment> enchantments, @Nullable UUID owningPlayerId);
    }
}
