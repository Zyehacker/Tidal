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

public final class TidalZoom {
    private static long lastFrameNanos;
    private static float fieldOfViewScale = 1.0f;

    private TidalZoom() {}

    public static float fieldOfViewScale() {
        TidalModule zoom = TidalModules.byId("zoom");
        float target = zoom != null && zoom.enabled && TidalKeybinds.isDown(zoom)
                ? Math.max(0.1f, Math.min(1.0f, zoom.intensity / 100.0f)) : 1.0f;
        long now = System.nanoTime();
        float frameSeconds = lastFrameNanos == 0 ? 0.0f
                : Math.min(0.05f, Math.max(0.0f,
                (now - lastFrameNanos) / 1_000_000_000.0f));
        lastFrameNanos = now;
        float smoothness = TidalConfig.zoomSmoothness / 100.0f;
        float response = 3.0f + (1.0f - smoothness) * 30.0f;
        float amount = 1.0f - (float) Math.exp(-response * frameSeconds);
        fieldOfViewScale += (target - fieldOfViewScale) * amount;
        if (Math.abs(target - fieldOfViewScale) < 0.0001f) {
            fieldOfViewScale = target;
        }
        return fieldOfViewScale;
    }

    static double adjustMouseScroll(double verticalAmount) {
        if (!TidalConfig.zoomScrollEnabled || verticalAmount == 0.0
                || !TidalModules.byId("zoom").enabled
                || !TidalKeybinds.isDown(TidalModules.byId("zoom"))) return 0.0;
        TidalModule zoom = TidalModules.byId("zoom");
        zoom.intensity = Math.max(10, Math.min(100,
                zoom.intensity + (verticalAmount > 0.0 ? -5 : 5)));
        TidalConfig.save();
        return verticalAmount;
    }

    public static boolean interceptMouseScroll(double verticalAmount) {
        return adjustMouseScroll(verticalAmount) != 0.0;
    }

    public static float mouseSensitivityScale() {
        Minecraft client = Minecraft.getInstance();
        return client.gui.screen() == null ? fieldOfViewScale : 1.0f;
    }
}
