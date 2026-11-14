/*
 * tidal — proprietary source-viewing license
 * maintained by graphine studios
 * see LICENSE and CREDITS.md for licensing and attribution.
 *
 * provided here for inspection only.
 * this notice does not claim ownership of other contributors' material.
 * no general permission is granted by graphine studios to reuse, copy,
 * modify, redistribute or create derivative works from this file.
 */

package cc.tidal.tidal;

import cc.tidal.tidal.api.TidalConfigScreen;
import cc.tidal.tidal.api.TidalRuntimeToggle;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class TidalModManager {
    static final String TIDAL_TOGGLE_ENTRYPOINT = "tidal:toggle";
    static final String TIDAL_CONFIG_ENTRYPOINT = "tidal:config";
    private static final String MODMENU_ENTRYPOINT = "modmenu";
    private static final Logger LOGGER = LoggerFactory.getLogger("Tidal Mods");
    private static final int ICON_SIZE = 32;
    private static final int MAX_ICON_DIMENSION = 8192;
    private static final long MAX_ICON_PIXELS = 16_777_216L;
    private static final long TOGGLE_DELAY_NANOS = 100_000_000L;
    private static final long MAX_ICON_BYTES = 8_388_608L;
    private static final ExecutorService ICON_WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Tidal Mod Icon Loader");
        thread.setDaemon(true);
        return thread;
    });
    private static final List<ModEntry> MODS = new ArrayList<>();
    private static final Deque<PendingToggle> PENDING_TOGGLES = new ArrayDeque<>();
    private static boolean initialized;

    private TidalModManager() {}

    static void initialize() {
        if (initialized) return;
        initialized = true;
        FabricLoader loader = FabricLoader.getInstance();
        Map<String, ToggleAccess> toggles = loadTidalToggles(loader);
        Map<String, TidalConfigScreen> configScreens = loadTidalConfigScreens(loader);
        Map<String, Object> modMenuApis = loadModMenuApis(loader);

        List<ModContainer> containers = new ArrayList<>(loader.getAllMods());
        containers.sort(Comparator.comparing((ModContainer container) ->
                        container.getMetadata().getName().toLowerCase(Locale.ROOT))
                .thenComparing(container -> container.getMetadata().getId()));
        for (ModContainer container : containers) {
            var metadata = container.getMetadata();
            String id = metadata.getId();
            ModEntry entry = new ModEntry(container, metadata.getName(), id,
                    metadata.getVersion().getFriendlyString(), metadata.getDescription(),
                    toggles.get(id), configScreens.get(id), modMenuApis.get(id));
            entry.favorite = TidalConfig.isFavoriteMod(id);
            entry.readToggleState();
            MODS.add(entry);
            loadIcon(entry, metadata.getIconPath(128));
        }
    }

    private static Method accessibleMethod(Object instance, String name,
                                           Class<?>... parameterTypes)
            throws ReflectiveOperationException {
        Method method = instance.getClass().getMethod(name, parameterTypes);
        if (!method.canAccess(instance) && !method.trySetAccessible()) {
            throw new IllegalAccessException("Cannot access " + name);
        }
        return method;
    }

    static List<ModEntry> all() {
        return List.copyOf(MODS);
    }

    static List<ModEntry> search(String query) {
        if (query.isBlank()) return all();
        List<ModEntry> matches = new ArrayList<>();
        for (ModEntry entry : MODS) {
            if (matches(entry, query)) matches.add(entry);
        }
        return matches;
    }

    static List<ModEntry> exportableMods() {
        return all();
    }

    static void setFavorite(ModEntry entry, boolean favorite) {
        entry.favorite = favorite;
        TidalConfig.setFavoriteMod(entry.id, favorite);
    }

    static void requestToggle(ModEntry entry) {
        if (entry.toggle == null || entry.pending) return;
        entry.pending = true;
        entry.pendingEnabled = !entry.enabled;
        entry.failure = null;
        PENDING_TOGGLES.addLast(new PendingToggle(entry,
                System.nanoTime() + TOGGLE_DELAY_NANOS));
    }

    static void tick() {
        long now = System.nanoTime();
        while (!PENDING_TOGGLES.isEmpty()
                && PENDING_TOGGLES.peekFirst().executeAfterNanos <= now) {
            PendingToggle pending = PENDING_TOGGLES.removeFirst();
            applyToggle(pending.entry);
        }
    }

    static boolean openSettings(ModEntry entry, Screen parent) {
        entry.settingsFailure = null;
        try {
            Screen screen = entry.tidalConfig == null ? null : entry.tidalConfig.create(parent);
            if (screen == null && entry.modMenuApi != null) {
                screen = createModMenuScreen(entry.modMenuApi, parent);
            }
            if (screen == null) {
                entry.settingsFailure = "Settings unavailable";
                return false;
            }
            Minecraft.getInstance().setScreen(screen);
            return true;
        } catch (Exception | LinkageError exception) {
            LOGGER.error("Could not open settings for mod {}", entry.id, exception);
            entry.settingsFailure = "Settings unavailable";
            return false;
        }
    }

    private static Screen createModMenuScreen(Object api, Screen parent)
            throws ReflectiveOperationException {
        Method factoryMethod = accessibleMethod(api, "getModConfigScreenFactory");
        Object factory = factoryMethod.invoke(api);
        if (factory == null) return null;
        Method createMethod = accessibleMethod(factory, "create", Screen.class);
        Object result = createMethod.invoke(factory, parent);
        return result instanceof Screen screen ? screen : null;
    }

    static Identifier icon(String id) {
        for (ModEntry entry : MODS) {
            if (entry.id.equals(id)) return entry.icon;
        }
        return null;
    }

    static String iconData(String id) {
        for (ModEntry entry : MODS) {
            if (entry.id.equals(id)) return entry.iconData;
        }
        return null;
    }

    private static Map<String, ToggleAccess> loadTidalToggles(FabricLoader loader) {
        Map<String, ToggleAccess> toggles = new HashMap<>();
        for (EntrypointContainer<TidalRuntimeToggle> container
                : loader.getEntrypointContainers(TIDAL_TOGGLE_ENTRYPOINT,
                TidalRuntimeToggle.class)) {
            String id = container.getProvider().getMetadata().getId();
            try {
                addToggle(toggles, id, tidalToggleAccess(container.getEntrypoint()));
            } catch (RuntimeException | LinkageError exception) {
                LOGGER.error("Could not load Tidal toggle entrypoint for mod {}", id, exception);
            }
        }
        return toggles;
    }

    private static void addToggle(Map<String, ToggleAccess> toggles, String id,
                                  ToggleAccess access) {
        if (toggles.putIfAbsent(id, access) != null) {
            LOGGER.warn("Ignoring duplicate runtime toggle for mod {}", id);
        }
    }

    static ToggleAccess tidalToggleAccess(TidalRuntimeToggle instance) {
        return new ToggleAccess(instance::isEnabled, instance::setEnabled);
    }

    static boolean matches(ModEntry entry, String query) {
        String searchable = (entry.name + " " + entry.id + " " + entry.version + " "
                + entry.description).toLowerCase(Locale.ROOT);
        for (String token : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!searchable.contains(token)) return false;
        }
        return true;
    }

    static boolean isSafeIconPath(String relative) {
        if (relative == null || relative.isBlank()) return false;
        char first = relative.charAt(0);
        boolean hasDrivePrefix = relative.length() >= 2 && relative.charAt(1) == ':'
                && ((first >= 'A' && first <= 'Z') || (first >= 'a' && first <= 'z'));
        if (first == '/' || relative.indexOf('\\') >= 0 || hasDrivePrefix) return false;
        try {
            Path path = Path.of(relative).normalize();
            return !path.isAbsolute() && !path.startsWith("..") && !path.toString().isBlank()
                    && !path.toString().equals(".");
        } catch (RuntimeException exception) {
            return false;
        }
    }

    static void applyToggle(ModEntry entry) {
        boolean target = !entry.enabled;
        try {
            entry.toggle.setEnabled(target);
            boolean actual = entry.toggle.isEnabled();
            if (actual != target) {
                throw new IllegalStateException("Mod reported " + actual
                        + " after requesting " + target);
            }
            entry.enabled = actual;
            entry.failure = null;
        } catch (Exception | LinkageError exception) {
            entry.failure = "Toggle failed";
            LOGGER.error("Cooperative runtime toggle failed for mod {}", entry.id, exception);
        } finally {
            entry.pending = false;
        }
    }

    private static Map<String, TidalConfigScreen> loadTidalConfigScreens(FabricLoader loader) {
        Map<String, TidalConfigScreen> screens = new HashMap<>();
        for (EntrypointContainer<TidalConfigScreen> container
                : loader.getEntrypointContainers(TIDAL_CONFIG_ENTRYPOINT,
                TidalConfigScreen.class)) {
            String id = container.getProvider().getMetadata().getId();
            try {
                if (screens.putIfAbsent(id, container.getEntrypoint()) != null) {
                    LOGGER.warn("Ignoring duplicate Tidal config screen entrypoint for mod {}", id);
                }
            } catch (RuntimeException | LinkageError exception) {
                LOGGER.error("Could not load Tidal config entrypoint for mod {}", id, exception);
            }
        }
        return screens;
    }

    private static Map<String, Object> loadModMenuApis(FabricLoader loader) {
        Map<String, Object> apis = new HashMap<>();
        for (EntrypointContainer<Object> container
                : loader.getEntrypointContainers(MODMENU_ENTRYPOINT, Object.class)) {
            String id = container.getProvider().getMetadata().getId();
            try {
                Object api = container.getEntrypoint();
                Method getFactory = accessibleMethod(api, "getModConfigScreenFactory");
                Object factory = getFactory.invoke(api);
                if (factory == null || factory.getClass().getName()
                        .equals("com.terraformersmc.modmenu.util.NullScreenFactory")) continue;
                apis.putIfAbsent(id, api);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                LOGGER.debug("Ignoring unsupported Mod Menu configuration for {}", id, exception);
            }
        }
        return apis;
    }

    private static void loadIcon(ModEntry entry, Optional<String> iconPath) {
        if (iconPath.isEmpty()) return;
        String relative = iconPath.get();
        Path relativePath;
        try {
            relativePath = Path.of(relative).normalize();
        } catch (RuntimeException exception) {
            LOGGER.warn("Ignoring invalid icon path for mod {}", entry.id, exception);
            return;
        }
        if (!isSafeIconPath(relative)) {
            LOGGER.warn("Ignoring unsafe icon path for mod {}: {}", entry.id, relative);
            return;
        }
        try {
            Optional<Path> found = entry.container.findPath(relativePath.toString());
            if (found.isEmpty()) return;
            ICON_WORKER.execute(() -> decodeIcon(entry, found.get()));
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not resolve icon path for mod {}", entry.id, exception);
        }
    }

    private static void decodeIcon(ModEntry entry, Path path) {
        NativeImage image = null;
        try {
            image = decodeIconImage(path);
            entry.iconData = encodeIcon(image);
            NativeImage decodedImage = image;
            RenderSystem.queueFencedTask(() -> uploadIcon(entry, decodedImage));
        } catch (IOException | RuntimeException exception) {
            if (image != null) image.close();
            LOGGER.warn("Could not load icon for mod {} from {}", entry.id, path, exception);
        }
    }

    private static String encodeIcon(NativeImage image) throws IOException {
        BufferedImage buffered = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        try {
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int abgr = image.getPixel(x, y);
                    int argb = (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16)
                            | ((abgr >>> 16) & 0xFF);
                    buffered.setRGB(x, y, argb);
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream(2048);
            if (!ImageIO.write(buffered, "png", output)) {
                throw new IOException("No PNG encoder is available");
            }
            return "data:image/png;base64," + Base64.getEncoder()
                    .encodeToString(output.toByteArray());
        } finally {
            buffered.flush();
        }
    }

    static NativeImage decodeIconImage(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path);
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (Files.size(path) > MAX_ICON_BYTES) {
                throw new IOException("Icon file exceeds the safe decode limit");
            }
            if (imageInput == null) throw new IOException("Unsupported image stream");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw new IOException("Unsupported image format");
            ImageReader reader = readers.next();
            BufferedImage decoded;
            try {
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_ICON_DIMENSION
                        || height > MAX_ICON_DIMENSION
                        || (long) width * height > MAX_ICON_PIXELS) {
                    throw new IOException("Icon dimensions exceed the safe decode limit");
                }
                decoded = reader.read(0, reader.getDefaultReadParam());
            } finally {
                reader.dispose();
            }
            if (decoded == null) throw new IOException("Image decoder returned no pixels");
            try {
                return resizeIcon(decoded);
            } finally {
                decoded.flush();
            }
        }
    }

    private static NativeImage resizeIcon(BufferedImage source) {
        BufferedImage scaled = new BufferedImage(ICON_SIZE, ICON_SIZE,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            double scale = Math.min(ICON_SIZE / (double) source.getWidth(),
                    ICON_SIZE / (double) source.getHeight());
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            graphics.drawImage(source, (ICON_SIZE - width) / 2,
                    (ICON_SIZE - height) / 2, width, height, null);
        } finally {
            graphics.dispose();
        }
        NativeImage image = new NativeImage(NativeImage.Format.RGBA,
                ICON_SIZE, ICON_SIZE, false);
        try {
            for (int y = 0; y < ICON_SIZE; y++) {
                for (int x = 0; x < ICON_SIZE; x++) {
                    image.setPixel(x, y, ARGB.toABGR(scaled.getRGB(x, y)));
                }
            }
            return image;
        } catch (RuntimeException | Error exception) {
            image.close();
            throw exception;
        } finally {
            scaled.flush();
        }
    }

    private static void uploadIcon(ModEntry entry, NativeImage image) {
        try {
            String safeId = entry.id.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9_/.-]", "_") + "_"
                    + Integer.toUnsignedString(entry.id.hashCode(), 36);
            Identifier id = Identifier.fromNamespaceAndPath("tidal", "mod_icons/" + safeId);
            DynamicTexture texture = new IconTexture(entry.name, image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            entry.icon = id;
        } catch (RuntimeException exception) {
            image.close();
            LOGGER.warn("Could not register icon for mod {}", entry.id, exception);
        }
    }

    private interface ToggleReader {
        boolean get() throws Exception;
    }

    private interface ToggleWriter {
        void set(boolean enabled) throws Exception;
    }

    record ToggleAccess(ToggleReader reader, ToggleWriter writer) {
        boolean isEnabled() throws Exception {
            return reader.get();
        }

        void setEnabled(boolean enabled) throws Exception {
            writer.set(enabled);
        }
    }

    private record PendingToggle(ModEntry entry, long executeAfterNanos) {}

    static final class ModEntry {
        final ModContainer container;
        final String name;
        final String id;
        final String version;
        final String description;
        final String authors;
        final String environment;
        final ToggleAccess toggle;
        final TidalConfigScreen tidalConfig;
        final Object modMenuApi;
        volatile Identifier icon;
        volatile String iconData;
        boolean enabled;
        boolean favorite;
        boolean pending;
        boolean pendingEnabled;
        String failure;
        String settingsFailure;

        ModEntry(ModContainer container, String name, String id, String version,
                 String description, ToggleAccess toggle,
                 TidalConfigScreen tidalConfig, Object modMenuApi) {
            this.container = container;
            this.name = name;
            this.id = id;
            this.version = version;
            this.description = description;
            var metadata = container == null ? null : container.getMetadata();
            authors = metadata == null ? "" : metadata.getAuthors().stream()
                    .map(person -> person.getName())
                    .collect(java.util.stream.Collectors.joining(", "));
            environment = metadata == null ? "UNKNOWN" : metadata.getEnvironment().name();
            this.toggle = toggle;
            this.tidalConfig = tidalConfig;
            this.modMenuApi = modMenuApi;
        }

        boolean canToggle() {
            return toggle != null;
        }

        boolean canOpenSettings() {
            return tidalConfig != null || modMenuApi != null;
        }

        String status() {
            if (pending) return "Applying…";
            if (failure != null) return failure;
            String state = toggle == null ? "Restart required"
                    : enabled ? "Enabled" : "Disabled";
            return settingsFailure == null ? state : state + " · Settings unavailable";
        }

        private void readToggleState() {
            if (toggle == null) return;
            try {
                enabled = toggle.isEnabled();
            } catch (Exception | LinkageError exception) {
                failure = "Toggle unavailable";
                LOGGER.error("Could not read runtime state for mod {}", id, exception);
            }
        }
    }

    private static final class IconTexture extends DynamicTexture {
        private IconTexture(String label, NativeImage pixels) {
            super(() -> "Tidal mod icon " + label, pixels);
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
    }
}
