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

import java.util.Set;

public final class TidalModule {
    public enum Status {
        IMPLEMENTED, PARTIAL, PLANNED
    }

    public enum Anchor {
        TOP_LEFT(0, 0), TOP_CENTER(1, 0), TOP_RIGHT(2, 0),
        CENTER_LEFT(0, 1), CENTER(1, 1), CENTER_RIGHT(2, 1),
        BOTTOM_LEFT(0, 2), BOTTOM_CENTER(1, 2), BOTTOM_RIGHT(2, 2);

        final int horizontal;
        final int vertical;

        Anchor(int horizontal, int vertical) {
            this.horizontal = horizontal;
            this.vertical = vertical;
        }
    }

    public enum HudStyle {
        MINIMAL, SUBTLE, ROUNDED, BORDERED, TRANSPARENT, SQUARE
    }

    public enum Group {
        HUD, COMBAT, PLAYER, WORLD, PERFORMANCE, MOVEMENT, VISUAL, INVENTORY, CONVENIENCE
    }

    public final String id;
    public final String name;
    public final String description;
    public final Group group;
    public final boolean implemented;
    public final Status status;
    public boolean enabled;
    public boolean favorite;
    public boolean hidden;
    public int keyCode;
    public int x = 12;
    public int y = 12;
    public float normalizedX;
    public float normalizedY;
    public boolean normalizedPosition;
    public float scale = 1.0f;
    public Anchor anchor = Anchor.TOP_LEFT;
    public boolean locked;
    public HudStyle hudStyle = HudStyle.ROUNDED;
    public int textColor = TidalTheme.PRIMARY;
    public int accentColor = TidalTheme.ACCENT;
    public int backgroundColor = TidalTheme.SURFACE;
    public int backgroundOpacity = 86;
    public int surfaceGroupId;
    public int intensity = 100;
    private static final Set<String> PARTIAL_FEATURES = Set.of(
            "armor_status", "armor_durability", "hunger", "saturation",
            "potion_effects");

    TidalModule(String id, String name, Group group, boolean implemented) {
        this.id = id;
        this.name = name;
        this.description = switch (id) {
            case "fps" -> "Shows the current client frame rate.";
            case "frame_time" -> "Shows the measured time between rendered frames.";
            case "ping" -> "Shows server latency; hidden in single-player.";
            case "memory_usage" -> "Shows used and maximum Java heap memory.";
            case "fullbright" -> "Brightens the 26.2 lightmap without changing saved gamma; intensity is configurable.";
            case "custom_crosshair" -> "Replaces the vanilla crosshair with a configurable client-side reticle.";
            case "view_bobbing" -> "Temporarily disables vanilla view bobbing while enabled.";
            case "server_address" -> "Shows the connected multiplayer server address.";
            case "resource_pack_display" -> "Shows the names of currently selected resource packs.";
            case "player_count" -> "Shows the number of online players reported by the server.";
            case "session_timer" -> "Shows elapsed time in the current world session.";
            case "coordinates" -> "Shows the player's current X, Y, and Z position.";
            case "direction" -> "Shows the cardinal direction the player is facing.";
            case "yaw_pitch" -> "Shows the player's camera yaw and pitch.";
            case "compass" -> "Shows the player's current compass heading.";
            case "dimension" -> "Shows the current dimension identifier.";
            case "movement_speed" -> "Shows horizontal movement speed sampled from player positions.";
            case "biome" -> "Shows the biome at the player's current position.";
            case "in_game_clock" -> "Shows the current Minecraft world time.";
            case "real_world_clock" -> "Shows the local system time.";
            case "day_counter" -> "Shows the current Minecraft world day.";
            case "health" -> "Shows current and maximum player health.";
            case "absorption" -> "Shows the player's current absorption health.";
            case "hunger" -> "Shows the player's current hunger level.";
            case "saturation" -> "Shows the player's current saturation level.";
            case "combined_hunger" -> "Shows hunger and saturation together.";
            case "xp_level" -> "Shows the player's experience level and progress to the next level.";
            case "armor_status" -> "Shows the player's total equipped armor points.";
            case "armor_durability" -> "Shows the lowest remaining durability among worn armor pieces.";
            case "potion_effects" -> "Shows the number of active status effects.";
            case "potion_timers" -> "Shows active effects and remaining time.";
            case "held_item" -> "Shows the selected hotbar item and stack count.";
            case "item_durability" -> "Shows remaining durability for the held damageable item.";
            case "totem_counter" -> "Counts totems in the main inventory and offhand.";
            case "arrow_counter" -> "Counts arrows, spectral arrows, and tipped arrows in inventory.";
            case "ender_pearl_counter" -> "Counts ender pearls in the player's inventory.";
            case "golden_apple_counter" -> "Counts regular and enchanted golden apples in inventory.";
            case "cps" -> "Shows left and right clicks per second.";
            case "keystrokes" -> "Shows live movement keys, mouse buttons, and click rate.";
            case "toggle_sprint" -> "Holds the sprint input while enabled; movement rules still apply.";
            case "toggle_sneak" -> "Holds the sneak input while enabled.";
            case "zoom" -> "Hold the keybind to zoom; adjust level, smoothness, and scroll behavior.";
            case "hud_layout_profiles" -> "HUD layout presets are available in the HUD editor; this catalogue entry is not independently toggleable.";
            default -> "Coming soon.";
        };
        this.group = group;
        this.implemented = implemented;
        this.status = !implemented ? Status.PLANNED
                : PARTIAL_FEATURES.contains(id) ? Status.PARTIAL : Status.IMPLEMENTED;
    }
}
