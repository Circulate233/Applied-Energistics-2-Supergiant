package ae2.api.storage.cells;

/**
 * Provides optional capacity statistics for a {@link StorageCell}.
 * <p>
 * Storage cells implement this interface when they can report exact byte and type usage. Consumers must treat this
 * information as a point-in-time view and must not assume that every registered storage cell supports it.
 */
public interface StorageCellStatistics {

    /**
     * Returns the number of bytes currently occupied by stored resources and their type overhead.
     *
     * @return the used byte count, never negative
     */
    long getUsedBytes();

    /**
     * Returns the total number of bytes available to this cell.
     *
     * @return the total byte count, never negative
     */
    long getTotalBytes();

    /**
     * Returns the number of distinct resource types currently stored in this cell.
     *
     * @return the stored type count, never negative
     */
    long getStoredTypes();

    /**
     * Returns the maximum number of distinct resource types supported by this cell.
     *
     * @return the total type capacity, never negative
     */
    long getTotalTypes();

    /**
     * Returns the fixed byte cost applied to each stored resource type. Cells without type overhead can keep the
     * default value.
     *
     * @return the byte cost per stored type, never negative
     */
    default int getBytesPerType() {
        return 0;
    }
}
