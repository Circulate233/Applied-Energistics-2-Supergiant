package ae2.client.gui.widgets;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class CraftingCpuSearch {
    private CraftingCpuSearch() {
    }

    public static boolean matches(String query, String cpuName, @Nullable String outputName) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) {
            return true;
        }

        String name = cpuName == null ? "" : cpuName;
        String output = outputName == null ? "" : outputName;
        if (normalizedQuery.startsWith("#")) {
            return matchesTerm(name, normalizedQuery.substring(1));
        }
        if (normalizedQuery.startsWith("$")) {
            return matchesTerm(output, normalizedQuery.substring(1));
        }
        return matchesTerm(name, normalizedQuery) || matchesTerm(output, normalizedQuery);
    }

    private static boolean matchesTerm(String haystack, String term) {
        String normalizedTerm = term.trim();
        return !normalizedTerm.isEmpty() && haystack.contains(normalizedTerm);
    }
}
