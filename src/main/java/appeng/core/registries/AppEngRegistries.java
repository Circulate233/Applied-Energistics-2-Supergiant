package appeng.core.registries;

import appeng.api.stacks.AEKeyTypesInternal;

/**
 *
 */
public final class AppEngRegistries {

    private static boolean initialized;

    private AppEngRegistries() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        AEKeyTypesInternal.getAllTypes();
    }
}
