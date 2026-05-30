package appeng.parts.automation.special;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.container.GuiIds.GuiKey;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.storagebus.StorageBusPart;
import appeng.util.prioritylist.IPartitionList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class ModStorageBusPart extends StorageBusPart implements ModFilterHost {
    private static final ResourceLocation MODEL_BASE = AppEng.makeId("part/mod_storage_bus_base");

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, StorageBusPartModels.OFF);

    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, StorageBusPartModels.ON);

    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, StorageBusPartModels.HAS_CHANNEL);

    private String modExpression = "";

    public ModStorageBusPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.modExpression = data.getString("modid");
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setString("modid", this.modExpression);
    }

    @Override
    protected IPartitionList createFilter() {
        return new ModPriorityList(this.modExpression);
    }

    @Override
    public String getModFilter() {
        return this.modExpression;
    }

    @Override
    public void setModFilter(String expression) {
        expression = expression == null ? "" : expression;
        if (!expression.equals(this.modExpression)) {
            this.modExpression = expression;
            onConfigurationChanged();
            getHost().markForSave();
        }
    }

    @Override
    public GuiKey getGuiKey() {
        return GuiKey.MOD_STORAGE_BUS;
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return MODELS_ON;
        }
        return MODELS_OFF;
    }
}
