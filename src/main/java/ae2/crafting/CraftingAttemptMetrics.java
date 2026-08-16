package ae2.crafting;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight counters retained with a crafting plan independently of performance logging.
 */
public final class CraftingAttemptMetrics {
    private final AtomicLong wholeFallbacks = new AtomicLong();
    private final AtomicLong localPatternPlans = new AtomicLong();
    private final AtomicLong localComponentPlans = new AtomicLong();
    private final AtomicLong localReplans = new AtomicLong();
    private final AtomicLong nativeSccs = new AtomicLong();
    private final AtomicLong localSccs = new AtomicLong();
    private final AtomicLong graphCalculationNanos = new AtomicLong();
    private final AtomicLong displayMaterializationNanos = new AtomicLong();

    void recordLocalPatternPlan() {
        localPatternPlans.incrementAndGet();
    }

    void recordLocalComponentPlan() {
        localComponentPlans.incrementAndGet();
    }

    void recordLocalReplan() {
        localReplans.incrementAndGet();
    }

    void recordNativeScc() {
        nativeSccs.incrementAndGet();
    }

    void recordLocalScc() {
        localSccs.incrementAndGet();
    }

    void recordGraphCalculation(long nanos) {
        graphCalculationNanos.set(nanos);
    }

    void recordDisplayMaterialization(long nanos) {
        displayMaterializationNanos.compareAndSet(0, nanos);
    }

    public Snapshot snapshot() {
        return new Snapshot(
            wholeFallbacks.get(),
            localPatternPlans.get(),
            localComponentPlans.get(),
            localReplans.get(),
            nativeSccs.get(),
            localSccs.get(),
            graphCalculationNanos.get(),
            displayMaterializationNanos.get());
    }

    public record Snapshot(long wholeFallbacks,
                           long localPatternPlans,
                           long localComponentPlans,
                           long localReplans,
                           long nativeSccs,
                           long localSccs,
                           long graphCalculationNanos,
                           long displayMaterializationNanos) {
    }
}
