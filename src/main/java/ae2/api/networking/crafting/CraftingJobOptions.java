package ae2.api.networking.crafting;

/**
 * Player-facing options attached to a standalone crafting job.
 */
public record CraftingJobOptions(int priority, boolean subscribed) {
    public static final CraftingJobOptions DEFAULT = new CraftingJobOptions(0, false);
}
