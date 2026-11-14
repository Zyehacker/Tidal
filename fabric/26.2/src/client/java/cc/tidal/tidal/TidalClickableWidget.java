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

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

abstract class TidalClickableWidget extends AbstractWidget {
    enum SoundMode {
        TIDAL, VANILLA
    }

    private final SoundMode soundMode;
    private boolean clickPlayedOnPress;

    TidalClickableWidget(int x, int y, int width, int height, Component message) {
        this(x, y, width, height, message, SoundMode.TIDAL);
    }

    TidalClickableWidget(int x, int y, int width, int height, Component message,
                         SoundMode soundMode) {
        super(x, y, width, height, message);
        this.soundMode = soundMode;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        clickPlayedOnPress = false;
        if (soundMode == SoundMode.VANILLA) {
            super.playDownSound(soundManager);
        } else if (active && clickOnPress()) {
            clickPlayedOnPress = true;
            TidalSounds.playClick(this);
        }
    }

    @Override
    public final void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!active) return;
        if (soundMode == SoundMode.TIDAL && !clickPlayedOnPress) {
            TidalSounds.playClick(this);
        }
        clickPlayedOnPress = false;
        onActivated(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        try {
            return super.mouseReleased(event);
        } finally {
            clickPlayedOnPress = false;
        }
    }

    final void resetPressSound() {
        clickPlayedOnPress = false;
    }

    protected boolean clickOnPress() {
        return false;
    }

    protected abstract void onActivated(MouseButtonEvent event, boolean doubleClick);
}
