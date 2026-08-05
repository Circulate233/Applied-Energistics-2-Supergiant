package ae2.client.gui.me.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ExternalSortFallbackTest {

    @Test
    void preservesHealthyExternalOrderingAndStabilizesTies() {
        Comparator<String> comparator = ExternalSortFallback.comparator(
            Comparator.comparingInt(String::length),
            Comparator.naturalOrder());

        assertEquals(1, Integer.signum(comparator.compare("bbb", "a")));
        assertEquals(-1, Integer.signum(comparator.compare("a", "b")));
    }

    @Test
    void switchesPermanentlyToFallbackAfterRuntimeFailure() {
        AtomicInteger calls = new AtomicInteger();
        Comparator<String> comparator = ExternalSortFallback.comparator(
            (String left, String right) -> {
                calls.incrementAndGet();
                throw new IllegalStateException("broken item tree");
            },
            Comparator.naturalOrder());

        assertEquals(-1, Integer.signum(comparator.compare("a", "b")));
        assertEquals(1, Integer.signum(comparator.compare("b", "a")));
        assertEquals(1, calls.get());
    }

    @Test
    void switchesPermanentlyToFallbackAfterLinkageFailure() {
        AtomicInteger calls = new AtomicInteger();
        Comparator<String> comparator = ExternalSortFallback.comparator(
            (String left, String right) -> {
                calls.incrementAndGet();
                throw new NoClassDefFoundError("optional sorter");
            },
            Comparator.naturalOrder());

        assertEquals(-1, Integer.signum(comparator.compare("a", "b")));
        assertEquals(1, Integer.signum(comparator.compare("b", "a")));
        assertEquals(1, calls.get());
    }

    @Test
    void retriesTheWholeSortAfterComparatorFailure() {
        List<String> values = new ArrayList<>(List.of("delta", "alpha", "charlie", "bravo"));
        Comparator<String> brokenComparator = (left, right) -> {
            throw new IllegalArgumentException("Comparison method violates its general contract");
        };

        Throwable failure = ExternalSortFallback.sort(values, brokenComparator, Comparator.naturalOrder());

        assertInstanceOf(IllegalArgumentException.class, failure);
        assertEquals(List.of("alpha", "bravo", "charlie", "delta"), values);
    }

    @Test
    void doesNotUseFallbackForAHealthySort() {
        List<String> values = new ArrayList<>(List.of("b", "a"));

        Throwable failure = ExternalSortFallback.sort(
            values,
            Comparator.naturalOrder(),
            Comparator.reverseOrder());

        assertNull(failure);
        assertEquals(List.of("a", "b"), values);
    }
}
