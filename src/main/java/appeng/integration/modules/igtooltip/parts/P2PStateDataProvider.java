package appeng.integration.modules.igtooltip.parts;

import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.api.integrations.igtooltip.providers.ServerDataProvider;
import appeng.core.localization.InGameTooltip;
import appeng.parts.p2p.MEP2PTunnelPart;
import appeng.parts.p2p.P2PTunnelPart;
import appeng.util.Platform;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;

@SuppressWarnings("rawtypes")
public final class P2PStateDataProvider implements BodyProvider<P2PTunnelPart>, ServerDataProvider<P2PTunnelPart> {
    public static final String TAG_P2P_STATE = "p2pState";
    public static final String TAG_P2P_OUTPUTS = "p2pOutputs";
    public static final String TAG_P2P_FREQUENCY = "p2pFrequency";
    public static final String TAG_P2P_FREQUENCY_NAME = "p2pFrequencyName";
    public static final String TAG_P2P_ME_CARRIED_CHANNELS = "p2pCarriedChannels";
    private static final byte STATE_UNLINKED = 0;
    private static final byte STATE_OUTPUT = 1;
    private static final byte STATE_INPUT = 2;

    private static ITextComponent getOutputText(int outputs) {
        if (outputs <= 1) {
            return InGameTooltip.P2PInputOneOutput.text();
        }
        return InGameTooltip.P2PInputManyOutputs.text(outputs);
    }

    @Override
    public void buildTooltip(P2PTunnelPart object, TooltipContext context, TooltipBuilder tooltip) {
        NBTTagCompound serverData = context.serverData();

        if (serverData.hasKey(TAG_P2P_STATE, 1)) {
            byte state = serverData.getByte(TAG_P2P_STATE);
            int outputs = serverData.getInteger(TAG_P2P_OUTPUTS);

            switch (state) {
                case STATE_UNLINKED:
                    tooltip.addLine(InGameTooltip.P2PUnlinked.text());
                    break;
                case STATE_OUTPUT:
                    tooltip.addLine(InGameTooltip.P2POutput.text());
                    break;
                case STATE_INPUT:
                    tooltip.addLine(getOutputText(outputs));
                    break;
                default:
                    break;
            }

            short freq = serverData.getShort(TAG_P2P_FREQUENCY);
            ITextComponent freqTooltip = Platform.p2p().toColoredHexString(freq).setStyle(new Style().setBold(true));
            if (serverData.hasKey(TAG_P2P_FREQUENCY_NAME, 8)) {
                String freqName = serverData.getString(TAG_P2P_FREQUENCY_NAME);
                freqTooltip = new TextComponentString(freqName).appendText(" (").appendSibling(freqTooltip).appendText(")");
            }

            tooltip.addLine(InGameTooltip.P2PFrequency.text(freqTooltip));

            if (serverData.hasKey(TAG_P2P_ME_CARRIED_CHANNELS, 3)) {
                int carriedChannels = serverData.getInteger(TAG_P2P_ME_CARRIED_CHANNELS);
                tooltip.addLine(InGameTooltip.P2PMECarriedChannels.text(carriedChannels));
            }
        }
    }

    @Override
    public void provideServerData(EntityPlayer player, P2PTunnelPart part, NBTTagCompound serverData) {
        if (!part.isPowered()) {
            return;
        }

        serverData.setShort(TAG_P2P_FREQUENCY, part.getFrequency());

        byte state = STATE_UNLINKED;
        if (!part.isOutput()) {
            int outputCount = part.getOutputs().size();
            if (outputCount > 0) {
                state = STATE_INPUT;
                serverData.setInteger(TAG_P2P_OUTPUTS, outputCount);
            }

            if (part.getCustomName() != null) {
                serverData.setString(TAG_P2P_FREQUENCY_NAME, part.getCustomName().getUnformattedText());
            }
        } else {
            var input = part.getInput();
            if (input != null) {
                state = STATE_OUTPUT;
                if (input.getCustomName() != null) {
                    serverData.setString(TAG_P2P_FREQUENCY_NAME, input.getCustomName().getUnformattedText());
                }
            }
        }

        serverData.setByte(TAG_P2P_STATE, state);

        if (part instanceof MEP2PTunnelPart meTunnel) {
            var externalNode = meTunnel.getExternalFacingNode();
            if (externalNode != null) {
                serverData.setInteger(TAG_P2P_ME_CARRIED_CHANNELS, externalNode.getUsedChannels());
            }
        }
    }
}
