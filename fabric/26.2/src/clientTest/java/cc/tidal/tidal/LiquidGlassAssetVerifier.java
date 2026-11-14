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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.ARGB;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LiquidGlassAssetVerifier {
    private LiquidGlassAssetVerifier() {}

    public static void main(String[] args) throws Exception {
        List<LiquidGlassAssets.Asset> assets = LiquidGlassAssets.all();
        require(assets.size() == 178, "Expected 178 registered assets; found " + assets.size());
        long iconCount = assets.stream().filter(asset -> asset.path().startsWith("icons/")).count();
        require(iconCount == 72, "Expected 72 legacy icons; found " + iconCount);
        require(assets.stream().filter(asset -> asset.path().startsWith("tidal/")).count()
                        == 83,
                "Expected 83 Tidal assets");
        verifyTidalRegistry(assets);
        verifyUiAssetAliases();
        verifyTitleScreenAssets();

        for (LiquidGlassAssets.Asset asset : assets) {
            require(asset.width() > 0 && asset.height() > 0,
                    "Invalid dimensions for " + asset.path());
            BufferedImage image = asset.scene().rasterize(asset.width(), asset.height());
            require(image.getWidth() == asset.width() && image.getHeight() == asset.height(),
                    "Raster size mismatch for " + asset.path());
            image.flush();
            if (asset.path().startsWith("icons/")) verifyIcon(asset);
        }

        verifyAntialiasedCorners("components/card_default");
        verifyAntialiasedCorners("components/button_default");
        verifyAntialiasedCorners("tidal/ui/polyglass_module_card");
        verifyQualityLevels("components/card_default");
        verifyDeviceScaleEdges("components/card_default");
        verifyDevicePixelSnapping();
        TidalTextVerifier.verify();
        verifyNativePixelPacking();
        verifyNineSlice("components/button_default", 59, 21);
        verifyNineSlice("components/card_default", 240, 36);
        verifyNineSlice("components/card_default", 272, 52);
        verifyNineSlice("components/card_default", 420, 80);
        TidalModManagerVerifier.verify();

        System.out.println("Verified 178 SVG assets, 72 legacy icons, 40 Tidal module icons, "
                + "edge coverage at device scales 1-4, "
                + "device-pixel snapping, premultiplied pixel packing, nine-slice output, "
                + "STB text metrics/edge coverage/layout at scales 1-4, and cooperative mod "
                + "toggle/search/path handling.");
    }

    private static void verifyUiAssetAliases() {
        try {
            LiquidGlassAssets.require("controls/checkbox-unchecked-disabled");
            throw new IllegalStateException("Checkbox state must not alias to a toggle asset");
        } catch (IllegalArgumentException expected) {
            // Checkbox states are drawn as native square controls, not switch SVGs.
        }
        require(!LiquidGlassAssets.contains("icons/ui/not-a-real-icon"),
                "Missing icons must not fall back to a generic icon");
    }

    private static void verifyTitleScreenAssets() {
        for (String icon : List.of("icon_profile", "icon_close", "icon_features",
                "icon_cosmetics", "icon_options", "icon_language")) {
            LiquidGlassAssets.Asset asset = LiquidGlassAssets.require("icons/ui/" + icon);
            require(asset.width() == 24 && asset.height() == 24,
                    "Invalid title icon dimensions for " + icon);
        }
        require(LiquidGlassAssets.require("components/menu_button").width() == 400
                        && LiquidGlassAssets.require("components/menu_button_hover").width() == 400,
                "Title button surfaces must retain their 400px source width");
        require(LiquidGlassAssets.require("components/menu_icon_button").width() == 32
                        && LiquidGlassAssets.require("components/menu_icon_button_hover").width()
                        == 32,
                "Title icon button surfaces must retain their 32px source width");
        require(LiquidGlassAssets.require("components/menu_card").width() == 220
                        && LiquidGlassAssets.require("components/menu_card").height() == 94,
                "Title brand card dimensions must match its source asset");
        for (String path : List.of(
                "tidal/ui/polyglass_window_frame",
                "tidal/ui/polyglass_card",
                "tidal/ui/polyglass_card_hover",
                "tidal/ui/button_primary",
                "tidal/ui/button_primary_hover",
                "tidal/ui/button_primary_pressed",
                "tidal/ui/button_secondary",
                "tidal/ui/nav_pill_active",
                "tidal/ui/pref_appearance",
                "tidal/ui/pref_graphics",
                "tidal/ui/pref_typography",
                "tidal/ui/pref_input",
                "tidal/ui/pref_advanced",
                "tidal/actions/back",
                "tidal/actions/configure",
                "tidal/actions/copy",
                "tidal/actions/favorite_add",
                "tidal/actions/favorite_remove",
                "tidal/actions/folder_open",
                "tidal/actions/liquid_glass_gallery",
                "tidal/actions/project_page",
                "tidal/actions/restore_defaults",
                "tidal/modules/gameplay/fps_boost")) {
            require(LiquidGlassAssets.require(path).path().equals(path),
                    "Missing Clean Surface v4 asset " + path);
        }
        LiquidGlassAssets.Asset fpsBoost =
                LiquidGlassAssets.require("tidal/modules/gameplay/fps_boost");
        require(fpsBoost.width() == 64 && fpsBoost.height() == 64,
                "Unexpected FPS Boost icon dimensions");
        require(LiquidGlassAssets.require("surfaces/window-bg").path()
                        .equals("tidal/ui/polyglass_window_frame"),
                "Legacy window path does not resolve to the Tidal frame");
        require(LiquidGlassAssets.require("module-cards/module-card-normal").path()
                        .equals("tidal/ui/polyglass_module_card"),
                "Legacy card path does not resolve to the Tidal card");
        require(LiquidGlassAssets.require("toggles/toggle-off").path()
                        .equals("tidal/ui/toggle_off"),
                "Legacy toggle path does not resolve to the Tidal toggle");
        require(LiquidGlassAssets.require("buttons/nav-selected").path()
                        .equals("tidal/ui/polyglass_module_card_hover"),
                "Legacy navigation path does not resolve to the Tidal card");
        require(LiquidGlassAssets.require("inputs/field-normal").path()
                        .equals("components/field_default"),
                "Legacy input path does not resolve to a packaged field asset");
        LiquidGlassAssets.Asset radio = LiquidGlassAssets.require("controls/radio-selected");
        require(radio.path().equals("components/slider_thumb")
                        && radio.scaling() == LiquidGlassAssets.Scaling.STRETCH,
                "Small radio controls must resolve to a stretchable marker asset");
    }

    private static void verifyTidalRegistry(List<LiquidGlassAssets.Asset> assets)
            throws Exception {
        JsonObject manifest;
        try (InputStream stream = LiquidGlassAssetVerifier.class.getResourceAsStream(
                "/assets/tidal/textures/gui/liquid-glass/tidal/manifest.json")) {
            require(stream != null, "Tidal manifest was not packaged");
            manifest = JsonParser.parseString(new String(stream.readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
        }
        Set<String> packIds = new HashSet<>();
        for (var element : manifest.getAsJsonArray("modules")) {
            JsonObject entry = element.getAsJsonObject();
            if (!entry.get("implemented").getAsBoolean()) continue;
            String id = entry.get("id").getAsString();
            String type = entry.get("type").getAsString();
            String expectedIcon = "modules/" + type + "/" + id + ".svg";
            require(expectedIcon.equals(entry.get("icon").getAsString()),
                    "Manifest icon does not match module ID " + id);
            LiquidGlassAssets.Asset icon = LiquidGlassAssets.require(
                    "tidal/" + expectedIcon.substring(0, expectedIcon.length() - 4));
            require(icon.width() == 24 && icon.height() == 24,
                    "Unexpected icon dimensions for " + id);
            require(icon.currentColorTint() == icon.scene().usesCurrentColor(),
                    "Module icon currentColor metadata is inconsistent: " + id);
            require(packIds.add(id), "Duplicate implemented module ID " + id);
        }
        Set<String> registryIds = new HashSet<>();
        int placeholderCount = 0;
        for (TidalModule module : TidalModules.all()) {
            if (!module.implemented) {
                placeholderCount++;
                continue;
            }
            registryIds.add(module.id);
            require(LiquidGlassAssets.require(TidalUi.moduleIconPath(module)) != null,
                    "Missing registered module icon for " + module.id);
        }
        require(packIds.size() == 40 && packIds.equals(registryIds),
                "Implemented module registry does not match the 40 Tidal IDs");
        require(placeholderCount == 44,
                "Expected 44 hidden placeholder modules; found " + placeholderCount);
        require(manifest.getAsJsonObject("counts").get("total_implemented_modules").getAsInt()
                        == packIds.size(),
                "Tidal manifest implemented-module count is inconsistent");

        BufferedImage accented = LiquidGlassAssets.require(
                "tidal/modules/hud/cps").scene()
                .rasterize(24, 24, 1, 0xFF8E95A5);
        require(hasAccent(accented, 0),
                "The CPS icon's static blue accent was not preserved");
        accented.flush();
        BufferedImage statusIcon = LiquidGlassAssets.require(
                "tidal/modules/hud/xp_level").scene().rasterize(24, 24);
        require(hasAccent(statusIcon, 1),
                "Static success accent was not preserved in the XP icon");
        statusIcon.flush();

        LiquidGlassAssets.Asset health =
                LiquidGlassAssets.require("tidal/modules/hud/health");
        BufferedImage light = health.scene().rasterize(24, 24, 1, 0xFFF3F5F8);
        BufferedImage muted = health.scene().rasterize(24, 24, 1, 0xFF8E95A5);
        boolean colorChanged = false;
        for (int y = 0; y < 24 && !colorChanged; y++) {
            for (int x = 0; x < 24; x++) {
                if (light.getRGB(x, y) != muted.getRGB(x, y)) {
                    colorChanged = true;
                    break;
                }
            }
        }
        light.flush();
        muted.flush();
        require(colorChanged, "currentColor tint did not change module icon strokes");
        TidalTheme.apply(false, TidalTheme.DEFAULT_ACCENT);
        require(TidalTheme.BACKGROUND == 0xE011141C
                        && TidalTheme.SURFACE == 0xF00B0D12
                        && TidalTheme.ELEVATED == 0xFF181B24
                        && TidalTheme.INTERACTIVE == 0xFF232836
                        && TidalTheme.HOVER == 0xFF1E222E
                        && TidalTheme.BORDER == 0x14FFFFFF
                        && TidalTheme.ACCENT == 0xFF2B52FB
                        && TidalTheme.ACCENT_HOVER == 0xFF3B60FF
                        && TidalTheme.ACCENT_PRESSED == 0xFF2243D6,
                "Tidal Clean Surface v4 theme tokens are not applied");
        require(TidalTheme.Glass.SURFACE_OPACITY == 224
                        && TidalTheme.Glass.SIDEBAR_OPACITY == 240
                        && TidalTheme.Glass.BLUR_RADIUS > 0,
                "Frosted window and sidebar surfaces are not enabled");
        require(TidalTheme.Radius.WINDOW == 14 && TidalTheme.Radius.CARD == 8
                        && TidalTheme.Radius.PILL == 6 && TidalTheme.Radius.TOGGLE == 999,
                "PolyGlass Dark radii are not applied");
    }

    private static boolean hasAccent(BufferedImage image, int channel) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int red = (pixel >>> 16) & 255;
                int green = (pixel >>> 8) & 255;
                int blue = pixel & 255;
                if ((pixel >>> 24) > 0 && switch (channel) {
                    case 0 -> blue > red && blue > green;
                    case 1 -> green > red && green > blue;
                    default -> false;
                }) return true;
            }
        }
        return false;
    }

    private static void verifyDeviceScaleEdges(String path) {
        LiquidGlassAssets.Asset asset = LiquidGlassAssets.require(path);
        for (int scale = 1; scale <= 4; scale++) {
            int width = (int) Math.ceil(asset.width() * (double) scale);
            int height = (int) Math.ceil(asset.height() * (double) scale);
            BufferedImage image = asset.scene().rasterize(width, height);
            boolean partialAlpha = false;
            for (int y = 0; y < height && !partialAlpha; y++) {
                for (int x = 0; x < width; x++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    if (alpha > 0 && alpha < 255) {
                        partialAlpha = true;
                        break;
                    }
                }
            }
            image.flush();
            require(partialAlpha, "No partial-alpha edge at device scale " + scale
                    + " for " + path);
        }
    }

    private static void verifyDevicePixelSnapping() {
        for (int scale = 1; scale <= 4; scale++) {
            for (double coordinate : new double[] {-17.25, -0.5, 0.0, 0.125, 2.7, 99.99}) {
                int snapped = TidalUi.snapDevicePixel(coordinate, scale);
                require(snapped == Math.round(coordinate * scale),
                        "Device pixel snapping mismatch at scale " + scale);
            }
        }
        require(LiquidGlassAssets.resolveDeviceScale(0, 960, 2) == 2,
                "A minimized window must fall back to the configured GUI scale");
        require(LiquidGlassAssets.resolveDeviceScale(1920, 960, 2) == 2,
                "Framebuffer dimensions must determine the device scale");
        require(LiquidGlassAssets.resolveDeviceScale(0, 0, 0) == 1,
                "Invalid window dimensions must resolve to a safe device scale");
    }

    private static void verifyNativePixelPacking() {
        int argb = 0x80201008;
        int abgr = ARGB.toABGR(argb);
        require(abgr == 0x80081020, "ARGB-to-ABGR channel conversion failed");
        byte[] expected = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
                ? new byte[] {0x20, 0x10, 0x08, (byte) 0x80}
                : new byte[] {(byte) 0x80, 0x08, 0x10, 0x20};
        byte[] actual = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder())
                .putInt(abgr).array();
        require(Arrays.equals(expected, actual),
                "Bulk texture upload does not match native RGBA byte order");
    }

    private static void verifyIcon(LiquidGlassAssets.Asset asset) {
        require(asset.tint(), "Icon is not configured as a tintable white mask: " + asset.path());
        BufferedImage image = asset.scene().rasterize(asset.width(), asset.height());
        boolean visible = false;
        for (int y = 0; y < image.getHeight() && !visible; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                if ((pixel >>> 24) != 0) visible = true;
            }
        }
        image.flush();
        require(visible, "Icon SVG rasterized as transparent: " + asset.path());
    }

    private static void verifyAntialiasedCorners(String path) {
        LiquidGlassAssets.Asset asset = LiquidGlassAssets.require(path);
        int width = asset.width() * 4;
        int height = asset.height() * 4;
        BufferedImage image = asset.scene().rasterize(width, height);
        int edgeWidth = Math.min(width / 2, Math.max(8, asset.nineSlice().left() * 4));
        int edgeHeight = Math.min(height / 2, Math.max(8, asset.nineSlice().top() * 4));
        boolean partialAlpha = false;
        for (int y = 0; y < edgeHeight && !partialAlpha; y++) {
            for (int x = 0; x < edgeWidth; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha > 0 && alpha < 255) {
                    partialAlpha = true;
                    break;
                }
            }
        }
        image.flush();
        require(partialAlpha, "No antialiased corner pixels found in " + path + " at 4x");
    }

    private static void verifyQualityLevels(String path) {
        LiquidGlassAssets.Asset asset = LiquidGlassAssets.require(path);
        for (int quality : new int[] {1, 2, 4}) {
            BufferedImage image = asset.scene().rasterize(asset.width(), asset.height(), quality);
            require(image.getWidth() == asset.width() && image.getHeight() == asset.height(),
                    "Supersampled output dimensions changed at quality " + quality);
            boolean antialiased = false;
            for (int y = 0; y < Math.min(12, image.getHeight()) && !antialiased; y++) {
                for (int x = 0; x < Math.min(12, image.getWidth()); x++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    if (alpha > 0 && alpha < 255) {
                        antialiased = true;
                        break;
                    }
                }
            }
            image.flush();
            require(antialiased, "No edge coverage at raster quality " + quality);
        }
    }

    private static void verifyNineSlice(String path, int width, int height) {
        LiquidGlassAssets.Asset asset = LiquidGlassAssets.require(path);
        require(width >= asset.nineSlice().left() + asset.nineSlice().right()
                        && height >= asset.nineSlice().top() + asset.nineSlice().bottom(),
                "Nine-slice insets exceed target size for " + path + " at "
                        + width + "x" + height);
        for (int scale = 1; scale <= 4; scale++) {
            int sourceWidth = asset.width() * scale;
            int sourceHeight = asset.height() * scale;
            int targetWidth = width * scale;
            int targetHeight = height * scale;
            BufferedImage source = asset.scene().rasterize(sourceWidth, sourceHeight);
            BufferedImage output = new BufferedImage(targetWidth, targetHeight,
                    BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D graphics = output.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int left = asset.nineSlice().left() * scale;
            int right = asset.nineSlice().right() * scale;
            int top = asset.nineSlice().top() * scale;
            int bottom = asset.nineSlice().bottom() * scale;
            int[] sourceX = {0, left, sourceWidth - right, sourceWidth};
            int[] sourceY = {0, top, sourceHeight - bottom, sourceHeight};
            int[] targetX = {0, left, targetWidth - right, targetWidth};
            int[] targetY = {0, top, targetHeight - bottom, targetHeight};
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    graphics.drawImage(source, targetX[column], targetY[row],
                            targetX[column + 1], targetY[row + 1],
                            sourceX[column], sourceY[row],
                            sourceX[column + 1], sourceY[row + 1], null);
                }
            }
            graphics.dispose();

            int centerY = targetHeight / 2;
            for (int x = 0; x < targetWidth; x++) {
                require((output.getRGB(x, centerY) >>> 24) > 0,
                        "Nine-slice seam or gap at " + width + "x" + height
                                + ", scale=" + scale + ", x=" + x);
            }
            source.flush();
            output.flush();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
