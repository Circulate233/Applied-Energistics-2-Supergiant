package appeng.parts.automation.special;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.me.storage.NullInventory;
import appeng.parts.PartModel;
import appeng.parts.storagebus.StorageBusPart;
import appeng.util.ConfigInventory;
import appeng.util.prioritylist.IPartitionList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class PreciseStorageBusPart extends StorageBusPart {
    private static final ResourceLocation MODEL_BASE = AppEng.makeId("part/precise_storage_bus_base");

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, StorageBusPartModels.OFF);

    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, StorageBusPartModels.ON);

    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, StorageBusPartModels.HAS_CHANNEL);

    private final ConfigInventory stackConfig = ConfigInventory.configStacks(63)
                                                               .changeListener(this::onConfigurationChanged)
                                                               .allowOverstacking(true)
                                                               .build();

    public PreciseStorageBusPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public ConfigInventory getConfig() {
        return this.stackConfig;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.stackConfig.readFromChildTag(data, "config");
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        this.stackConfig.writeToChildTag(data, "config");
    }

    @Override
    protected IPartitionList createFilter() {
        KeyCounter filter = new KeyCounter();
        for (var entry : this.stackConfig.getAvailableStacks()) {
            filter.add(entry.getKey(), entry.getLongValue());
        }
        return new PreciseAmountFilter(filter);
    }

    @Override
    protected StorageBusInventory createHandler() {
        return new PreciseStorageBusInventory(NullInventory.of());
    }

    @Override
    public appeng.container.GuiIds.GuiKey getGuiKey() {
        return appeng.container.GuiIds.GuiKey.PRECISE_STORAGE_BUS;
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

    public static class PreciseStorageBusInventory extends StorageBusInventory {
        public PreciseStorageBusInventory(MEStorage inventory) {
            super(inventory);
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            var filter = getPartitionList();
            long targetAmount = filter instanceof PreciseAmountFilter precise ? precise.getAmount(what)
                : filter.isEmpty() ? 1 : 0;
            if (targetAmount <= 0) {
                return 0;
            }
            long missing = targetAmount - this.getAvailableStacks().get(what);
            if (missing <= 0) {
                return 0;
            }
            return super.insert(what, Math.min(amount, missing), mode, source);
        }
    }

    private record PreciseAmountFilter(KeyCounter filter) implements IPartitionList {
        @Override
        public boolean isListed(AEKey input) {
            return this.filter.get(input) > 0;
        }

        @Override
        public boolean isEmpty() {
            return this.filter.isEmpty();
        }

        @Override
        public Iterable<AEKey> getItems() {
            return this.filter.keySet();
        }

        long getAmount(AEKey input) {
            return this.filter.get(input);
        }
    }
}
