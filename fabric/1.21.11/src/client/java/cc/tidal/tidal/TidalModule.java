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
    boolean wasDown;
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
            case "playtime_tracker" -> "Would show elapsed time in the current session; not implemented.";
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
            case "inventory_preview" -> "Would preview inventory contents without opening the inventory; not implemented.";
            case "item_counter" -> "Would count a user-selected inventory item; not implemented.";
            case "entity_counter" -> "Would count selected nearby entities within a configured range; not implemented.";
            case "fps_graph" -> "Would graph recent frame rates over time; not implemented.";
            case "cps" -> "Shows left and right clicks per second.";
            case "keystrokes" -> "Shows live movement keys, sprint, sneak, and mouse activity.";
            case "mouse_buttons" -> "Would show current mouse button activity; not implemented.";
            case "mouse_trail" -> "Would display a recent mouse movement trail; not implemented.";
            case "combo_counter" -> "Would count consecutive attacks within a timeout; not implemented.";
            case "last_hit_reach" -> "Would show measured distance of the last valid attack; not implemented.";
            case "block_placement_counter" -> "Would count blocks placed in the current session; not implemented.";
            case "jump_reset_indicator" -> "Would indicate jump-reset timing; not implemented.";
            case "toggle_sprint" -> "Holds the sprint input while enabled; movement rules still apply.";
            case "toggle_sneak" -> "Holds the sneak input while enabled.";
            case "zoom" -> "Narrows the world camera field of view while enabled.";
            case "custom_zoom" -> "Would zoom the camera while its configurable key is active; not implemented.";
            case "fov_adjustment" -> "Would adjust supported camera field-of-view changes; not implemented.";
            case "freelook" -> "Would rotate the local camera independently of player movement; not implemented.";
            case "perspective_toggle" -> "Would toggle between supported camera perspectives; not implemented.";
            case "auto_jump_toggle" -> "Would toggle Minecraft's automatic jump setting; not implemented.";
            case "sprint_key_customization" -> "Would customize sprint key behavior; not implemented.";
            case "input_display_settings" -> "Would configure the Tidal input display; not implemented.";
            case "crosshair_color" -> "Would change the crosshair color; not implemented.";
            case "crosshair_size" -> "Would change the crosshair size; not implemented.";
            case "crosshair_gap_thickness" -> "Would adjust crosshair gap and line thickness; not implemented.";
            case "camera_shake" -> "Would adjust supported camera damage shake; not implemented.";
            case "hand_animations" -> "Would customize first-person hand animations; not implemented.";
            case "weather_visibility" -> "Would adjust local weather rendering visibility; not implemented.";
            case "particle_density" -> "Would adjust the client's particle density; not implemented.";
            case "inventory_sorting" -> "Would sort inventory items without changing server contents; not implemented.";
            case "item_reference" -> "Would provide item reference information; not implemented.";
            case "recipe_lookup" -> "Would look up known crafting recipes; not implemented.";
            case "item_usage_lookup" -> "Would show documented item uses; not implemented.";
            case "shulker_preview" -> "Would preview supported shulker contents in tooltips; not implemented.";
            case "container_search" -> "Would filter visible container slots by item name; not implemented.";
            case "durability_warnings" -> "Would notify when equipped item durability is low; not implemented.";
            case "potion_reminder" -> "Would notify before selected potion effects expire; not implemented.";
            case "copy_coordinates" -> "Would copy current coordinates using the client clipboard; not implemented.";
            case "server_information" -> "Would show server details available to the client; not implemented.";
            case "screenshot_manager" -> "Would organize screenshots created by Minecraft; not implemented.";
            case "screenshot_folder" -> "Would open the configured screenshot folder; not implemented.";
            case "session_statistics" -> "Would summarize statistics for the current session; not implemented.";
            case "mod_list_search" -> "Would search installed mod names; not implemented.";
            case "keybind_manager" -> "Would provide a searchable view of key bindings; not implemented.";
            case "configuration_profiles" -> "Would save and switch named Tidal configuration profiles; not implemented.";
            case "hud_layout_profiles" -> "HUD layout presets are available in the HUD editor; this catalogue entry is not independently toggleable.";
            case "reset_settings" -> "Would restore Tidal settings to defaults; not implemented.";
            case "crash_log_viewer" -> "Would open recent Minecraft crash logs; not implemented.";
            default -> "This feature is planned and currently unavailable.";
        };
        this.group = group;
        this.implemented = implemented;
        this.status = !implemented ? Status.PLANNED
                : PARTIAL_FEATURES.contains(id) ? Status.PARTIAL : Status.IMPLEMENTED;
    }
}
