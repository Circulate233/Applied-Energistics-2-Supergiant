package ae2.core.mixins;

import io.netty.util.internal.EmptyArrays;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@IFMLLoadingPlugin.Name("AE2Core")
@IFMLLoadingPlugin.SortingIndex(-1)
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class AE2Core implements IFMLLoadingPlugin {

    @Nullable
    public String[] getASMTransformerClass() {
        return EmptyArrays.EMPTY_STRINGS;
    }

    @Nullable
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    public String getSetupClass() {
        return null;
    }

    public void injectData(Map<String, Object> data) {
    }

    @Nullable
    public String getAccessTransformerClass() {
        return null;
    }

}
