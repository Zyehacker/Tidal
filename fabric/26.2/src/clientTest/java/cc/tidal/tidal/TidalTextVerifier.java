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

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

final class TidalTextVerifier {
    private static final List<String> SAMPLES = List.of(
            "Tidal 26.2", "Smooth text: AVATAR", "Search modules");

    private TidalTextVerifier() {}

    static void verify() {
        ByteBuffer font = loadFont();
        STBTTFontinfo info = STBTTFontinfo.malloc();
        try {
            require(STBTruetype.stbtt_InitFont(info, font), "stb_truetype could not read Noto Sans");
            for (int scale = 1; scale <= 4; scale++) {
                for (String sample : SAMPLES) verifySample(info, scale, sample);
            }
        } finally {
            info.free();
            MemoryUtil.memFree(font);
        }
        verifyProductionRasterPath();
    }

    private static void verifyProductionRasterPath() {
        for (int scale = 1; scale <= 4; scale++) {
            for (String sample : SAMPLES) {
                TidalTextRenderer.OffscreenText first =
                        TidalTextRenderer.renderOffscreen(sample, scale);
                TidalTextRenderer.OffscreenText second =
                        TidalTextRenderer.renderOffscreen(sample, scale);
                BufferedImage image = first.image();
                BufferedImage repeat = second.image();
                int width = image.getWidth(), height = image.getHeight();
                require(Arrays.equals(image.getRGB(0, 0, width, height, null, 0, width),
                                repeat.getRGB(0, 0, width, height, null, 0, width)),
                        "Production STB raster is not deterministic at scale " + scale);
                require(Math.abs(first.measuredAdvance() - first.renderedAdvance()) < 0.001f,
                        "Production STB advances do not match font metrics");
                image.flush();
                repeat.flush();
            }
        }
    }

    private static void verifySample(STBTTFontinfo info, int deviceScale, String sample) {
        int pixelHeight = 9 * deviceScale;
        float fontScale = STBTruetype.stbtt_ScaleForPixelHeight(info, pixelHeight);
        int[] ascentUnits = new int[1];
        STBTruetype.stbtt_GetFontVMetrics(info, ascentUnits, new int[1], new int[1]);
        int baseline = Math.round(ascentUnits[0] * fontScale);
        float expectedAdvance = measure(info, sample, fontScale);
        int width = Math.max(1, (int) Math.ceil(expectedAdvance) + 1);
        Rendered first = render(info, sample, fontScale, baseline, width, pixelHeight * 2);
        Rendered second = render(info, sample, fontScale, baseline, width, pixelHeight * 2);
        require(Arrays.equals(first.image.getRGB(0, 0, width, pixelHeight * 2, null, 0, width),
                        second.image.getRGB(0, 0, width, pixelHeight * 2, null, 0, width)),
                "Text layout changed between runs at scale " + deviceScale + ": " + sample);
        boolean antialiased = false;
        for (int y = 0; y < first.image.getHeight(); y++) {
            for (int x = 0; x < first.image.getWidth(); x++) {
                int pixel = first.image.getRGB(x, y);
                int alpha = pixel >>> 24;
                if (alpha > 0 && alpha < 255) antialiased = true;
                require((pixel & 0x00FFFFFF) == 0x00FFFFFF || alpha == 0,
                        "Unexpected shadow/color pixel in text output");
            }
        }
        require(antialiased, "No antialiased glyph edge at scale " + deviceScale);
        require(Math.abs(first.advance - expectedAdvance) < 0.001f
                        && Math.abs(second.advance - expectedAdvance) < 0.001f,
                "Glyph advances differ from stb font metrics");
        first.image.flush();
        second.image.flush();
    }

    private static float measure(STBTTFontinfo info, String text, float scale) {
        float width = 0;
        int previous = -1;
        for (int codePoint : text.codePoints().toArray()) {
            if (previous >= 0) width += STBTruetype.stbtt_GetCodepointKernAdvance(
                    info, previous, codePoint) * scale;
            int[] advance = new int[1];
            STBTruetype.stbtt_GetCodepointHMetrics(info, codePoint, advance, new int[1]);
            width += advance[0] * scale;
            previous = codePoint;
        }
        return width;
    }

    private static Rendered render(STBTTFontinfo info, String text, float scale,
                                   int baseline, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        float penX = 0;
        int previous = -1;
        for (int codePoint : text.codePoints().toArray()) {
            if (previous >= 0) penX += STBTruetype.stbtt_GetCodepointKernAdvance(
                    info, previous, codePoint) * scale;
            int[] glyphWidth = new int[1], glyphHeight = new int[1];
            int[] xOffset = new int[1], yOffset = new int[1];
            ByteBuffer bitmap = STBTruetype.stbtt_GetCodepointBitmap(info, scale, scale,
                    codePoint, glyphWidth, glyphHeight, xOffset, yOffset);
            if (bitmap != null) {
                int startX = Math.round(penX) + xOffset[0];
                int startY = baseline + yOffset[0];
                for (int gy = 0; gy < glyphHeight[0]; gy++) {
                    for (int gx = 0; gx < glyphWidth[0]; gx++) {
                        int x = startX + gx, y = startY + gy;
                        if (x < 0 || x >= width || y < 0 || y >= height) continue;
                        int coverage = Byte.toUnsignedInt(bitmap.get(gy * glyphWidth[0] + gx));
                        int alpha = Math.round((float) (Math.pow(coverage / 255.0, 1.0 / 2.2) * 255));
                        image.setRGB(x, y, (alpha << 24) | 0x00FFFFFF);
                    }
                }
                STBTruetype.stbtt_FreeBitmap(bitmap);
            }
            int[] advance = new int[1];
            STBTruetype.stbtt_GetCodepointHMetrics(info, codePoint, advance, new int[1]);
            penX += advance[0] * scale;
            previous = codePoint;
        }
        return new Rendered(image, penX);
    }

    private static ByteBuffer loadFont() {
        try (var stream = TidalTextVerifier.class.getResourceAsStream(
                "/assets/tidal/font/noto_sans.ttf")) {
            if (stream == null) throw new IOException("Noto Sans resource missing");
            byte[] bytes = stream.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Noto Sans for offscreen test", exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private record Rendered(BufferedImage image, float advance) {}
}
