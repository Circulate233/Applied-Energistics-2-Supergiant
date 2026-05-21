package appeng.api.storage;

import appeng.api.networking.IGridNode;
import appeng.api.util.IConfigurableObject;
import org.jetbrains.annotations.Nullable;

public interface IPatternAccessTermContainerHost extends IConfigurableObject {
    @Nullable
    IGridNode getGridNode();

    ILinkStatus getLinkStatus();
}
