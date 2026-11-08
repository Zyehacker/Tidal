package cc.tidal.tidal.internal.compat;

//? fabric {
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ModMenuShimLoader {
    private static final Logger LOGGER = LogManager.getLogger("Tidal/ModMenuShim");
    private static final String SHIM_RESOURCE = "META-INF/tidal/modmenu-api-shim.jar";
    private static final String SHIM_ENTRYPOINT = "cc.tidal.tidal.internal.compat.ModMenuApiCompat";
    private static boolean loaded;

    private ModMenuShimLoader() {
    }

    public static void enable() {
        if (loaded || CompatLoader.INSTANCE.hasMod("modmenu")) return;
        loaded = true;
        try {
            Path shimPath = extractShimJar();
            FabricLauncherBase.getLauncher().addToClassPath(shimPath);
            Class<?> compatClass = FabricLauncherBase.getLauncher().loadIntoTarget(SHIM_ENTRYPOINT);
            compatClass.getMethod("enable").invoke(null);
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to load Mod Menu API shim", throwable);
        }
    }

    private static Path extractShimJar() throws Exception {
        Path dir = FabricLoader.getInstance().getGameDir().resolve(".tidal");
        Files.createDirectories(dir);
        Path out = dir.resolve("modmenu-api-shim.jar");
        try (InputStream stream = ModMenuShimLoader.class.getClassLoader().getResourceAsStream(SHIM_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + SHIM_RESOURCE);
            }
            Files.copy(stream, out, StandardCopyOption.REPLACE_EXISTING);
        }
        return out;
    }
}
//?}
