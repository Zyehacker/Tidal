package cc.tidal.tidal.internal.mixin.keybind;

import net.minecraft.client.Options;
import cc.tidal.tidal.internal.ui.keybind.MinecraftKeybindProfiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class Mixin_OptionsSaveDetect {
    @Inject(method = "save", at = @At("RETURN"))
    private void tidal$captureSavedControls(CallbackInfo ci) {
        MinecraftKeybindProfiles.onOptionsSaved();
    }
}
