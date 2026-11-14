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

import cc.tidal.tidal.TidalZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class TidalZoomSensitivityMixin {
    @ModifyVariable(method = "turn", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double tidal$scaleYawSensitivity(double amount) {
        if ((Object) this != Minecraft.getInstance().player) return amount;
        return amount * TidalZoom.mouseSensitivityScale();
    }

    @ModifyVariable(method = "turn", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double tidal$scalePitchSensitivity(double amount) {
        if ((Object) this != Minecraft.getInstance().player) return amount;
        return amount * TidalZoom.mouseSensitivityScale();
    }
}
