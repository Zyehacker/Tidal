package cc.tidal.tidal.internal.ui.keybind;

/**
 * Implemented by the keybinds-screen mixin so an external recorder such as the Controlling key
 * handler can push keys into the same in-progress combo recording
 * <p>
 * Only keys need this since mouse and release polling and commit go through the vanilla screen
 */
public interface TidalKeybindRecorder {
    boolean tidal$isOurs();

    void tidal$recordKey(int keyCode);

    void tidal$recordEscape();
}
