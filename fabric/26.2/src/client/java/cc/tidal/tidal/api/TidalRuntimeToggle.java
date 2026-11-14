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

package cc.tidal.tidal.api;

/**
 * Cooperative master-toggle contract for Fabric client mods shown in Tidal's Mods page.
 * Register an implementation with the {@code tidal:toggle} entrypoint.
 *
 * <p>Implementations must quiesce their own features safely on the client thread when
 * disabled, release owned resources where possible, and restore them when enabled.
 * Fabric does not support unloading an initialized mod or removing its mixins at runtime.
 */
public interface TidalRuntimeToggle {
    boolean isEnabled();

    void setEnabled(boolean enabled);
}
