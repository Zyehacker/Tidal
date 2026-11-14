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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Client-only replacement for the vanilla crosshair. */
final class TidalCrosshair {
    private TidalCrosshair() {}

    static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen != null
                || client.options.hideGui || !client.options.getCameraType().isFirstPerson()) {
            return;
        }

        int centerX = client.getWindow().getGuiScaledWidth() / 2;
        int centerY = client.getWindow().getGuiScaledHeight() / 2;
        int size = Math.max(2, Math.min(16, TidalConfig.crosshairSize));
        int gap = Math.max(0, Math.min(16, TidalConfig.crosshairGap));
        int thickness = Math.max(1, Math.min(8, TidalConfig.crosshairThickness));
        if (TidalConfig.dynamicCrosshair) {
            float cooldown = client.player.getAttackStrengthScale(0.0f);
            gap += Math.round((1.0f - cooldown) * 3.0f);
        }

        if (TidalConfig.crosshairShape == 1) {
            int diameter = Math.max(thickness, size * 2 + 1);
            int startX = centerX - diameter / 2;
            int startY = centerY - diameter / 2;
            if (TidalConfig.crosshairOutline) {
                graphics.fill(startX - 1, startY - 1, startX + diameter + 1,
                        startY + diameter + 1, 0xCC000000);
            }
            graphics.fill(startX, startY, startX + diameter, startY + diameter,
                    TidalConfig.crosshairColor);
            return;
        }

        if (TidalConfig.crosshairShape == 2) {
            int outer = Math.max(thickness * 2 + 2, size * 2 + gap * 2);
            int halfOuter = outer / 2;
            if (TidalConfig.crosshairOutline) {
                drawSquare(graphics, centerX, centerY, halfOuter + 1, thickness + 2,
                        0xCC000000);
            }
            drawSquare(graphics, centerX, centerY, halfOuter, thickness,
                    TidalConfig.crosshairColor);
            return;
        }

        int left = centerX - gap - size;
        int right = centerX + gap + size;
        int top = centerY - gap - size;
        int bottom = centerY + gap + size;
        int half = thickness / 2;
        if (TidalConfig.crosshairOutline) {
            int outline = 1;
            graphics.fill(left, centerY - half - outline, centerX - gap + outline,
                    centerY - half + thickness + outline, 0xCC000000);
            graphics.fill(centerX + gap - outline, centerY - half - outline,
                    right, centerY - half + thickness + outline, 0xCC000000);
            graphics.fill(centerX - half - outline, top, centerX - half + thickness + outline,
                    centerY - gap + outline, 0xCC000000);
            graphics.fill(centerX - half - outline, centerY + gap - outline,
                    centerX - half + thickness + outline, bottom, 0xCC000000);
        }
        graphics.fill(left, centerY - half, centerX - gap, centerY - half + thickness,
                TidalConfig.crosshairColor);
        graphics.fill(centerX + gap, centerY - half, right, centerY - half + thickness,
                TidalConfig.crosshairColor);
        graphics.fill(centerX - half, top, centerX - half + thickness, centerY - gap,
                TidalConfig.crosshairColor);
        graphics.fill(centerX - half, centerY + gap, centerX - half + thickness, bottom,
                TidalConfig.crosshairColor);
    }

    private static void drawSquare(GuiGraphics graphics, int centerX, int centerY,
                                   int halfSize, int thickness, int color) {
        int left = centerX - halfSize;
        int right = centerX + halfSize + 1;
        int top = centerY - halfSize;
        int bottom = centerY + halfSize + 1;
        graphics.fill(left, top, right, top + thickness, color);
        graphics.fill(left, bottom - thickness, right, bottom, color);
        graphics.fill(left, top + thickness, left + thickness, bottom - thickness, color);
        graphics.fill(right - thickness, top + thickness, right, bottom - thickness, color);
    }
}
