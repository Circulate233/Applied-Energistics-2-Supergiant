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

package ae2.client.gui.me.common;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

final class ExternalSortFallback {

    private ExternalSortFallback() {
    }

    static <T> Comparator<T> comparator(Comparator<T> external, Comparator<T> fallback) {
        return new FallbackComparator<>(external, fallback);
    }

    static <T> Throwable sort(List<T> values, Comparator<? super T> comparator,
                              Comparator<? super T> fallback) {
        try {
            values.sort(comparator);
            return null;
        } catch (RuntimeException | LinkageError error) {
            values.sort(fallback);
            return error;
        }
    }

    private static final class FallbackComparator<T> implements Comparator<T> {
        private final Comparator<T> external;
        private final Comparator<T> fallback;
        private volatile boolean fallbackOnly;

        private FallbackComparator(Comparator<T> external, Comparator<T> fallback) {
            this.external = Objects.requireNonNull(external);
            this.fallback = Objects.requireNonNull(fallback);
        }

        @Override
        public int compare(T left, T right) {
            if (fallbackOnly) {
                return fallback.compare(left, right);
            }

            try {
                int result = external.compare(left, right);
                return result != 0 ? result : fallback.compare(left, right);
            } catch (RuntimeException | LinkageError error) {
                fallbackOnly = true;
                return fallback.compare(left, right);
            }
        }
    }
}
