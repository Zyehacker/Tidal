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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class TidalCameraZoomMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void tidal$applyZoom(CameraRenderState state, float partialTick,
                                 CallbackInfo callback) {
        TidalModule zoom = TidalModules.byId("zoom");
        if (zoom == null || !zoom.enabled || state.projectionMatrix == null) return;
        float fieldOfViewScale = Math.max(0.1f, Math.min(1.0f, zoom.intensity / 100.0f));
        float projectionScale = 1.0f / fieldOfViewScale;
        state.projectionMatrix.scale(projectionScale, projectionScale, 1.0f);
        state.hudFov *= fieldOfViewScale;
    }
}
