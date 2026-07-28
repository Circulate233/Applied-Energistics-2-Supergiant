package ae2.api.storage;

/**
 * Provides {@link IStorageProvider} with a convenient way to control the storage they provide to the network.
 */
public interface IStorageMounts {
    /** Default priority used when a provider does not request explicit storage ordering. */
    int DEFAULT_PRIORITY = 0;

    /**
     * Mounts event-driven storage with {@link #DEFAULT_PRIORITY}.
     *
     * @param inventory storage whose later visible changes are reported through monitor callbacks
     */
    default void mount(MEStorageMonitor inventory) {
        mount(inventory, DEFAULT_PRIORITY);
    }

    /**
     * Mounts event-driven storage into the grid. The storage is enumerated once when the grid cache is initialized and
     * must report every later visible content change through its monitor listeners.
     *
     * @param inventory storage whose later visible changes are reported through monitor callbacks
     * @param priority  routing priority of the mounted storage
     */
    void mount(MEStorageMonitor inventory, int priority);
}
