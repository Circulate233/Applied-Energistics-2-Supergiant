/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ae2.api.storage;

import ae2.api.networking.storage.IStorageService;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.util.IConfigurableObject;
import ae2.client.Hotkeys;
import org.jetbrains.annotations.Nullable;

public interface ITerminalHost extends IUpgradeableObject, IConfigurableObject, ISubGuiHost {
    /**
     * Please note that this will only be called <strong>once</strong> when the container is opened. If the inventory of this
     * terminal host can change during its lifecycle, you need to return a {@link SupplierStorage}.
     */
    MEStorage getInventory();

    /**
     * Returns the grid-wide storage service displayed by this terminal, if this host represents a complete network
     * inventory.
     * <p>
     * Containers use this identity to subscribe directly to the network inventory monitor and avoid enumerating the
     * same network once per open terminal. Hosts backed by a local inventory, such as portable cells and ME chests,
     * must keep the default value. Hosts whose grid connection can change must return {@code null} while disconnected
     * and the current service after reconnecting.
     *
     * @return the currently displayed grid storage service, or {@code null} for local or disconnected inventories
     */
    @Nullable
    default IStorageService getGridStorageService() {
        return null;
    }

    /**
     * For hosts that do not have a fixed connection to the grid, this method is used to give feedback to the player
     * about the current connection status.
     */
    ILinkStatus getLinkStatus();

    /**
     * An optional hotkey used to close the terminal while its open.
     *
     * @return Hotkey id as it would be registered by {@link Hotkeys}, or null if there isn't one
     */
    @Nullable
    default String getCloseHotkey() {
        return null;
    }
}
