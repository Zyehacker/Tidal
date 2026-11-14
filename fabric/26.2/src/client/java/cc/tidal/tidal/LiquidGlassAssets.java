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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class LiquidGlassAssets {
    private static final Logger LOGGER = LoggerFactory.getLogger("Tidal Liquid Glass");
    private static final String RESOURCE_ROOT = "/assets/tidal/textures/gui/liquid-glass/";
    private static final String TIDAL_ROOT = RESOURCE_ROOT + "tidal/";
    private static final long CACHE_LIMIT_BYTES = 128L * 1024 * 1024;
    private static final ExecutorService RASTER_THREAD = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Tidal Liquid Glass Rasterizer");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, Asset> ASSETS = loadAssets();
    private static final Map<TextureKey, CompletableFuture<NativeImage>> PENDING =
            new ConcurrentHashMap<>();
    private static final Map<TextureKey, Throwable> FAILURES = new ConcurrentHashMap<>();
    private static final LinkedHashMap<TextureKey, TextureEntry> TEXTURES =
            new LinkedHashMap<>(16, 0.75f, true);
    private static long cacheBytes;
    private static double scaleX = -1;
    private static double scaleY = -1;
    private static volatile int galleryScale;

    private LiquidGlassAssets() {}

    static Asset require(String path) {
        Asset asset = ASSETS.get(path);
        if (asset == null) asset = ASSETS.get(canonicalPath(path));
        if (asset == null) {
            throw new IllegalArgumentException("Unknown Liquid Glass asset: " + path);
        }
        return asset;
    }

    static boolean contains(String path) {
        return ASSETS.containsKey(path) || ASSETS.containsKey(canonicalPath(path));
    }

    private static String canonicalPath(String path) {
        if (path.startsWith("tidal/")) return path;
        if (path.equals("surfaces/window-bg")) return "tidal/ui/polyglass_window_frame";
        if (path.equals("surfaces/panel") || path.startsWith("surfaces/menu-item-")
                || path.equals("buttons/icon-button-selected")
                || path.startsWith("module-cards/module-card-")
                || path.startsWith("buttons/nav-")) {
            return path.endsWith("hover") || path.endsWith("selected")
                    ? "tidal/ui/polyglass_module_card_hover"
                    : "tidal/ui/polyglass_module_card";
        }
        if (path.startsWith("toggles/toggle-")) {
            return "tidal/ui/toggle_" + (path.contains("on") ? "on" : "off");
        }
        if (path.startsWith("borders/")) return "tidal/ui/polyglass_module_card_hover";
        if (path.startsWith("inputs/field-")) {
            return path.endsWith("focused") || path.endsWith("hover")
                    ? "components/field_focus" : "components/field_default";
        }
        if (path.startsWith("surfaces/tooltip")) return "components/tooltip";
        if (path.startsWith("buttons/")) return buttonAsset(path);
        if (path.startsWith("controls/radio-")) return "components/slider_thumb";
        if (path.startsWith("controls/slider-fill")) return "components/slider_fill";
        if (path.startsWith("controls/slider-thumb")
                || path.startsWith("controls/knob-")) return "components/slider_thumb";
        if (path.startsWith("controls/slider-track")
                || path.startsWith("controls/scrollbar-track")) return "components/slider_track";
        if (path.startsWith("icons/")) return iconAsset(path.substring("icons/".length()));
        return path;
    }

    private static String buttonAsset(String path) {
        if (path.endsWith("disabled")) return "components/button_disabled";
        if (path.startsWith("buttons/button-primary-")) return "components/button_accent";
        if (path.endsWith("pressed")) return "components/button_pressed";
        if (path.endsWith("hover")) return "components/button_hover";
        return "components/button_default";
    }

    private static String iconAsset(String name) {
        String icon = switch (name) {
            case "success", "check", "select" -> "check";
            case "warning", "info" -> "info";
            case "settings", "edit" -> "color";
            case "hidden" -> "eye_off";
            case "visibility" -> "eye";
            case "lock", "unlock", "group" -> "group";
            case "keybind" -> "nav_keybinds";
            case "hud-position" -> "nav_hud_editor";
            case "up" -> "chevron_up";
            case "down" -> "chevron_down";
            case "left", "back" -> "chevron_left";
            case "right", "forward" -> "chevron_right";
            case "expand", "add" -> "plus";
            case "collapse", "minus" -> "minus";
            case "reset" -> "reset";
            case "delete", "close" -> "close";
            default -> name;
        };
        String uiPath = "icons/ui/" + icon;
        return uiPath;
    }

    static List<Asset> all() {
        return List.copyOf(ASSETS.values());
    }

    static int assetCount() {
        return ASSETS.size();
    }

    static void setGalleryScale(int scale) {
        if (scale < 1 || scale > 4) throw new IllegalArgumentException("Invalid gallery scale");
        galleryScale = scale;
    }

    static void clearGalleryScale() {
        galleryScale = 0;
    }

    static Identifier texture(Asset asset) {
        return texture(asset, TidalTheme.PRIMARY);
    }

    static Identifier texture(Asset asset, int currentColor) {
        Minecraft minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        int guiWidth = Math.max(1, window.getGuiScaledWidth());
        int guiHeight = Math.max(1, window.getGuiScaledHeight());
        int override = galleryScale;
        double nextScaleX = override == 0
                ? resolveDeviceScale(window.getWidth(), guiWidth, window.getGuiScale()) : override;
        double nextScaleY = override == 0
                ? resolveDeviceScale(window.getHeight(), guiHeight, window.getGuiScale()) : override;
        synchronized (TEXTURES) {
            if (Double.compare(scaleX, nextScaleX) != 0
                    || Double.compare(scaleY, nextScaleY) != 0) {
                scaleX = nextScaleX;
                scaleY = nextScaleY;
                LOGGER.debug("Liquid Glass device scale changed to {}x{}", scaleX, scaleY);
            }
        }
        int deviceWidth = Math.max(1, (int) Math.ceil(asset.width * (double) nextScaleX));
        int deviceHeight = Math.max(1, (int) Math.ceil(asset.height * (double) nextScaleY));
        int requestedQuality = Math.max(1, Math.min(4, TidalConfig.uiAntialiasing));
        int quality = rasterQuality(requestedQuality, deviceWidth, deviceHeight);
        int tintColor = asset.currentColorTint ? currentColor | 0xFF000000 : -1;
        TextureKey key = new TextureKey(asset.path, deviceWidth, deviceHeight, quality,
                tintColor);
        synchronized (TEXTURES) {
            TextureEntry entry = TEXTURES.get(key);
            if (entry != null) return entry.identifier;
        }
        Throwable failure = FAILURES.get(key);
        if (failure != null) {
            throw new IllegalStateException("Liquid Glass rasterization failed for "
                    + asset.path, failure);
        }
        PENDING.computeIfAbsent(key, ignored -> rasterizeAsync(asset, key));
        return null;
    }

    private static int rasterQuality(int requested, int width, int height) {
        long devicePixels = (long) width * height;
        long maximumPixels = CACHE_LIMIT_BYTES * 3 / 4 / Integer.BYTES;
        int memoryLimited = (int) Math.floor(Math.sqrt(maximumPixels / (double) devicePixels));
        return Math.max(1, Math.min(requested, memoryLimited));
    }

    static void preload(List<String> paths) {
        List<String> ordered = new ArrayList<>(new LinkedHashSet<>(paths));
        ordered.sort(Comparator.comparingInt(LiquidGlassAssets::preloadPriority)
                .thenComparing(path -> path));
        for (String path : ordered) texture(require(path));
    }

    private static int preloadPriority(String path) {
        return switch (path) {
            case "components/panel" -> 0;
            case "components/card_default" -> 1;
            case "components/button_default" -> 2;
            case "components/field_default" -> 3;
            default -> path.startsWith("components/") ? 4
                    : path.startsWith("icons/ui/") ? 5 : 6;
        };
    }

    static int deviceWidth(Asset asset) {
        Minecraft minecraft = Minecraft.getInstance();
        int guiWidth = Math.max(1, minecraft.getWindow().getGuiScaledWidth());
        double scale = galleryScale == 0
                ? resolveDeviceScale(minecraft.getWindow().getWidth(), guiWidth,
                minecraft.getWindow().getGuiScale()) : galleryScale;
        return Math.max(1, (int) Math.ceil(asset.width * scale));
    }

    static double deviceScaleX() {
        Minecraft minecraft = Minecraft.getInstance();
        int guiWidth = Math.max(1, minecraft.getWindow().getGuiScaledWidth());
        return resolveDeviceScale(minecraft.getWindow().getWidth(), guiWidth,
                minecraft.getWindow().getGuiScale());
    }

    static double deviceScaleY() {
        Minecraft minecraft = Minecraft.getInstance();
        int guiHeight = Math.max(1, minecraft.getWindow().getGuiScaledHeight());
        return resolveDeviceScale(minecraft.getWindow().getHeight(), guiHeight,
                minecraft.getWindow().getGuiScale());
    }

    static int deviceHeight(Asset asset) {
        Minecraft minecraft = Minecraft.getInstance();
        int guiHeight = Math.max(1, minecraft.getWindow().getGuiScaledHeight());
        double scale = galleryScale == 0
                ? resolveDeviceScale(minecraft.getWindow().getHeight(), guiHeight,
                minecraft.getWindow().getGuiScale()) : galleryScale;
        return Math.max(1, (int) Math.ceil(asset.height * scale));
    }

    static double resolveDeviceScale(int pixelSize, int logicalSize, int fallbackScale) {
        if (pixelSize > 0 && logicalSize > 0) {
            double measured = pixelSize / (double) logicalSize;
            if (Double.isFinite(measured) && measured >= 1 && measured <= 16) return measured;
        }
        return fallbackScale > 0 && fallbackScale <= 16 ? fallbackScale : 1;
    }

    private static CompletableFuture<NativeImage> rasterizeAsync(Asset asset, TextureKey key) {
        return CompletableFuture.supplyAsync(() -> {
            BufferedImage rendered = asset.scene.rasterize(
                    key.width, key.height, key.quality,
                    key.currentColor == -1 ? TidalTheme.PRIMARY : key.currentColor);
            NativeImage image = new NativeImage(NativeImage.Format.RGBA,
                    key.width, key.height, false);
            try {
                int[] pixels = ((DataBufferInt) rendered.getRaster()
                        .getDataBuffer()).getData();
                for (int i = 0; i < pixels.length; i++) pixels[i] = ARGB.toABGR(pixels[i]);
                image.getPixelBytes().order(ByteOrder.nativeOrder())
                        .asIntBuffer().put(pixels);
                return image;
            } catch (RuntimeException | Error exception) {
                image.close();
                throw exception;
            } finally {
                rendered.flush();
            }
        }, RASTER_THREAD).whenComplete((image, failure) -> {
            if (failure != null) {
                PENDING.remove(key);
                FAILURES.put(key, failure);
                LOGGER.error("Unable to rasterize Liquid Glass asset {}", asset.path, failure);
                return;
            }
            RenderSystem.queueFencedTask(() -> upload(asset, key, image));
        });
    }

    private static void upload(Asset asset, TextureKey key, NativeImage image) {
        try {
            Identifier id = Identifier.fromNamespaceAndPath("tidal",
                    "dynamic/liquid_glass/" + asset.path + "/" + key.width + "x"
                            + key.height + "_q" + key.quality
                            + (key.currentColor == -1 ? "" : "_c"
                            + Integer.toHexString(key.currentColor)));
            LinearTexture texture = new LinearTexture("Tidal Liquid Glass " + asset.path, image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            synchronized (TEXTURES) {
                TextureEntry replaced = TEXTURES.put(key, new TextureEntry(id, texture,
                        (long) key.width * key.height * 4));
                if (replaced != null) {
                    cacheBytes -= replaced.bytes;
                    Minecraft.getInstance().getTextureManager().release(replaced.identifier);
                }
                cacheBytes += (long) key.width * key.height * 4;
                evictIfNeeded();
            }
        } catch (RuntimeException exception) {
            image.close();
            FAILURES.put(key, exception);
            LOGGER.error("Unable to upload Liquid Glass asset {}", asset.path, exception);
        } finally {
            PENDING.remove(key);
        }
    }

    private static void evictIfNeeded() {
        TextureManager manager = Minecraft.getInstance().getTextureManager();
        var iterator = TEXTURES.entrySet().iterator();
        while (cacheBytes > CACHE_LIMIT_BYTES && iterator.hasNext()) {
            Map.Entry<TextureKey, TextureEntry> entry = iterator.next();
            cacheBytes -= entry.getValue().bytes;
            manager.release(entry.getValue().identifier);
            iterator.remove();
        }
    }

    private static Map<String, Asset> loadAssets() {
        try (InputStream stream = LiquidGlassAssets.class.getResourceAsStream(
                RESOURCE_ROOT + "manifest.json")) {
            if (stream == null) throw new IOException("Liquid Glass manifest is missing");
            JsonObject manifest = JsonParser.parseString(new String(stream.readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
            int declaredCount = manifest.get("count").getAsInt();
            var assets = manifest.getAsJsonArray("assets");
            if (assets.size() != declaredCount) {
                throw new IOException("Manifest declares " + declaredCount + " assets but lists "
                        + assets.size());
            }
            Map<String, Asset> loaded = new LinkedHashMap<>();
            for (JsonElement element : assets) {
                JsonObject json = element.getAsJsonObject();
                String file = json.get("file").getAsString();
                if (!file.endsWith(".svg")) throw new IOException("Invalid asset file " + file);
                String path = file.substring(0, file.length() - 4);
                int[] dimensions = pair(json.get("dimensions").getAsString());
                JsonElement sliceElement = json.get("nineSlice");
                Insets nineSlice = sliceElement != null && sliceElement.isJsonObject()
                        ? insets(sliceElement.getAsJsonObject()) : Insets.ZERO;
                Insets safeArea = insets(json.getAsJsonObject("safeArea"));
                Scaling scaling = Scaling.parse(json.get("scaling").getAsString());
                String svg;
                try (InputStream svgStream = LiquidGlassAssets.class.getResourceAsStream(
                        RESOURCE_ROOT + file)) {
                    if (svgStream == null) throw new IOException("Missing SVG " + file);
                    svg = new String(svgStream.readAllBytes(), StandardCharsets.UTF_8);
                }
                LiquidGlassSvg.Scene scene = LiquidGlassSvg.parse(svg, path);
                if (Math.abs(scene.width() - dimensions[0]) > 0.001
                        || Math.abs(scene.height() - dimensions[1]) > 0.001) {
                    throw new IOException("Manifest dimensions do not match SVG viewBox for "
                            + file);
                }
                if (safeArea.left() < 0 || safeArea.top() < 0
                        || safeArea.right() < 0 || safeArea.bottom() < 0
                        || safeArea.left() + safeArea.right() > dimensions[0]
                        || safeArea.top() + safeArea.bottom() > dimensions[1]) {
                    throw new IOException("Invalid safeArea in " + file);
                }
                if (nineSlice.left() + nineSlice.right() > dimensions[0]
                        || nineSlice.top() + nineSlice.bottom() > dimensions[1]) {
                    throw new IOException("Invalid nineSlice in " + file);
                }
                Asset asset = new Asset(path, dimensions[0], dimensions[1],
                        scaling, nineSlice, safeArea, json.get("tint").getAsBoolean(),
                        scene, false);
                if (loaded.put(path, asset) != null) {
                    throw new IOException("Duplicate Liquid Glass asset " + path);
                }
            }
            if (loaded.size() != declaredCount) {
                throw new IOException("Loaded " + loaded.size() + " assets; expected "
                        + declaredCount);
            }
            loadTidalAssets(loaded);
            LOGGER.info("Loaded and validated {} Liquid Glass SVG assets", loaded.size());
            return Collections.unmodifiableMap(loaded);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void loadTidalAssets(Map<String, Asset> loaded) throws IOException {
        JsonObject manifest;
        try (InputStream stream = LiquidGlassAssets.class.getResourceAsStream(
                TIDAL_ROOT + "manifest.json")) {
            if (stream == null) throw new IOException("Tidal asset manifest is missing");
            manifest = JsonParser.parseString(new String(stream.readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonObject counts = manifest.getAsJsonObject("counts");
        JsonObject uiAssets = manifest.getAsJsonObject("ui_assets");
        var modules = manifest.getAsJsonArray("modules");
        if (counts == null || uiAssets == null || modules == null
                || uiAssets.size() != counts.get("ui_assets").getAsInt()) {
            throw new IOException("Tidal asset manifest counts are invalid");
        }
        for (Map.Entry<String, JsonElement> entry : uiAssets.entrySet()) {
            String file = entry.getValue().getAsString();
            registerTidalAsset(loaded, "tidal/" + file.substring(0,
                    file.length() - 4), file);
        }
        int implementedCount = 0;
        for (JsonElement element : modules) {
            JsonObject module = element.getAsJsonObject();
            if (!module.get("implemented").getAsBoolean()) continue;
            implementedCount++;
            String id = module.get("id").getAsString();
            String file = module.get("icon").getAsString();
            if (!file.endsWith("/" + id + ".svg")) {
                throw new IOException("Tidal module icon does not match ID " + id);
            }
            registerTidalAsset(loaded, "tidal/" + file.substring(0,
                    file.length() - 4), file);
        }
        if (implementedCount != counts.get("total_implemented_modules").getAsInt()) {
            throw new IOException("Tidal manifest declares "
                    + counts.get("total_implemented_modules").getAsInt()
                    + " implemented modules but lists " + implementedCount);
        }
    }

    private static void registerTidalAsset(Map<String, Asset> loaded, String path,
                                               String file) throws IOException {
        if (!file.endsWith(".svg")) throw new IOException("Invalid Tidal SVG path " + file);
        String svg;
        try (InputStream stream = LiquidGlassAssets.class.getResourceAsStream(
                TIDAL_ROOT + file)) {
            if (stream == null) throw new IOException("Missing Tidal SVG " + file);
            svg = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        LiquidGlassSvg.Scene scene = LiquidGlassSvg.parse(svg, path);
        int width = (int) Math.round(scene.width());
        int height = (int) Math.round(scene.height());
        if (width <= 0 || height <= 0
                || Math.abs(scene.width() - width) > 0.001
                || Math.abs(scene.height() - height) > 0.001) {
            throw new IOException("Invalid Tidal SVG dimensions for " + file);
        }
        Insets nineSlice = Insets.ZERO;
        Insets safeArea = Insets.ZERO;
        Scaling scaling = Scaling.STRETCH;
        if (path.endsWith("polyglass_window_frame")) {
            scaling = Scaling.NINE_SLICE;
            nineSlice = new Insets(14, 14, 14, 14);
        } else if (path.endsWith("polyglass_module_card")
                || path.endsWith("polyglass_module_card_hover")) {
            scaling = Scaling.NINE_SLICE;
            nineSlice = new Insets(8, 8, 8, 8);
            safeArea = new Insets(14, 8, 8, 8);
        } else if (path.endsWith("polyglass_card")
                || path.endsWith("polyglass_card_hover")
                || path.endsWith("button_primary")
                || path.endsWith("button_primary_hover")
                || path.endsWith("button_primary_pressed")
                || path.endsWith("button_secondary")) {
            scaling = Scaling.NINE_SLICE;
            nineSlice = new Insets(8, 8, 8, 8);
        } else if (path.endsWith("nav_pill_active")) {
            scaling = Scaling.NINE_SLICE;
            nineSlice = new Insets(6, 6, 6, 6);
        } else if (path.endsWith("polyglass_hud_studio_drawer")) {
            scaling = Scaling.NINE_SLICE;
            nineSlice = new Insets(12, 12, 12, 12);
        }
        Asset asset = new Asset(path, width, height, scaling, nineSlice, safeArea,
                false, scene, scene.usesCurrentColor());
        if (loaded.put(path, asset) != null) {
            throw new IOException("Duplicate Tidal asset " + path);
        }
    }

    private static Insets insets(JsonObject json) {
        return new Insets(json.get("left").getAsInt(), json.get("top").getAsInt(),
                json.get("right").getAsInt(), json.get("bottom").getAsInt());
    }

    private static int[] pair(String text) throws IOException {
        String[] parts = text.split("x");
        if (parts.length != 2) throw new IOException("Invalid manifest dimensions " + text);
        return new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    enum Scaling {
        NINE_SLICE, NINE_SLICE_X, NINE_SLICE_Y, STRETCH, STRETCH_X, STRETCH_Y, FIXED;

        static Scaling parse(String value) throws IOException {
            return switch (value) {
                case "nine-slice" -> NINE_SLICE;
                case "nine-slice-x" -> NINE_SLICE_X;
                case "nine-slice-y" -> NINE_SLICE_Y;
                case "stretch" -> STRETCH;
                case "stretch-x" -> STRETCH_X;
                case "stretch-y" -> STRETCH_Y;
                case "fixed" -> FIXED;
                default -> throw new IOException("Unsupported scaling mode " + value);
            };
        }
    }

    record Insets(int left, int top, int right, int bottom) {
        private static final Insets ZERO = new Insets(0, 0, 0, 0);
    }

    record Asset(String path, int width, int height, Scaling scaling, Insets nineSlice,
                 Insets safeArea, boolean tint, LiquidGlassSvg.Scene scene,
                 boolean currentColorTint) {
        int scaledInsetX(int value, int deviceWidth) {
            return Math.round(value * deviceWidth / (float) width);
        }

        int scaledInsetY(int value, int deviceHeight) {
            return Math.round(value * deviceHeight / (float) height);
        }
    }

    private record TextureKey(String path, int width, int height, int quality,
                              int currentColor) {}

    private record TextureEntry(Identifier identifier, DynamicTexture texture, long bytes) {}

    private static final class LinearTexture extends DynamicTexture {
        private LinearTexture(String label, NativeImage pixels) {
            super(() -> label, pixels);
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
    }
}
