package appeng.me.helpers;

import appeng.api.networking.IGridServiceProvider;

import java.util.Map;

public record GridServiceContainer(
    Map<Class<?>, IGridServiceProvider> services,
    IGridServiceProvider[] serverStartTickServices,
    IGridServiceProvider[] levelStartTickServices,
    IGridServiceProvider[] levelEndTickServices,
    IGridServiceProvider[] serverEndTickServices) {
}
