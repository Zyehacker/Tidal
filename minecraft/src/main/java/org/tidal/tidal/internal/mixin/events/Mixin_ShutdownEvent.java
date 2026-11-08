package cc.tidal.tidal.internal.mixin.events;

import net.minecraft.client.Minecraft;
import cc.tidal.tidal.api.event.v1.EventManager;
import cc.tidal.tidal.api.event.v1.events.ShutdownEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_ShutdownEvent {

    @Inject(method = "stop", at = @At("HEAD"))
    private void shutdownCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(ShutdownEvent.INSTANCE);
    }

}
