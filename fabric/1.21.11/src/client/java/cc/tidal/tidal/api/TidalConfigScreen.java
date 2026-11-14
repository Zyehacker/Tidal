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

import net.minecraft.client.gui.screens.Screen;

/** Optional cooperative settings screen entrypoint registered as {@code tidal:config}. */
@FunctionalInterface
public interface TidalConfigScreen {
    Screen create(Screen parent);
}
