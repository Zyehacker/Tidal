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

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TidalSoundEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("Tidal Sounds");

    public static final Identifier UI_CLICK_ID = id("ui.click");
    public static final Identifier UI_NOTIFICATION_ID = id("ui.notification");
    public static final Identifier UI_AMBIENT_ID = id("ui.ambient");
    public static final SoundEvent UI_CLICK = SoundEvent.createVariableRangeEvent(UI_CLICK_ID);
    public static final SoundEvent UI_NOTIFICATION =
            SoundEvent.createVariableRangeEvent(UI_NOTIFICATION_ID);
    public static final SoundEvent UI_AMBIENT = SoundEvent.createVariableRangeEvent(UI_AMBIENT_ID);

    private static boolean registered;

    private TidalSoundEvents() {}

    public static void register() {
        if (registered) return;
        Registry.register(BuiltInRegistries.SOUND_EVENT, UI_CLICK_ID, UI_CLICK);
        Registry.register(BuiltInRegistries.SOUND_EVENT, UI_NOTIFICATION_ID, UI_NOTIFICATION);
        Registry.register(BuiltInRegistries.SOUND_EVENT, UI_AMBIENT_ID, UI_AMBIENT);
        registered = true;
        LOGGER.info("Registered Tidal UI click, notification, and ambient sound events");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("tidal", path);
    }
}
