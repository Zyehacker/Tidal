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

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TidalModules {
    private static final List<TidalModule> ALL = new ArrayList<>();
    private static TidalModule fullbright;
    private static int defaultHudIndex;

    static {
        add(TidalModule.Group.HUD, true,
                "CPS", "Keystrokes", "Mouse Buttons",
                "Mouse Trail", "Combo Counter", "Last-hit Reach", "Block Placement Counter",
                "Jump Reset Indicator", "Armor Status", "Armor Durability", "Held Item",
                "Item Durability", "Potion Effects", "Potion Timers", "Health", "Hunger",
                "Saturation", "Combined Hunger", "Absorption", "Compass",
                "Resource Pack Display", "Totem Counter", "Arrow Counter", "Inventory Preview",
                "Item Counter", "XP Level", "Coordinates", "Direction", "Yaw/Pitch",
                "Movement Speed", "Biome", "Dimension", "In-game Clock",
                "Real-world Clock", "Day Counter",
                "Entity Counter", "Server Address", "Player Count", "FPS", "Frame Time",
                "FPS Graph",
                "Ping", "Memory Usage", "Session Timer");

        add(TidalModule.Group.MOVEMENT, false,
                "Toggle Sprint", "Toggle Sneak", "Zoom", "Custom Zoom", "FOV Adjustment",
                "Freelook", "Perspective Toggle", "Auto-jump Toggle",
                "Sprint Key Customization", "Input Display Settings");
        add(TidalModule.Group.VISUAL, false,
                "Fullbright", "Custom Crosshair", "Crosshair Color", "Crosshair Size",
                "Crosshair Gap/Thickness", "View Bobbing", "Camera Shake", "Hand Animations",
                "Weather Visibility", "Particle Density");
        add(TidalModule.Group.INVENTORY, false,
                "Inventory Sorting", "Item Reference", "Recipe Lookup", "Item Usage Lookup",
                "Shulker Preview", "Container Search", "Durability Warnings", "Potion Reminder",
                "Copy Coordinates", "Server Information");
        add(TidalModule.Group.CONVENIENCE, false,
                "Screenshot Manager", "Screenshot Folder", "Session Statistics",
                "Playtime Tracker", "Mod List/Search", "Keybind Manager",
                "Configuration Profiles", "HUD Layout Profiles", "Reset Settings",
                "Crash Log Viewer");
    }

    private TidalModules() {}

    private static void add(TidalModule.Group group, boolean hud, String... names) {
        for (String name : names) {
            String id = name.toLowerCase(java.util.Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
            boolean implemented = hud && switch (id) {
                case "cps", "keystrokes",
                        "fps", "frame_time", "coordinates", "direction", "armor_status",
                        "armor_durability",
                        "potion_effects", "potion_timers", "in_game_clock", "real_world_clock", "health",
                        "hunger", "saturation", "xp_level", "held_item", "item_durability",
                        "totem_counter", "arrow_counter", "day_counter", "ping", "memory_usage",
                        "server_address", "player_count", "movement_speed", "session_timer",
                        "yaw_pitch", "biome", "dimension", "combined_hunger", "absorption",
                        "compass", "resource_pack_display" -> true;
                default -> false;
            } || switch (id) {
                case "fullbright", "view_bobbing", "zoom", "custom_crosshair", "toggle_sprint",
                        "toggle_sneak" -> true;
                default -> false;
            };
            TidalModule module = new TidalModule(id, name, group, implemented);
            if (id.equals("zoom")) {
                module.intensity = 35;
                module.keyCode = GLFW.GLFW_KEY_C;
                module.enabled = true;
            }
            if (id.equals("fullbright")) fullbright = module;
            if (implemented) {
                module.x = 12;
                module.y = 12 + defaultHudIndex++ * 26;
            }
            ALL.add(module);
        }
    }

    public static List<TidalModule> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static TidalModule byId(String id) {
        for (TidalModule module : ALL) {
            if (module.id.equals(id)) return module;
        }
        return null;
    }

    public static TidalModule fullbright() {
        return fullbright;
    }
}
