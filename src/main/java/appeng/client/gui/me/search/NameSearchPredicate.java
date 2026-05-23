package appeng.client.gui.me.search;

import appeng.api.stacks.AEKey;
import appeng.container.me.common.GridInventoryEntry;

import java.util.Objects;
import java.util.function.Predicate;

final class NameSearchPredicate implements Predicate<GridInventoryEntry> {
    private final String term;

    public NameSearchPredicate(String term) {
        this.term = term.toLowerCase();
    }

    @Override
    public boolean test(GridInventoryEntry gridInventoryEntry) {
        AEKey entryInfo = Objects.requireNonNull(gridInventoryEntry.what());
        String displayName = entryInfo.getDisplayName().getFormattedText();
        return displayName.toLowerCase().contains(term);
    }
}

