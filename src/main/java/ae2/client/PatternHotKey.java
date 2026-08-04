package ae2.client;

import ae2.container.pattern.PatternGuiHandler;
import ae2.core.network.InitNetwork;
import ae2.core.network.serverbound.PatternViewPacket;
import ae2.crafting.pattern.EncodedPatternItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.List;

@SideOnly(Side.CLIENT)
public final class PatternHotKey {

    public static final PatternHotKey INSTANCE = new PatternHotKey();
    private static final KeyBinding VIEW_PATTERN = new KeyBinding(
        "key.ae2.view_pattern", KeyConflictContext.GUI, KeyModifier.NONE, Keyboard.KEY_P, "key.ae2.category");

    private PatternHotKey() {
    }

    public static void init() {
        ClientRegistry.registerKeyBinding(VIEW_PATTERN);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void addPatternViewTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (VIEW_PATTERN.getKeyCode() == Keyboard.KEY_NONE
            || !(stack.getItem() instanceof EncodedPatternItem<?>)) {
            return;
        }

        String keyName = VIEW_PATTERN.getKeyModifier().getLocalizedComboName(VIEW_PATTERN.getKeyCode());
        List<String> tooltip = event.getToolTip();
        tooltip.add(Math.min(1, tooltip.size()), TextFormatting.DARK_GRAY
            + I18n.format("pattern.tooltip", TextFormatting.GRAY + keyName));

        if (VIEW_PATTERN.isPressed()) {
            PatternGuiHandler.prepareClient(stack);
            InitNetwork.sendToServer(new PatternViewPacket(stack));
        }
    }
}
