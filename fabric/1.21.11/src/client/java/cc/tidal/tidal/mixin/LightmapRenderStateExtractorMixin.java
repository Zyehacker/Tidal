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
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
    @Shadow
    private boolean needsUpdate;

    @org.spongepowered.asm.mixin.Unique
    private boolean tidal$wasEnabled;

    @org.spongepowered.asm.mixin.Unique
    private int tidal$lastIntensity = -1;

    @org.spongepowered.asm.mixin.Unique
    private Object tidal$lastLevel;

    @Inject(method = "extract", at = @At("TAIL"))
    private void tidal$applyFullbright(LightmapRenderState state, float partialTick,
                                       CallbackInfo callback) {
        TidalModule fullbright = TidalModules.fullbright();
        var client = net.minecraft.client.Minecraft.getInstance();
        Object level = client.level;
        boolean enabled = fullbright != null && fullbright.enabled
                && level != null && client.player != null;
        boolean levelChanged = level != tidal$lastLevel;
        tidal$lastLevel = level;
        if (!enabled) {
            if (tidal$wasEnabled) {
                needsUpdate = true;
                state.needsUpdate = true;
            }
            tidal$wasEnabled = false;
            tidal$lastIntensity = -1;
            return;
        }

        int intensity = Math.max(0, Math.min(100, fullbright.intensity));
        if (!tidal$wasEnabled || intensity != tidal$lastIntensity || levelChanged) {
            needsUpdate = true;
            state.needsUpdate = true;
        }
        tidal$wasEnabled = true;
        tidal$lastIntensity = intensity;
        if (!state.needsUpdate) return;

        float strength = intensity / 100.0f;
        state.brightness += (1.0f - state.brightness) * strength;
        state.blockFactor += (1.0f - state.blockFactor) * strength;
        state.skyFactor += (1.0f - state.skyFactor) * strength;
        state.darknessEffectScale *= 1.0f - strength;
        state.bossOverlayWorldDarkening *= 1.0f - strength;
        state.blockLightTint = brighten(state.blockLightTint, strength);
        state.skyLightColor = brighten(state.skyLightColor, strength);
        state.ambientColor = brighten(state.ambientColor, strength);
    }

    @org.spongepowered.asm.mixin.Unique
    private static Vector3f brighten(org.joml.Vector3fc color, float strength) {
        return new Vector3f(color).lerp(new Vector3f(1.0f), strength);
    }
}
