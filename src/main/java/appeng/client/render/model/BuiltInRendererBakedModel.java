package appeng.client.render.model;

import appeng.client.render.DelegateBakedModel;
import net.minecraft.client.renderer.block.model.IBakedModel;

public class BuiltInRendererBakedModel extends DelegateBakedModel {
    public BuiltInRendererBakedModel(IBakedModel base) {
        super(base);
    }

    @Override
    public boolean isBuiltInRenderer() {
        return true;
    }
}
