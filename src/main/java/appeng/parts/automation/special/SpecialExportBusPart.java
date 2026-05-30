package appeng.parts.automation.special;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEKey;
import appeng.parts.automation.ExportBusPart;
import appeng.util.prioritylist.IPartitionList;
import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.NBTTagCompound;

abstract class SpecialExportBusPart extends ExportBusPart {

    private IPartitionList filter;

    SpecialExportBusPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public void readFromNBT(NBTTagCompound extra) {
        super.readFromNBT(extra);
        this.filter = null;
    }

    protected void invalidateSpecialFilter() {
        this.filter = null;
        this.getHost().markForSave();
        this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    protected IPartitionList getSpecialFilter() {
        if (this.filter == null) {
            this.filter = createSpecialFilter();
        }
        return this.filter;
    }

    protected abstract IPartitionList createSpecialFilter();

    @Override
    protected boolean doBusWork(IGrid grid) {
        var storageService = grid.getStorageService();
        var context = createTransferContext(storageService, grid.getEnergyService());
        var filter = getSpecialFilter();

        for (var entry : ImmutableList.copyOf(storageService.getCachedInventory())) {
            AEKey what = entry.getKey();
            if (!filter.isListed(what)) {
                continue;
            }
            long amount = (long) context.getOperationsRemaining() * what.getAmountPerOperation();
            amount = getExportStrategy().transfer(context, what, amount);
            if (amount > 0) {
                context.reduceOperationsRemaining(Math.max(1, amount / what.getAmountPerOperation()));
            }
            if (!context.hasOperationsLeft()) {
                break;
            }
        }

        return context.hasDoneWork();
    }

    protected long simulatePush(AEKey what, long amount) {
        return getExportStrategy().push(what, amount, Actionable.SIMULATE);
    }
}
