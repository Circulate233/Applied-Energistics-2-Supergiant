/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.client.gui.me.common;

import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.integration.Integrations;
import appeng.integration.abstraction.IInvTweaks;
import appeng.integration.modules.bogosorter.InventoryBogoSortModule;

import java.util.Comparator;

final class KeySorters {

    public static final Comparator<AEKey> NAME_ASC = Comparator.comparing(
        is -> is.getDisplayName().getUnformattedText(),
        String::compareToIgnoreCase);
    public static final Comparator<AEKey> NAME_DESC = NAME_ASC.reversed();
    public static final Comparator<AEKey> MOD_ASC = Comparator.comparing(
        AEKey::getModId,
        String::compareToIgnoreCase).thenComparing(NAME_ASC);
    public static final Comparator<AEKey> MOD_DESC = MOD_ASC.reversed();
    public static final Comparator<AEKey> INVTWEAKS_ASC = KeySorters::compareInvTweaks;
    public static final Comparator<AEKey> INVTWEAKS_DESC = INVTWEAKS_ASC.reversed();

    private KeySorters() {
    }

    public static Comparator<AEKey> getComparator(SortOrder order, SortDir dir) {
        return switch (order) {
            case NAME -> dir == SortDir.ASCENDING ? NAME_ASC : NAME_DESC;
            case MOD -> dir == SortDir.ASCENDING ? MOD_ASC : MOD_DESC;
            case INVTWEAKS -> dir == SortDir.ASCENDING ? INVTWEAKS_ASC : INVTWEAKS_DESC;
            case AMOUNT -> throw new UnsupportedOperationException();
        };
    }

    private static int compareInvTweaks(AEKey left, AEKey right) {
        if (!(left instanceof AEItemKey leftItem) || !(right instanceof AEItemKey rightItem)) {
            return compareByFallback(left, right);
        }

        var bogoComparator = InventoryBogoSortModule.getComparator();
        if (bogoComparator != null) {
            return bogoComparator.compare(leftItem.getReadOnlyStack(), rightItem.getReadOnlyStack());
        }

        IInvTweaks invTweaks = Integrations.invTweaks();
        if (invTweaks.isEnabled()) {
            return invTweaks.compareItems(leftItem.getReadOnlyStack(), rightItem.getReadOnlyStack());
        }

        return compareByFallback(left, right);
    }

    private static int compareByFallback(AEKey left, AEKey right) {
        int nameCompare = NAME_ASC.compare(left, right);
        if (nameCompare != 0) {
            return nameCompare;
        }
        return MOD_ASC.compare(left, right);
    }

}

