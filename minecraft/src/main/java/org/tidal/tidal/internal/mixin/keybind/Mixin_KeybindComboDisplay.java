package cc.tidal.tidal.internal.mixin.keybind;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import cc.tidal.tidal.api.ui.v1.keybind.TidalKeybind;
import cc.tidal.tidal.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class Mixin_KeybindComboDisplay {
    @Inject(method = "getTranslatedKeyMessage", at = @At("RETURN"), cancellable = true)
    private void tidal$showCombo(CallbackInfoReturnable<Component> cir) {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null) return;
        KeyMapping self = (KeyMapping) (Object) this;
        TidalKeybind bind = bridge.bindFor(self);
        if (bind == null) return;
        // while recording show the live combo as keys are added
        Component preview = bridge.previewFor(self);
        if (preview != null) {
            cir.setReturnValue(preview);
            return;
        }
        Component decorated = MinecraftKeybindBridgeImpl.comboLabel(bind, cir.getReturnValue());
        if (decorated != null) cir.setReturnValue(decorated);
    }
}
