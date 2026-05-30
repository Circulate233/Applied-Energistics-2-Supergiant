package appeng.parts.automation.special;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.storagebus.StorageBusPart;
import appeng.util.prioritylist.IPartitionList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class ODStorageBusPart extends StorageBusPart implements ODFilterHost {
    private static final ResourceLocation MODEL_BASE = AppEng.makeId("part/od_storage_bus_base");

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, StorageBusPartModels.OFF);

    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, StorageBusPartModels.ON);

    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, StorageBusPartModels.HAS_CHANNEL);

    private String whiteExpression = "";
    private String blackExpression = "";

    public ODStorageBusPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.whiteExpression = data.getString("odWhite");
        this.blackExpression = data.getString("odBlack");
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setString("odWhite", this.whiteExpression);
        data.setString("odBlack", this.blackExpression);
    }

    @Override
    protected IPartitionList createFilter() {
        return new ODPriorityList(this.whiteExpression, this.blackExpression);
    }

    @Override
    public String getODFilter(boolean whitelist) {
        return whitelist ? this.whiteExpression : this.blackExpression;
    }

    @Override
    public void setODFilter(String expression, boolean whitelist) {
        expression = expression == null ? "" : expression;
        if (whitelist) {
            if (!expression.equals(this.whiteExpression)) {
                this.whiteExpression = expression;
                onConfigurationChanged();
                getHost().markForSave();
            }
        } else if (!expression.equals(this.blackExpression)) {
            this.blackExpression = expression;
            onConfigurationChanged();
            getHost().markForSave();
        }
    }

    @Override
    public appeng.container.GuiIds.GuiKey getGuiKey() {
        return appeng.container.GuiIds.GuiKey.OD_STORAGE_BUS;
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
