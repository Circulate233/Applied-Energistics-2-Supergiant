package ae2.client.gui.implementations;

import ae2.client.gui.AEBaseGui;
import ae2.client.gui.color.ColorArea;
import ae2.client.gui.color.ColorWindow;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.render.NetworkDataHandler;
import ae2.me.NetworkData;
import ae2.container.implementations.ContainerNetworkAnalyser;
import ae2.core.AEConfig;
import ae2.core.localization.GuiText;
import ae2.core.network.InitNetwork;
import ae2.core.network.serverbound.AnalyserGenericPacket;
import ae2.core.network.serverbound.NetworkConfigSavePacket;
import ae2.items.tools.NetworkAnalyserConfig;
import ae2.me.AnalyserMode;
import ae2.me.netdata.LinkFlag;
import ae2.me.netdata.NodeFlag;
import ae2.util.ColorData;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.TextComponentString;

import java.util.List;

public class GuiNetworkAnalyser extends AEBaseGui<ContainerNetworkAnalyser> {
    private static final String STYLE_PATH = "/screens/network_analyser.json";

    private static final List<Enum<?>> COLOR_ORDER = List.of(
        LinkFlag.NORMAL, LinkFlag.DENSE, LinkFlag.COMPRESSED,
        NodeFlag.NORMAL, NodeFlag.DENSE, NodeFlag.MISSING
    );

    private final Reference2ObjectOpenHashMap<Enum<?>, ColorData> colors = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<Enum<?>, ColorArea> colorBtns = new Reference2ObjectOpenHashMap<>();
    private final ColorWindow colorWindow;

    private AnalyserMode mode = AnalyserMode.FULL;
    private float size = 0.4F;

    public GuiNetworkAnalyser(ContainerNetworkAnalyser container, InventoryPlayer playerInventory) {
        super(container, playerInventory, GuiStyleManager.loadStyleDoc(STYLE_PATH));
        this.colors.putAll(NetworkAnalyserConfig.DEFAULT_COLORS);

        this.widgets.addButton("mode_previous", new TextComponentString("<"), () -> changeMode(-1));
        this.widgets.addButton("mode_next", new TextComponentString(">"), () -> changeMode(1));
        this.widgets.addButton("size_decrease", new TextComponentString("<"), () -> changeSize(-0.1F));
        this.widgets.addButton("size_increase", new TextComponentString(">"), () -> changeSize(0.1F));
        this.widgets.addButton("reset_colors", GuiText.NetworkAnalyserReset.text(),
            this::loadDefault);
        for (int i = 0; i < COLOR_ORDER.size(); i++) {
            Enum<?> type = COLOR_ORDER.get(i);
            ColorArea button = new ColorArea(0, 0, 0, 0, this, () -> beginColorConfig(type));
            this.widgets.add("color_" + i, button);
            this.colorBtns.put(type, button);
        }

        this.colorWindow = new ColorWindow(0, 0, 0, 0, this,
            () -> closeColorConfig(true),
            () -> closeColorConfig(false));
        this.widgets.add("color_window", this.colorWindow);

        loadConfig(container.getConfig());
        InitNetwork.sendToServer(new AnalyserGenericPacket(this.container.windowId, "update"));
    }

    public void loadConfig(NetworkAnalyserConfig config) {
        this.mode = config.mode();
        this.size = config.nodeSize();
        this.colors.clear();
        this.colors.ensureCapacity(NetworkAnalyserConfig.DEFAULT_COLORS.size() + config.colors().size());
        this.colors.putAll(NetworkAnalyserConfig.DEFAULT_COLORS);
        this.colors.putAll(config.colors());
        for (var entry : this.colors.entrySet()) {
            ColorArea button = this.colorBtns.get(entry.getKey());
            if (button != null) {
                button.setColor(entry.getValue());
            }
        }
        NetworkDataHandler.updateConfig(new NetworkAnalyserConfig(this.mode, this.size, this.colors));
    }

    public void loadDefault() {
        this.colors.clear();
        this.colors.ensureCapacity(NetworkAnalyserConfig.DEFAULT_COLORS.size());
        this.colors.putAll(NetworkAnalyserConfig.DEFAULT_COLORS);
        for (var entry : this.colors.entrySet()) {
            ColorArea button = this.colorBtns.get(entry.getKey());
            if (button != null) {
                button.setColor(entry.getValue());
            }
        }
        syncConfig();
    }

    public void closeColorConfig(boolean save) {
        if (save) {
            ColorArea colorBtn = this.colorBtns.get(this.colorWindow.getConfigType());
            ColorData newData = this.colorWindow.getColor();
            colorBtn.setColor(newData);
            this.colors.put(this.colorWindow.getConfigType(), newData);
            syncConfig();
        }
        this.colorWindow.close();
    }

    private void beginColorConfig(Enum<?> type) {
        ColorData color = this.colorBtns.get(type).getColor();
        this.colorWindow.open(type, color);
    }

    private NetworkData lastData;
    private String lastModeText = "";
    private String lastSizeText = "";

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        var data = NetworkDataHandler.pullData();
        boolean dataChanged = data != this.lastData;
        this.lastData = data;
        if (dataChanged) {
            setTextContent("normal_nodes", GuiText.NetworkAnalyserStateNormalNodes.text(data.countNode(NodeFlag.NORMAL)));
            setTextContent("dense_nodes", GuiText.NetworkAnalyserStateDenseNodes.text(data.countNode(NodeFlag.DENSE)));
            setTextContent("missing_nodes", GuiText.NetworkAnalyserStateMissingNodes.text(
                data.countNode(NodeFlag.MISSING)));
        }
        var modeComponent = this.mode.getTranslatedName();
        String modeKey = modeComponent.getFormattedText();
        if (!modeKey.equals(this.lastModeText)) {
            this.lastModeText = modeKey;
            setTextContent("mode_value", modeComponent);
        }
        String sizeText = String.valueOf((int) (this.size * 10));
        if (!sizeText.equals(this.lastSizeText)) {
            this.lastSizeText = sizeText;
            setTextContent("node_size_value", new TextComponentString(sizeText));
        }
        setTextContent("channel_mode", GuiText.networkAnalyserChannel(AEConfig.instance().getChannelMode()).text());
        for (int i = 0; i < COLOR_ORDER.size(); i++) {
            Enum<?> entry = COLOR_ORDER.get(i);
            setTextContent("color_label_" + i, GuiText.networkAnalyserLabel(entry).text());
        }
    }

    @Override
    public void onGuiClosed() {
        syncConfig();
        super.onGuiClosed();
    }

    private void changeMode(int offset) {
        this.mode = AnalyserMode.byIndex(this.mode.ordinal() + offset);
        syncConfig();
    }

    private void changeSize(float offset) {
        this.size = Math.clamp(this.size + offset, 0.1F, 0.9F);
        syncConfig();
    }

    private void syncConfig() {
        NetworkAnalyserConfig config = new NetworkAnalyserConfig(this.mode, this.size,
            new Reference2ObjectOpenHashMap<>(this.colors));
        NetworkDataHandler.updateConfig(config);
        getContainer().saveConfig(config);
        InitNetwork.sendToServer(new NetworkConfigSavePacket(getContainer().windowId, config));
    }
}
