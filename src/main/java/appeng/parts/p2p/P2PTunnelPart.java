/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.parts.p2p;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.features.P2PTunnelAttunement;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardColors;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.networking.GridFlags;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEKeyType;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.client.render.cablebus.P2PTunnelFrequencyModelData;
import appeng.core.AEConfig;
import appeng.items.tools.MemoryCardItem;
import appeng.me.service.P2PService;
import appeng.parts.AEBasePart;
import appeng.util.InteractionUtil;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class P2PTunnelPart<T extends P2PTunnelPart<T>> extends AEBasePart {
    private boolean output;
    private short freq;

    public P2PTunnelPart(IPartItem<?> partItem) {
        super(partItem);
        this.getMainNode().setIdlePowerUsage(this.getPowerDrainPerTick());
        this.getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    protected float getPowerDrainPerTick() {
        return 1.0f;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public T getInput() {
        if (this.getFrequency() == 0) {
            return null;
        }

        if (getMainNode().getGrid() != null) {
            P2PTunnelPart<?> tunnel = P2PService.get(getMainNode().getGrid()).getInput(this.getFrequency());
            if (this.getClass().isInstance(tunnel)) {
                return (T) tunnel;
            }
        }
        return null;
    }

    public List<T> getOutputs() {
        return getOutputStream().collect(Collectors.<T>toList());
    }

    public Stream<T> getOutputStream() {
        if (this.getMainNode().isOnline() && getMainNode().getGrid() != null) {
            return P2PService.get(getMainNode().getGrid()).getOutputs(this.getFrequency(), getTunnelClass());
        }
        return Stream.empty();
    }

    @SuppressWarnings("unchecked")
    private Class<T> getTunnelClass() {
        return (Class<T>) this.getClass();
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(5, 5, 12, 11, 11, 13);
        bch.addBox(3, 3, 13, 13, 13, 14);
        bch.addBox(2, 2, 14, 14, 14, 16);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.output = data.getBoolean("output");
        this.freq = data.getShort("freq");
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean("output", this.isOutput());
        data.setShort("freq", this.getFrequency());
    }

    @Override
    public boolean readFromStream(PacketBuffer data) {
        boolean changed = super.readFromStream(data);
        short oldFreq = this.freq;
        this.freq = data.readShort();
        return changed || oldFreq != this.freq;
    }

    @Override
    public void writeToStream(PacketBuffer data) {
        super.writeToStream(data);
        data.writeShort(this.getFrequency());
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1;
    }

    @Override
    public boolean useStandardMemoryCard() {
        return false;
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, EntityPlayer player, EnumHand hand, Vec3d pos) {
        ItemStack newType = P2PTunnelAttunement.getTunnelPartByTriggerItem(heldItem);
        if (!newType.isEmpty() && newType.getItem() != getPartItem().asItem() && newType.getItem() instanceof IPartItem<?> partItem) {
            boolean oldOutput = isOutput();
            short myFreq = getFrequency();

            IPart tunnel = getHost().replacePart(partItem, getSide(), player, hand);
            if (!isClientSide() && tunnel instanceof P2PTunnelPart<?> newTunnel) {
                newTunnel.setOutput(oldOutput);
                newTunnel.onTunnelNetworkChange();
                newTunnel.getMainNode().ifPresent(grid -> P2PService.get(grid).updateFreq(newTunnel, myFreq));
            }

            Platform.notifyBlocksOfNeighbors(getLevel(), getTileEntity().getPos());
            return true;
        }

        if (isClientSide() || hand == EnumHand.OFF_HAND) {
            return false;
        }

        if (heldItem.getItem() instanceof IMemoryCard memoryCard) {
            if (InteractionUtil.isInAlternateUseMode(player)) {
                short newFreq = this.getFrequency();
                boolean wasOutput = this.isOutput();
                this.setOutput(false);

                boolean needsNewFrequency = wasOutput || newFreq == 0;
                if (getMainNode().getGrid() != null) {
                    P2PService p2p = P2PService.get(getMainNode().getGrid());
                    if (needsNewFrequency) {
                        newFreq = p2p.newFrequency();
                    }
                    p2p.updateFreq(this, newFreq);
                } else if (needsNewFrequency) {
                    newFreq = newFrequency();
                    setFrequency(newFreq);
                    onTunnelNetworkChange();
                }

                this.onTunnelConfigChange();

                MemoryCardItem.clearCard(heldItem);
                NBTTagCompound cardTag = Platform.openNbtData(heldItem);
                AEComponents.EXPORTED_SETTINGS_SOURCE_COMPONENT.writeTo(cardTag,
                    new NBTTagString(new ItemStack(getPartItem().asItem()).getDisplayName()));
                cardTag.merge(exportSettings(SettingsFrom.MEMORY_CARD));

                memoryCard.notifyUser(player,
                    needsNewFrequency ? MemoryCardMessages.SETTINGS_RESET : MemoryCardMessages.SETTINGS_SAVED);
                return true;
            }

            NBTTagCompound cardTag = heldItem.getTagCompound();
            if (cardTag != null) {
                var typeIdTag = AEComponents.EXPORTED_P2P_TYPE_COMPONENT.readFrom(cardTag);
                if (typeIdTag == null) {
                    memoryCard.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                    return false;
                }
                String typeId = typeIdTag.getString();
                Item item = Item.REGISTRY.getObject(new ResourceLocation(typeId));
                if (item instanceof IPartItem<?> partItem && P2PTunnelPart.class.isAssignableFrom(partItem.getPartClass())) {
                    IPart newPart = this;
                    if (newPart.getPartItem() != partItem) {
                        newPart = this.getHost().replacePart(partItem, this.getSide(), player, hand);
                    }

                    if (newPart instanceof P2PTunnelPart) {
                        newPart.importSettings(SettingsFrom.MEMORY_CARD, cardTag, player);
                    }

                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                    return true;
                }
            }

            memoryCard.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
            return false;
        }

        return false;
    }

    @Override
    public void importSettings(SettingsFrom mode, NBTTagCompound input, @Nullable EntityPlayer player) {
        super.importSettings(mode, input, player);

        NBTBase frequencyBase = AEComponents.EXPORTED_P2P_FREQUENCY_COMPONENT.copy(input.getTag(AEComponents.EXPORTED_P2P_FREQUENCY));
        NBTTagShort frequencyTag = frequencyBase instanceof NBTTagShort ? (NBTTagShort) frequencyBase : null;
        if (frequencyTag != null) {
            short frequency = frequencyTag.getShort();
            if (frequency != this.freq) {
                setOutput(true);
                if (getMainNode().getGrid() != null) {
                    P2PService.get(getMainNode().getGrid()).updateFreq(this, frequency);
                } else {
                    setFrequency(frequency);
                    onTunnelNetworkChange();
                }
            }
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, NBTTagCompound output) {
        super.exportSettings(mode, output);

        if (mode == SettingsFrom.MEMORY_CARD) {
            ResourceLocation id = getPartItem().asItem().getRegistryName();
            if (id != null) {
                AEComponents.EXPORTED_P2P_TYPE_COMPONENT.writeTo(output, new NBTTagString(id.toString()));
            }

            if (freq != 0) {
                AEComponents.EXPORTED_P2P_FREQUENCY_COMPONENT.writeTo(output, new NBTTagShort(freq));
                AEColor[] colors = Platform.p2p().toColors(freq);
                MemoryCardItem.setMemoryCardColors(output,
                    new MemoryCardColors(colors[0], colors[0], colors[1], colors[1],
                        colors[2], colors[2], colors[3], colors[3]));
            }
        }
    }

    public void onTunnelConfigChange() {
    }

    public void onTunnelNetworkChange() {
    }

    protected void deductEnergyCost(double energyTransported) {
        double costFactor = AEConfig.instance().getP2PTunnelEnergyTax();
        if (costFactor <= 0) {
            return;
        }

        getMainNode().ifPresent(grid -> {
            double tax = PowerUnit.FE.convertTo(PowerUnit.AE, energyTransported * costFactor);
            grid.getEnergyService().extractAEPower(tax, Actionable.MODULATE, PowerMultiplier.CONFIG);
        });
    }

    protected void deductTransportCost(long amountTransported, AEKeyType typeTransported) {
        double costFactor = AEConfig.instance().getP2PTunnelTransportTax();
        if (costFactor <= 0) {
            return;
        }

        getMainNode().ifPresent(grid -> {
            double operations = amountTransported / (double) typeTransported.getAmountPerOperation();
            double tax = operations * costFactor;
            grid.getEnergyService().extractAEPower(tax, Actionable.MODULATE, PowerMultiplier.CONFIG);
        });
    }

    @Deprecated
    protected void queueTunnelDrain(PowerUnit unit, double amount) {
        final double aeToTax = unit.convertTo(PowerUnit.AE, amount * 0.05);
        getMainNode().ifPresent(grid -> grid.getEnergyService().extractAEPower(aeToTax, Actionable.MODULATE,
            PowerMultiplier.CONFIG));
    }

    public short getFrequency() {
        return this.freq;
    }

    public void setFrequency(short freq) {
        short oldFreq = this.freq;
        this.freq = freq;
        if (oldFreq != this.freq) {
            this.getHost().markForSave();
            this.getHost().markForUpdate();
        }
    }

    private short newFrequency() {
        return (short) ThreadLocalRandom.current().nextInt(1, 1 << 16);
    }

    public boolean isOutput() {
        return this.output;
    }

    void setOutput(boolean output) {
        this.output = output;
        this.getHost().markForSave();
    }

    @Override
    public Object getModelData() {
        return P2PTunnelFrequencyModelData.of(this.getFrequency(), this.isActive() && this.isPowered());
    }
}


