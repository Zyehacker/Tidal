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

package cc.tidal.tidal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TidalSounds {
    private static final Logger LOGGER = LoggerFactory.getLogger("Tidal Sounds");
    private static AmbientSound ambientSound;

    private TidalSounds() {}

    static void playClick(AbstractWidget widget) {
        String label = widget.getMessage().getString();
        String context = widget.getClass().getSimpleName()
                + (label.isBlank() ? "" : "[" + label + "]");
        playClick(context);
    }

    static void playClick(String widget) {
        LOGGER.debug("Tidal click: widget={} sound={} category={}",
                widget, TidalSoundEvents.UI_CLICK_ID, SoundSource.UI);
        playUiSound(TidalSoundEvents.UI_CLICK);
    }

    static void playNotification() {
        LOGGER.debug("Playing Tidal sound {} on {}", TidalSoundEvents.UI_NOTIFICATION_ID,
                SoundSource.UI);
        playUiSound(TidalSoundEvents.UI_NOTIFICATION);
    }

    static void startAmbient() {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        if (ambientSound != null && soundManager.isActive(ambientSound)) return;
        ambientSound = new AmbientSound();
        LOGGER.debug("Playing Tidal sound {} on {}", TidalSoundEvents.UI_AMBIENT_ID,
                SoundSource.MUSIC);
        soundManager.play(ambientSound);
    }

    static void stopAmbient() {
        if (ambientSound == null) return;
        SoundInstance sound = ambientSound;
        ambientSound = null;
        Minecraft.getInstance().getSoundManager().stop(sound);
    }

    private static void playUiSound(SoundEvent event) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0f));
    }

    private static final class AmbientSound extends AbstractTickableSoundInstance {
        private AmbientSound() {
            super(TidalSoundEvents.UI_AMBIENT, SoundSource.MUSIC, RandomSource.create());
            looping = true;
            volume = 0.35f;
            pitch = 1.0f;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {}
    }
}
