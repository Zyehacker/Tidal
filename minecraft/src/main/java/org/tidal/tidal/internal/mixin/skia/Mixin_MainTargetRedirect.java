package cc.tidal.tidal.internal.mixin.skia;

//? if < 1.21.8 {
/*import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import cc.tidal.tidal.internal.ui.hud.GuiTargetRedirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class Mixin_MainTargetRedirect {
    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void tidal$redirectMainTarget(CallbackInfoReturnable<RenderTarget> cir) {
        RenderTarget override = GuiTargetRedirect.INSTANCE.target;
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
*///? }
