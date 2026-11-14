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

import cc.tidal.tidal.TidalData;
import cc.tidal.tidal.TidalZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class TidalMouseInputMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void tidal$countHudClicks(long window, MouseButtonInfo button, int action,
                                      CallbackInfo callback) {
        Minecraft client = Minecraft.getInstance();
        if (action == 1 && client.player != null && client.gui.screen() == null) {
            TidalData.recordMouseClick(button.button());
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void tidal$adjustHeldZoom(long window, double horizontalAmount,
                                      double verticalAmount, CallbackInfo callback) {
        if (TidalZoom.interceptMouseScroll(verticalAmount)) callback.cancel();
    }
}
