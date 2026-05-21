package appeng.client.gui.me.search;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.container.me.common.GridInventoryEntry;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class TagSearchPredicate implements Predicate<GridInventoryEntry> {
    private final String term;
    private final Map<AEKeyType, List<String>> tagCache = new Reference2ObjectOpenHashMap<>();

    public TagSearchPredicate(String term) {
        this.term = term.toLowerCase(Locale.ROOT);
    }

    private List<String> getTagsMatchingTerm(AEKeyType keyType) {
        return keyType.getTagNames()
                      .filter(this::matchesTerm)
                      .collect(Collectors.toList());
    }

    private boolean matchesTerm(String tag) {
        String tagId = tag.toLowerCase(Locale.ROOT);
        if (term.contains(":")) {
            return tagId.contains(term);
        }

        int separator = tagId.indexOf(':');
        if (separator >= 0) {
            return tagId.substring(0, separator).contains(term) || tagId.substring(separator + 1).contains(term);
        }

        return tagId.contains(term);
    }

    @Override
    public boolean test(GridInventoryEntry entry) {
        AEKey what = Objects.requireNonNull(entry.what());
        List<String> tags = tagCache.computeIfAbsent(what.getType(), this::getTagsMatchingTerm);

        for (String tag : tags) {
            if (what.isTagged(tag)) {
                return true;
            }
        }

        return false;
    }
}
