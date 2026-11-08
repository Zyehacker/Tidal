package cc.tidal.tidal.internal.mixin.skia;

import net.minecraft.client.Minecraft;
import cc.tidal.tidal.internal.ui.compose.SkiaFontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_InitSkiaFontRenderer {
    @Inject(method = "<init>", at = @At("TAIL"))
    void impl$__init__(CallbackInfo ci) {
        SkiaFontRenderer.INSTANCE.init();
    }
}
