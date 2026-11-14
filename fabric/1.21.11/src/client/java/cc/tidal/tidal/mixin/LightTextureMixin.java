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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Inject(method = "getBrightness(FI)F", at = @At("RETURN"), cancellable = true)
    private static void tidal$applyFullbright(float ambientLight, int lightLevel,
                                               CallbackInfoReturnable<Float> callback) {
        TidalModule fullbright = TidalModules.fullbright();
        Minecraft client = Minecraft.getInstance();
        if (fullbright == null || !fullbright.enabled || client.level == null
                || client.player == null) return;

        float strength = Math.max(0, Math.min(100, fullbright.intensity)) / 100.0f;
        float brightness = callback.getReturnValue();
        callback.setReturnValue(brightness + (1.0f - brightness) * strength);
    }
}
