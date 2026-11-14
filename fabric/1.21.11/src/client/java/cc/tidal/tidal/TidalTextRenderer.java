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

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Device-pixel Noto Sans glyph atlas used by Tidal's extracted UI. */
final class TidalTextRenderer {
    private static final int FIRST = 32;
    private static final int LAST = 255;
    private static final int ATLAS_SIZE = 1024;
    private static final int PADDING = 2;
    private static final Map<Integer, Glyph> GLYPHS = new HashMap<>();
    private static final Map<Integer, String> MISSING_GLYPHS = new HashMap<>();
    private static final LinkedHashMap<String, Layout> LAYOUTS = new LinkedHashMap<>(256, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Layout> eldest) {
            return size() > 512;
        }
    };
    private static final ByteBufferHolder FONT = loadFont();
    private static STBTTFontinfo fontInfo;
    private static DynamicTexture texture;
    private static Identifier textureId;
    private static double bakedScaleX = -1;
    private static double bakedScaleY = -1;
    private static int pixelHeight;
    private static float fontScale;
    private static int ascent;
    private static boolean drawingShadow;

    private TidalTextRenderer() {}

    static void draw(GuiGraphics graphics, Font fallback, String text,
                     float x, int y, int color) {
        draw(graphics, fallback, text, x, y, color, false);
    }

    static void draw(GuiGraphics graphics, Font fallback, String text,
                     float x, int y, int color, boolean bold) {
        if (TidalConfig.uiTextShadow && !drawingShadow) {
            drawingShadow = true;
            try {
                draw(graphics, fallback, text, x + 1, y + 1,
                        TidalUi.withAlpha(0xFF000000, (color >>> 24) * 3 / 4), false);
            } finally {
                drawingShadow = false;
            }
        }
        color = TidalUi.withOpacity(color, TidalUi.frameAlpha());
        ensureBaked();
        Layout layout = layout(text);
        double scaleX = deviceScaleX();
        double scaleY = deviceScaleY();
        float cursor = x;
        for (int index = 0; index < layout.codePoints.length; index++) {
            int codePoint = layout.codePoints[index];
            Glyph glyph = GLYPHS.get(codePoint);
            if (glyph == null) {
                String missing = MISSING_GLYPHS.computeIfAbsent(
                        codePoint, value -> new String(Character.toChars(value)));
                int textX = Math.round(cursor);
                int textY = y;
                var pose = graphics.pose();
                pose.pushMatrix();
                pose.translate((float) (Math.round(cursor * scaleX) / scaleX - textX),
                        (float) (Math.round(y * scaleY) / scaleY - textY));
                graphics.drawString(fallback, missing, textX, textY, color, false);
                pose.popMatrix();
                cursor += layout.advances[index] / (float) scaleX;
                continue;
            }
            if (glyph.width > 0 && glyph.height > 0) {
                float deviceX = (float) (cursor * scaleX) + glyph.xOffset;
                float deviceY = (float) (y * scaleY) + ascent + glyph.yOffset;
                int left = Math.round(deviceX);
                int top = Math.round(deviceY);
                var pose = graphics.pose();
                pose.pushMatrix();
                pose.scale((float) (1.0 / scaleX), (float) (1.0 / scaleY));
                graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, left, top,
                        glyph.x, glyph.y, glyph.width, glyph.height, glyph.width,
                        glyph.height, ATLAS_SIZE, ATLAS_SIZE, color);
                if (bold) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, left + 1, top,
                            glyph.x, glyph.y, glyph.width, glyph.height, glyph.width,
                            glyph.height, ATLAS_SIZE, ATLAS_SIZE, color);
                }
                pose.popMatrix();
            }
            cursor += layout.advances[index] / (float) scaleX;
        }
    }

    static int width(String text) {
        ensureBaked();
        return Math.round(layout(text).width / (float) deviceScaleX());
    }

    static OffscreenText renderOffscreen(String text, int deviceScale) {
        if (deviceScale < 1 || deviceScale > 4) {
            throw new IllegalArgumentException("Invalid text device scale");
        }
        if (fontInfo == null) {
            fontInfo = STBTTFontinfo.malloc();
            if (!STBTruetype.stbtt_InitFont(fontInfo, FONT.bytes)) {
                throw new IllegalStateException("Could not initialize bundled Noto Sans with stb_truetype");
            }
        }
        int textPixelHeight = 9 * deviceScale;
        float textScale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, textPixelHeight);
        int[] ascentUnits = new int[1];
        STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascentUnits, new int[1], new int[1]);
        int baseline = Math.round(ascentUnits[0] * textScale);
        float measuredAdvance = 0;
        int previous = -1;
        for (int codePoint : text.codePoints().toArray()) {
            if (previous >= 0) measuredAdvance += STBTruetype.stbtt_GetCodepointKernAdvance(
                    fontInfo, previous, codePoint) * textScale;
            int[] advance = new int[1];
            STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, codePoint, advance, new int[1]);
            measuredAdvance += advance[0] * textScale;
            previous = codePoint;
        }
        int width = Math.max(1, (int) Math.ceil(measuredAdvance) + 1);
        BufferedImage output = new BufferedImage(width, textPixelHeight * 2,
                BufferedImage.TYPE_INT_ARGB_PRE);
        float penX = 0;
        previous = -1;
        for (int codePoint : text.codePoints().toArray()) {
            if (previous >= 0) penX += STBTruetype.stbtt_GetCodepointKernAdvance(
                    fontInfo, previous, codePoint) * textScale;
            int[] glyphWidth = new int[1], glyphHeight = new int[1];
            int[] xOffset = new int[1], yOffset = new int[1];
            var bitmap = STBTruetype.stbtt_GetCodepointBitmap(fontInfo, textScale,
                    textScale, codePoint, glyphWidth, glyphHeight, xOffset, yOffset);
            if (bitmap != null) {
                int startX = Math.round(penX) + xOffset[0];
                int startY = baseline + yOffset[0];
                for (int gy = 0; gy < glyphHeight[0]; gy++) {
                    for (int gx = 0; gx < glyphWidth[0]; gx++) {
                        int x = startX + gx, y = startY + gy;
                        if (x < 0 || x >= width || y < 0 || y >= output.getHeight()) continue;
                        int coverage = Byte.toUnsignedInt(bitmap.get(gy * glyphWidth[0] + gx));
                        int gamma = Math.round((float) (Math.pow(coverage / 255.0, 1.0 / 2.2) * 255));
                        output.setRGB(x, y, (gamma << 24) | 0x00FFFFFF);
                    }
                }
                STBTruetype.stbtt_FreeBitmap(bitmap);
            }
            int[] advance = new int[1];
            STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, codePoint, advance, new int[1]);
            penX += advance[0] * textScale;
            previous = codePoint;
        }
        return new OffscreenText(output, measuredAdvance, penX);
    }

    static String fit(Font fallback, String text, int maxWidth) {
        if (width(text) <= maxWidth) return text;
        String ellipsis = "…";
        if (width(ellipsis) > maxWidth) return "";
        StringBuilder result = new StringBuilder();
        int offset = 0;
        while (offset < text.length()) {
            int codePoint = text.codePointAt(offset);
            String next = new String(Character.toChars(codePoint));
            if (width(result + next + ellipsis) > maxWidth) break;
            result.append(next);
            offset += Character.charCount(codePoint);
        }
        return result.append(ellipsis).toString();
    }

    private static Layout layout(String text) {
        return LAYOUTS.computeIfAbsent(text, value -> {
            float advance = 0;
            int previous = -1;
            int[] codePoints = value.codePoints().toArray();
            float[] advances = new float[codePoints.length];
            for (int index = 0; index < codePoints.length; index++) {
                int codePoint = codePoints[index];
                Glyph glyph = GLYPHS.get(codePoint);
                if (glyph == null) {
                    advances[index] = Minecraft.getInstance().font.width(
                            new String(Character.toChars(codePoint))) * (float) deviceScaleX();
                    advance += advances[index];
                    previous = -1;
                    continue;
                }
                float kern = previous >= 0 ? STBTruetype.stbtt_GetCodepointKernAdvance(
                        fontInfo, previous, codePoint) * fontScale : 0;
                advances[index] = kern + glyph.advance;
                advance += advances[index];
                previous = codePoint;
            }
            return new Layout(codePoints, advances, advance);
        });
    }

    private static void ensureBaked() {
        ensureBaked(deviceScaleX(), deviceScaleY());
    }

    private static void ensureBaked(double scaleX, double scaleY) {
        if (Double.compare(scaleX, bakedScaleX) == 0
                && Double.compare(scaleY, bakedScaleY) == 0 && texture != null) return;
        if (textureId != null) Minecraft.getInstance().getTextureManager().release(textureId);
        bakedScaleX = scaleX;
        bakedScaleY = scaleY;
        pixelHeight = Math.max(1, (int) Math.ceil(9 * Math.max(scaleX, scaleY)));
        bakeAtlas();
        LAYOUTS.clear();
    }

    private static void bakeAtlas() {
        GLYPHS.clear();
        if (fontInfo == null) {
            fontInfo = STBTTFontinfo.malloc();
            if (!STBTruetype.stbtt_InitFont(fontInfo, FONT.bytes)) {
                throw new IllegalStateException("Could not initialize bundled Noto Sans with stb_truetype");
            }
        }
        fontScale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, pixelHeight);
        int[] ascentUnits = new int[1];
        int[] descentUnits = new int[1];
        STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascentUnits, descentUnits, new int[1]);
        ascent = Math.round(ascentUnits[0] * fontScale);
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, ATLAS_SIZE, ATLAS_SIZE, false);
        image.fillRect(0, 0, ATLAS_SIZE, ATLAS_SIZE, 0);
        int shelfX = PADDING;
        int shelfY = PADDING;
        int shelfHeight = 0;
        for (int codePoint = FIRST; codePoint <= LAST; codePoint++) {
            if (STBTruetype.stbtt_FindGlyphIndex(fontInfo, codePoint) == 0) continue;
            int[] width = new int[1], height = new int[1], xOffset = new int[1], yOffset = new int[1];
            var bitmap = STBTruetype.stbtt_GetCodepointBitmap(fontInfo, fontScale, fontScale,
                    codePoint, width, height, xOffset, yOffset);
            int[] advanceUnits = new int[1], bearing = new int[1];
            STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, codePoint, advanceUnits, bearing);
            float advance = advanceUnits[0] * fontScale;
            if (width[0] == 0 || height[0] == 0) {
                GLYPHS.put(codePoint, new Glyph(0, 0, 0, 0, xOffset[0], yOffset[0], advance, 255, 255));
                continue;
            }
            if (shelfX + width[0] + PADDING >= ATLAS_SIZE) {
                shelfX = PADDING;
                shelfY += shelfHeight + PADDING;
                shelfHeight = 0;
            }
            if (shelfY + height[0] + PADDING >= ATLAS_SIZE) {
                STBTruetype.stbtt_FreeBitmap(bitmap);
                image.close();
                throw new IllegalStateException("Noto Sans glyph atlas overflow at " + pixelHeight + "px");
            }
            int coverageMin = 255;
            int coverageMax = 0;
            for (int gy = 0; gy < height[0]; gy++) {
                for (int gx = 0; gx < width[0]; gx++) {
                    int coverage = Byte.toUnsignedInt(bitmap.get(gy * width[0] + gx));
                    coverageMin = Math.min(coverageMin, coverage);
                    coverageMax = Math.max(coverageMax, coverage);
                    int gamma = Math.round((float) (Math.pow(coverage / 255.0, 1.0 / 2.2) * 255));
                    image.setPixel(shelfX + gx, shelfY + gy,
                            ARGB.toABGR((gamma << 24) | 0x00FFFFFF));
                }
            }
            STBTruetype.stbtt_FreeBitmap(bitmap);
            GLYPHS.put(codePoint, new Glyph(shelfX, shelfY, width[0], height[0],
                    xOffset[0], yOffset[0], advance, coverageMin, coverageMax));
            shelfX += width[0] + PADDING;
            shelfHeight = Math.max(shelfHeight, height[0]);
        }
        Texture textureHandle = new Texture(image);
        textureId = Identifier.fromNamespaceAndPath("tidal", "dynamic/ui_text/" + pixelHeight);
        Minecraft.getInstance().getTextureManager().register(textureId, textureHandle);
        texture = textureHandle;
    }

    private static double deviceScaleX() { return LiquidGlassAssets.deviceScaleX(); }
    private static double deviceScaleY() { return LiquidGlassAssets.deviceScaleY(); }

    private static ByteBufferHolder loadFont() {
        try (var stream = TidalTextRenderer.class.getResourceAsStream(
                "/assets/tidal/font/noto_sans.ttf")) {
            if (stream == null) throw new IOException("Bundled Noto Sans font is missing");
            byte[] bytes = stream.readAllBytes();
            var buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return new ByteBufferHolder(buffer);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private record ByteBufferHolder(java.nio.ByteBuffer bytes) {}
    record OffscreenText(BufferedImage image, float measuredAdvance, float renderedAdvance) {}
    private record Glyph(int x, int y, int width, int height, int xOffset, int yOffset,
                         float advance, int coverageMin, int coverageMax) {}
    private record Layout(int[] codePoints, float[] advances, float width) {}

    private static final class Texture extends DynamicTexture {
        Texture(NativeImage image) {
            super(() -> "Tidal Noto Sans stb atlas", image);
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        }
    }
}
