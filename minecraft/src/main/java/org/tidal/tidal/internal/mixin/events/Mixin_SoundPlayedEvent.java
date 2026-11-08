package cc.tidal.tidal.internal.mixin.events;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import cc.tidal.tidal.api.event.v1.EventManager;
import cc.tidal.tidal.api.event.v1.events.SoundPlayedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SoundManager.class)
public class Mixin_SoundPlayedEvent {
    @ModifyVariable(
            method = "play",
            at = @At("HEAD"),
            argsOnly = true
    )
    private SoundInstance onPlaySoundCallback(SoundInstance value) {
        if (value != null && value.getSound() != null) {
            String name = value.getSound().getLocation().toString();
            SoundPlayedEvent event = new SoundPlayedEvent(name, null, value);
            EventManager.INSTANCE.post(event);

            Object modified = event.getSound();
            if (modified instanceof SoundInstance) {
                return (SoundInstance) modified;
            }
        }

        return value;
    }

}
