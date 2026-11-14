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

package cc.tidal.tidal.mixin;

import cc.tidal.tidal.TidalModule;
import cc.tidal.tidal.TidalModules;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class TidalCameraZoomMixin {
    @Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)F",
            at = @At("RETURN"), cancellable = true)
    private void tidal$applyZoom(Camera camera, float partialTick, boolean useFovSettings,
                                 CallbackInfoReturnable<Float> callback) {
        TidalModule zoom = TidalModules.byId("zoom");
        if (zoom == null || !zoom.enabled) return;
        double fieldOfViewScale = Math.max(0.1, Math.min(1.0, zoom.intensity / 100.0));
        double halfFov = Math.toRadians(callback.getReturnValue() * 0.5);
        callback.setReturnValue((float) Math.toDegrees(
                2.0 * Math.atan(Math.tan(halfFov) * fieldOfViewScale)));
    }
}
