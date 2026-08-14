package ae2.client.gui.style;

import ae2.core.AELog;
import ae2.core.AppEng;
import ae2.util.Platform;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.TextComponentString;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GuiStyleReloader {

    private static final int MAX_ANCESTOR_DEPTH = 10;
    @Nullable
    public static Path sourceOverride = null;

    private GuiStyleReloader() {
    }

    public static void init() {
        if (!Platform.isDev()) {
            return;
        }
        Path assetsDir = findAssetsDir();
        if (assetsDir == null) {
            AELog.warn("Screen style reload disabled: could not locate src/main/resources/assets/ae2");
            return;
        }
        GuiStyleReloader.sourceOverride = assetsDir;
        AELog.info("Screen style reload enabled: {}", assetsDir);
    }

    public static void reloadAll(ICommandSender sender) {
        if (!Platform.isDev()) {
            sender.sendMessage(new TextComponentString("[AE2] Screen style reload is only available in a dev environment"));
            return;
        }

        ReloadResult result = GuiStyleManager.reloadAll();
        String message = result.failures().isEmpty()
            ? "[AE2] Reloaded " + result.reloaded() + " screen styles"
            : "[AE2] Reloaded " + result.reloaded() + " screen styles, failed: " + String.join("; ", result.failures());
        sender.sendMessage(new TextComponentString(message));
    }

    @Nullable
    private static File getCodeSourceLocation() {
        try {
            var location = AppEng.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null || !"file".equals(location.getProtocol())) {
                return null;
            }
            try {
                return new File(location.toURI());
            } catch (Exception ignored) {
                return new File(URLDecoder.decode(location.getPath(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static Path findAssetsDir() {
        File location = getCodeSourceLocation();
        if (location == null) {
            return null;
        }
        Path current = location.toPath().toAbsolutePath().normalize();
        for (int i = 0; i < MAX_ANCESTOR_DEPTH && current != null; i++) {
            Path candidate = current.resolve("src/main/resources/assets/ae2");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    @Nullable
    public static Path overridePath(String path) {
        if (GuiStyleReloader.sourceOverride == null) {
            return null;
        }
        Path file = GuiStyleReloader.sourceOverride.resolve(path.substring(1)).normalize();
        if (!file.startsWith(GuiStyleReloader.sourceOverride)) {
            return null;
        }
        return Files.isRegularFile(file) ? file : null;
    }

    public record ReloadResult(int reloaded, List<String> failures) { }
}
