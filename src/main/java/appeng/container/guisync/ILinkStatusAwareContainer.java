package appeng.container.guisync;

import appeng.api.storage.ILinkStatus;

public interface ILinkStatusAwareContainer {
    void setLinkStatus(ILinkStatus linkStatus);
}

