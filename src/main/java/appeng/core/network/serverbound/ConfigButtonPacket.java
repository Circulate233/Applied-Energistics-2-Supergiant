package appeng.core.network.serverbound;

import appeng.api.config.Setting;
import appeng.api.config.Settings;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.container.AEBaseContainer;
import appeng.core.AELog;
import appeng.core.network.ServerboundPacket;
import appeng.util.EnumCycler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class ConfigButtonPacket extends ServerboundPacket {

    private Setting<?> option;
    private String optionName;
    private boolean rotationDirection;

    public ConfigButtonPacket() {
    }

    public ConfigButtonPacket(Setting<?> option, boolean rotationDirection) {
        this.option = option;
        this.optionName = option.getName();
        this.rotationDirection = rotationDirection;
    }

    @Override
    protected void read(ByteBuf buf) {
        this.optionName = ByteBufUtils.readUTF8String(buf);
        this.option = Settings.getOrThrow(this.optionName);
        this.rotationDirection = buf.readBoolean();
    }

    @Override
    protected void write(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, getOptionName());
        buf.writeBoolean(this.rotationDirection);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (this.option == null || !(player.openContainer instanceof AEBaseContainer baseContainer)) {
            return;
        }

        var target = baseContainer.getTarget();
        if (target instanceof IConfigurableObject configurableObject) {
            IConfigManager configManager = configurableObject.getConfigManager();
            if (configManager.hasSetting(this.option)) {
                cycleSetting(configManager, this.option);
            } else {
                AELog.info("Ignoring unsupported setting %s sent by client on %s", this.option, target);
            }
        }
    }

    private String getOptionName() {
        if (this.optionName != null) {
            return this.optionName;
        }
        return this.option.getName();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void cycleSetting(IConfigManager configManager, Setting setting) {
        Enum currentValue = configManager.getSetting(setting);
        Enum nextValue = EnumCycler.rotateEnum(currentValue, this.rotationDirection, setting.getValues());
        configManager.putSetting(setting, nextValue);
    }
}
