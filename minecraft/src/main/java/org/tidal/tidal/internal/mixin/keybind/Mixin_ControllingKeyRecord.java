package cc.tidal.tidal.internal.mixin.keybind;

import net.minecraft.client.Options;
//? if >=1.21.10 {
import net.minecraft.client.input.KeyEvent;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import cc.tidal.tidal.internal.ui.keybind.TidalKeybindRecorder;

@Pseudo
@Mixin(targets = "com.blamejared.controlling.platform.IPlatformHelper")
public interface Mixin_ControllingKeyRecord {
    //? if >=1.21.10 {
    @Inject(method = "handleKeyPress", at = @At("HEAD"), cancellable = true, remap = false)
    private void tidal$recordKeys(@Coerce Object screen, Options options, KeyEvent event, CallbackInfo ci) {
        if (!(screen instanceof TidalKeybindRecorder rec) || !rec.tidal$isOurs()) return;
        if (event.isEscape()) rec.tidal$recordEscape();
        else rec.tidal$recordKey(event.key());
        ci.cancel();
    }
    //?}
}
