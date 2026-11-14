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

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class TidalConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Tidal");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tidal.json");
    private static final Map<String, JsonObject> HUD_LAYOUTS = new LinkedHashMap<>();
    private static final Map<String, JsonObject> CONFIG_PROFILES = new LinkedHashMap<>();
    private static final Set<String> BUILTIN_LAYOUTS =
            Set.of("Default", "Minimal", "Performance", "PvP", "Custom");
    private static final Set<String> FAVORITE_MODS = new LinkedHashSet<>();
    public static boolean snapToGrid = true;
    public static boolean snapToEdges = true;
    public static boolean hudEnabled = true;
    public static boolean useCefUi;
    public static boolean uiAnimations = true;
    public static boolean uiTextShadow;
    public static boolean advancedDeveloperMode;
    public static boolean fpsBoostEnabled;
    public static boolean enableAquaCosmetics;
    public static boolean showEquippedSkinIntegration;
    public static int uiAntialiasing = 2;
    public static TidalModule.HudStyle defaultHudStyle = TidalModule.HudStyle.ROUNDED;
    public static int defaultHudAccent = TidalTheme.DEFAULT_ACCENT;
    public static boolean lightTheme;
    public static int themeAccent = TidalTheme.DEFAULT_ACCENT;
    public static int crosshairSize = 5;
    public static int crosshairGap = 3;
    public static int crosshairThickness = 1;
    public static int crosshairColor = 0xFFFFFFFF;
    public static int crosshairShape;
    public static boolean crosshairOutline = true;
    public static boolean dynamicCrosshair;
    public static int zoomSmoothness = 75;
    public static boolean zoomScrollEnabled = true;
    public static String activeHudLayout = "Default";
    public static String activeConfigProfile = "Default";

    private TidalConfig() {}

    public static void load() {
        HUD_LAYOUTS.clear();
        CONFIG_PROFILES.clear();
        FAVORITE_MODS.clear();
        fpsBoostEnabled = false;
        enableAquaCosmetics = false;
        showEquippedSkinIntegration = false;
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                snapToGrid = root.has("snapToGrid") && root.get("snapToGrid").getAsBoolean();
                snapToEdges = !root.has("snapToEdges") || root.get("snapToEdges").getAsBoolean();
                hudEnabled = !root.has("hudEnabled") || root.get("hudEnabled").getAsBoolean();
                if (root.has("useCefUi")) {
                    // CEF is dormant in this build; native rendering is always used.
                    useCefUi = false;
                }
                if (root.has("uiAnimations")) {
                    uiAnimations = root.get("uiAnimations").getAsBoolean();
                }
                if (root.has("uiTextShadow")) {
                    uiTextShadow = root.get("uiTextShadow").getAsBoolean();
                }
                if (root.has("favoriteMods") && root.get("favoriteMods").isJsonArray()) {
                    for (JsonElement mod : root.getAsJsonArray("favoriteMods")) {
                        if (mod.isJsonPrimitive()) FAVORITE_MODS.add(mod.getAsString());
                    }
                }
                if (root.has("advancedDeveloperMode")) {
                    advancedDeveloperMode = root.get("advancedDeveloperMode").getAsBoolean();
                }
                if (root.has("zoomSmoothness")) {
                    zoomSmoothness = Math.max(0, Math.min(100,
                            root.get("zoomSmoothness").getAsInt()));
                }
                if (root.has("zoomScrollEnabled")) {
                    zoomScrollEnabled = root.get("zoomScrollEnabled").getAsBoolean();
                }
                if (root.has("uiAntialiasing")) {
                    uiAntialiasing = aaQuality(root.get("uiAntialiasing").getAsInt());
                }
                if (root.has("defaultHudStyle")) {
                    defaultHudStyle = enumValue(TidalModule.HudStyle.class,
                            root.get("defaultHudStyle").getAsString(),
                            TidalModule.HudStyle.ROUNDED);
                }
                if (root.has("defaultHudAccent")) {
                    defaultHudAccent = migrateLegacyAccent(
                            root.get("defaultHudAccent").getAsInt());
                }
                if (root.has("lightTheme")) lightTheme = root.get("lightTheme").getAsBoolean();
                if (root.has("themeAccent")) {
                    themeAccent = migrateLegacyAccent(root.get("themeAccent").getAsInt());
                }
                loadCrosshairSettings(root);
                for (TidalModule module : TidalModules.all()) {
                    if (module.group == TidalModule.Group.HUD) {
                        module.hudStyle = defaultHudStyle;
                        module.accentColor = defaultHudAccent;
                    }
                }
                if (root.has("modules")) {
                    applyModuleStates(root.getAsJsonObject("modules"), true);
                    separateDefaultOverlaps();
                }
                if (root.has("hudLayouts") && root.get("hudLayouts").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry
                            : root.getAsJsonObject("hudLayouts").entrySet()) {
                        if (entry.getValue().isJsonObject()) {
                            HUD_LAYOUTS.put(entry.getKey(), entry.getValue().getAsJsonObject());
                        }
                    }
                }
                if (root.has("activeHudLayout")) {
                    activeHudLayout = root.get("activeHudLayout").getAsString();
                }
                if (root.has("configProfiles")
                        && root.get("configProfiles").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry
                            : root.getAsJsonObject("configProfiles").entrySet()) {
                        if (entry.getValue().isJsonObject()) {
                            CONFIG_PROFILES.put(entry.getKey(),
                                    entry.getValue().getAsJsonObject());
                        }
                    }
                }
                if (root.has("activeConfigProfile")) {
                    activeConfigProfile = root.get("activeConfigProfile").getAsString();
                }
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Could not read Tidal configuration at {}", FILE, exception);
            }
        }
        if (CONFIG_PROFILES.isEmpty()) {
            CONFIG_PROFILES.put("Default", captureConfigProfile());
        }
        if (!CONFIG_PROFILES.containsKey(activeConfigProfile)) {
            activeConfigProfile = CONFIG_PROFILES.keySet().iterator().next();
        }
        TidalTheme.apply(lightTheme, themeAccent);
    }

    private static int aaQuality(int value) {
        return value == 4 ? 4 : value == 2 ? 2 : 1;
    }

    private static int migrateLegacyAccent(int color) {
        return color == 0xFF385BFF ? TidalTheme.DEFAULT_ACCENT : color;
    }

    private static void loadCrosshairSettings(JsonObject source) {
        if (source.has("crosshairSize")) {
            crosshairSize = Math.max(2, Math.min(16, source.get("crosshairSize").getAsInt()));
        }
        if (source.has("crosshairGap")) {
            crosshairGap = Math.max(0, Math.min(16, source.get("crosshairGap").getAsInt()));
        }
        if (source.has("crosshairThickness")) {
            crosshairThickness = Math.max(1,
                    Math.min(8, source.get("crosshairThickness").getAsInt()));
        }
        if (source.has("crosshairColor")) crosshairColor = source.get("crosshairColor").getAsInt();
        if (source.has("crosshairShape")) {
            crosshairShape = Math.max(0, Math.min(2, source.get("crosshairShape").getAsInt()));
        }
        if (source.has("crosshairOutline")) {
            crosshairOutline = source.get("crosshairOutline").getAsBoolean();
        }
        if (source.has("dynamicCrosshair")) {
            dynamicCrosshair = source.get("dynamicCrosshair").getAsBoolean();
        }
    }

    private static void writeCrosshairSettings(JsonObject target) {
        target.addProperty("crosshairSize", crosshairSize);
        target.addProperty("crosshairGap", crosshairGap);
        target.addProperty("crosshairThickness", crosshairThickness);
        target.addProperty("crosshairColor", crosshairColor);
        target.addProperty("crosshairShape", crosshairShape);
        target.addProperty("crosshairOutline", crosshairOutline);
        target.addProperty("dynamicCrosshair", dynamicCrosshair);
    }

    public static void initializeHudLayouts(int viewportWidth, int viewportHeight) {
        TidalHud.initializePositions(viewportWidth, viewportHeight);
        ensureBuiltinLayouts();
        if (!HUD_LAYOUTS.containsKey(activeHudLayout)) activeHudLayout = "Default";
    }

    private static void applyModuleStates(JsonObject modules, boolean includeKeybinds) {
        for (TidalModule module : TidalModules.all()) {
            JsonElement value = modules.get(module.id);
            if (value == null && module.id.equals("cps")) {
                value = modules.get("combined_cps");
                if (value == null) value = modules.get("left_cps");
                if (value == null) value = modules.get("right_cps");
            }
            if (value == null || !value.isJsonObject()) continue;
            JsonObject state = value.getAsJsonObject();
            if (state.has("enabled")) module.enabled = state.get("enabled").getAsBoolean();
            if (module.id.equals("cps") && modules.get("cps") == null
                    && modules.get("combined_cps") == null) {
                module.enabled = enabled(modules.get("left_cps"))
                        || enabled(modules.get("right_cps"));
            }
            if (state.has("favorite")) module.favorite = state.get("favorite").getAsBoolean();
            if (state.has("hidden")) module.hidden = state.get("hidden").getAsBoolean();
            if (!module.implemented) module.enabled = false;
            if (includeKeybinds && state.has("keyCode")) {
                module.keyCode = state.get("keyCode").getAsInt();
                TidalKeybinds.applyConfiguredKey(module);
            }
            if (state.has("x")) module.x = state.get("x").getAsInt();
            if (state.has("y")) module.y = state.get("y").getAsInt();
            if (state.has("normalizedX") && state.has("normalizedY")) {
                module.normalizedX = Math.max(0,
                        Math.min(1, state.get("normalizedX").getAsFloat()));
                module.normalizedY = Math.max(0,
                        Math.min(1, state.get("normalizedY").getAsFloat()));
                module.normalizedPosition = true;
            } else {
                module.normalizedPosition = false;
            }
            if (state.has("scale")) module.scale = Math.max(0.5f,
                    Math.min(2.0f, state.get("scale").getAsFloat()));
            if (state.has("anchor")) {
                module.anchor = enumValue(TidalModule.Anchor.class,
                        state.get("anchor").getAsString(), TidalModule.Anchor.TOP_LEFT);
            }
            if (state.has("locked")) module.locked = state.get("locked").getAsBoolean();
            if (state.has("hudStyle")) {
                module.hudStyle = enumValue(TidalModule.HudStyle.class,
                        state.get("hudStyle").getAsString(), TidalModule.HudStyle.ROUNDED);
            }
            if (state.has("textColor")) module.textColor = state.get("textColor").getAsInt();
            if (state.has("accentColor")) module.accentColor =
                    migrateLegacyAccent(state.get("accentColor").getAsInt());
            if (state.has("backgroundColor")) module.backgroundColor =
                    state.get("backgroundColor").getAsInt();
            if (state.has("backgroundOpacity")) module.backgroundOpacity =
                    Math.max(0, Math.min(100, state.get("backgroundOpacity").getAsInt()));
            if (state.has("surfaceGroupId")) {
                module.surfaceGroupId = Math.max(0,
                        state.get("surfaceGroupId").getAsInt());
            } else module.surfaceGroupId = 0;
            if (state.has("intensity")) module.intensity =
                    Math.max(0, Math.min(100, state.get("intensity").getAsInt()));
        }
    }

    private static boolean enabled(JsonElement value) {
        return value != null && value.isJsonObject()
                && value.getAsJsonObject().has("enabled")
                && value.getAsJsonObject().get("enabled").getAsBoolean();
    }

    private static void ensureBuiltinLayouts() {
        HUD_LAYOUTS.putIfAbsent("Default", captureHudLayout());
        HUD_LAYOUTS.putIfAbsent("Minimal",
                createPreset(Set.of("fps", "coordinates", "health")));
        HUD_LAYOUTS.putIfAbsent("Performance",
                createPreset(Set.of("fps", "ping", "memory_usage", "movement_speed")));
        HUD_LAYOUTS.putIfAbsent("PvP", createPreset(Set.of("fps", "coordinates", "health",
                "armor_status", "armor_durability", "totem_counter", "ender_pearl_counter")));
        HUD_LAYOUTS.putIfAbsent("Custom", captureHudLayout());
    }

    private static JsonObject createPreset(Set<String> enabledIds) {
        JsonObject layout = captureHudLayout();
        JsonObject states = layout.getAsJsonObject("modules");
        int row = 0;
        for (TidalModule module : TidalModules.all()) {
            JsonObject state = states.getAsJsonObject(module.id);
            boolean enabled = module.implemented && enabledIds.contains(module.id);
            state.addProperty("enabled", enabled);
            if (enabled) {
                state.addProperty("anchor", TidalModule.Anchor.TOP_LEFT.name());
                state.addProperty("normalizedX", 0.02f);
                state.addProperty("normalizedY", Math.min(0.9f, 0.02f + row++ * 0.055f));
            }
        }
        return layout;
    }

    private static JsonObject captureHudLayout() {
        JsonObject layout = new JsonObject();
        JsonObject modules = new JsonObject();
        for (TidalModule module : TidalModules.all()) {
            JsonObject state = new JsonObject();
            state.addProperty("enabled", module.enabled && module.implemented);
            state.addProperty("favorite", module.favorite);
            state.addProperty("hidden", module.hidden);
            state.addProperty("x", module.x);
            state.addProperty("y", module.y);
            state.addProperty("normalizedX", module.normalizedX);
            state.addProperty("normalizedY", module.normalizedY);
            state.addProperty("scale", module.scale);
            state.addProperty("anchor", module.anchor.name());
            state.addProperty("locked", module.locked);
            state.addProperty("hudStyle", module.hudStyle.name());
            state.addProperty("textColor", module.textColor);
            state.addProperty("accentColor", module.accentColor);
            state.addProperty("backgroundColor", module.backgroundColor);
            state.addProperty("backgroundOpacity", module.backgroundOpacity);
            state.addProperty("surfaceGroupId", module.surfaceGroupId);
            state.addProperty("intensity", module.intensity);
            modules.add(module.id, state);
        }
        layout.add("modules", modules);
        return layout;
    }

    public static List<String> hudLayoutNames() {
        return new ArrayList<>(HUD_LAYOUTS.keySet());
    }

    public static List<String> configProfileNames() {
        return new ArrayList<>(CONFIG_PROFILES.keySet());
    }

    public static boolean saveConfigProfile(String name) {
        String normalized = normalizeLayoutName(name);
        if (normalized.isEmpty()) return false;
        CONFIG_PROFILES.put(normalized, captureConfigProfile());
        activeConfigProfile = normalized;
        save();
        return true;
    }

    public static boolean loadConfigProfile(String name) {
        JsonObject profile = CONFIG_PROFILES.get(name);
        if (profile == null || !profile.has("modules")
                || !profile.get("modules").isJsonObject()) return false;
        snapToGrid = !profile.has("snapToGrid") || profile.get("snapToGrid").getAsBoolean();
        snapToEdges = !profile.has("snapToEdges") || profile.get("snapToEdges").getAsBoolean();
        hudEnabled = !profile.has("hudEnabled") || profile.get("hudEnabled").getAsBoolean();
        uiAnimations = !profile.has("uiAnimations") || profile.get("uiAnimations").getAsBoolean();
        uiTextShadow = profile.has("uiTextShadow")
                && profile.get("uiTextShadow").getAsBoolean();
        advancedDeveloperMode = profile.has("advancedDeveloperMode")
                && profile.get("advancedDeveloperMode").getAsBoolean();
        fpsBoostEnabled = false;
        zoomSmoothness = profile.has("zoomSmoothness")
                ? Math.max(0, Math.min(100, profile.get("zoomSmoothness").getAsInt())) : 75;
        zoomScrollEnabled = !profile.has("zoomScrollEnabled")
                || profile.get("zoomScrollEnabled").getAsBoolean();
        enableAquaCosmetics = false;
        showEquippedSkinIntegration = false;
        if (profile.has("uiAntialiasing")) {
            uiAntialiasing = aaQuality(profile.get("uiAntialiasing").getAsInt());
        }
        defaultHudStyle = enumValue(TidalModule.HudStyle.class,
                profile.has("defaultHudStyle") ? profile.get("defaultHudStyle").getAsString()
                        : TidalModule.HudStyle.ROUNDED.name(),
                TidalModule.HudStyle.ROUNDED);
        if (profile.has("defaultHudAccent")) {
            defaultHudAccent = migrateLegacyAccent(
                    profile.get("defaultHudAccent").getAsInt());
        }
        lightTheme = profile.has("lightTheme") && profile.get("lightTheme").getAsBoolean();
        if (profile.has("themeAccent")) {
            themeAccent = migrateLegacyAccent(profile.get("themeAccent").getAsInt());
        }
        TidalTheme.apply(lightTheme, themeAccent);
        loadCrosshairSettings(profile);
        applyModuleStates(profile.getAsJsonObject("modules"), true);
        activeConfigProfile = name;
        save();
        return true;
    }

    public static boolean renameConfigProfile(String oldName, String newName) {
        String normalized = normalizeLayoutName(newName);
        if (oldName == null || oldName.equals("Default") || normalized.isEmpty()
                || CONFIG_PROFILES.containsKey(normalized)) return false;
        JsonObject profile = CONFIG_PROFILES.remove(oldName);
        if (profile == null) return false;
        CONFIG_PROFILES.put(normalized, profile);
        if (activeConfigProfile.equals(oldName)) activeConfigProfile = normalized;
        save();
        return true;
    }

    public static boolean deleteConfigProfile(String name) {
        if (name == null || name.equals("Default") || CONFIG_PROFILES.remove(name) == null) {
            return false;
        }
        if (activeConfigProfile.equals(name)) {
            if (!CONFIG_PROFILES.containsKey("Default")) {
                CONFIG_PROFILES.put("Default", captureConfigProfile());
            }
            loadConfigProfile("Default");
        } else {
            save();
        }
        return true;
    }

    public static String exportConfigProfile(String name) {
        JsonObject profile = CONFIG_PROFILES.get(name);
        if (profile == null) return "";
        JsonObject exported = new JsonObject();
        exported.addProperty("format", 1);
        exported.addProperty("name", name);
        exported.add("profile", profile.deepCopy());
        return exported.toString();
    }

    public static Path backupConfig() {
        save();
        Path backup = FILE.resolveSibling("tidal-backup-" + Instant.now().toString()
                .replace(':', '-') + ".json");
        try {
            Files.copy(FILE, backup, StandardCopyOption.COPY_ATTRIBUTES);
            return backup;
        } catch (IOException exception) {
            LOGGER.error("Could not back up Tidal configuration to {}", backup, exception);
            throw new IllegalStateException("Could not back up Tidal configuration", exception);
        }
    }

    public static String exportConfig() {
        save();
        try {
            return Files.readString(FILE);
        } catch (IOException exception) {
            LOGGER.error("Could not export Tidal configuration from {}", FILE, exception);
            throw new IllegalStateException("Could not export Tidal configuration", exception);
        }
    }

    public static boolean restoreDefaults() {
        return loadConfigProfile("Default");
    }

    public static boolean importConfigProfile(String json) {
        try {
            JsonObject exported = JsonParser.parseString(json).getAsJsonObject();
            if (!exported.has("format") || exported.get("format").getAsInt() != 1
                    || !exported.has("profile") || !exported.get("profile").isJsonObject()) {
                return false;
            }
            JsonObject profile = exported.getAsJsonObject("profile");
            if (!profile.has("modules") || !profile.get("modules").isJsonObject()) return false;
            String name = normalizeLayoutName(exported.has("name")
                    ? exported.get("name").getAsString() : "Imported");
            if (name.isEmpty()) name = "Imported";
            String candidate = name;
            int suffix = 2;
            while (CONFIG_PROFILES.containsKey(candidate)) {
                String tail = " (" + suffix++ + ")";
                candidate = name.substring(0, Math.min(name.length(), 32 - tail.length())) + tail;
            }
            CONFIG_PROFILES.put(candidate, profile.deepCopy());
            if (!loadConfigProfile(candidate)) {
                CONFIG_PROFILES.remove(candidate);
                return false;
            }
            return true;
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not import Tidal configuration profile", exception);
            return false;
        }
    }

    private static JsonObject captureConfigProfile() {
        JsonObject profile = new JsonObject();
        profile.addProperty("snapToGrid", snapToGrid);
        profile.addProperty("snapToEdges", snapToEdges);
        profile.addProperty("hudEnabled", hudEnabled);
        profile.addProperty("uiAnimations", uiAnimations);
        profile.addProperty("uiTextShadow", uiTextShadow);
        profile.addProperty("advancedDeveloperMode", advancedDeveloperMode);
        profile.addProperty("fpsBoostEnabled", false);
        profile.addProperty("zoomSmoothness", zoomSmoothness);
        profile.addProperty("zoomScrollEnabled", zoomScrollEnabled);
        profile.addProperty("enableAquaCosmetics", false);
        profile.addProperty("showEquippedSkinIntegration", false);
        profile.addProperty("uiAntialiasing", aaQuality(uiAntialiasing));
        profile.addProperty("defaultHudStyle", defaultHudStyle.name());
        profile.addProperty("defaultHudAccent", defaultHudAccent);
        profile.addProperty("lightTheme", lightTheme);
        profile.addProperty("themeAccent", themeAccent);
        writeCrosshairSettings(profile);
        profile.add("modules", captureHudLayout().getAsJsonObject("modules"));
        return profile;
    }

    public static boolean saveHudLayout(String name) {
        String normalized = normalizeLayoutName(name);
        if (normalized.isEmpty()) return false;
        HUD_LAYOUTS.put(normalized, captureHudLayout());
        activeHudLayout = normalized;
        save();
        return true;
    }

    public static boolean loadHudLayout(String name) {
        JsonObject layout = HUD_LAYOUTS.get(name);
        if (layout == null || !layout.has("modules")
                || !layout.get("modules").isJsonObject()) return false;
        applyModuleStates(layout.getAsJsonObject("modules"), false);
        activeHudLayout = name;
        save();
        return true;
    }

    public static boolean renameHudLayout(String oldName, String newName) {
        String normalized = normalizeLayoutName(newName);
        if (BUILTIN_LAYOUTS.contains(oldName) || normalized.isEmpty()
                || HUD_LAYOUTS.containsKey(normalized)) return false;
        JsonObject layout = HUD_LAYOUTS.remove(oldName);
        if (layout == null) return false;
        HUD_LAYOUTS.put(normalized, layout);
        if (activeHudLayout.equals(oldName)) activeHudLayout = normalized;
        save();
        return true;
    }

    public static boolean deleteHudLayout(String name) {
        if (BUILTIN_LAYOUTS.contains(name) || HUD_LAYOUTS.remove(name) == null) return false;
        if (activeHudLayout.equals(name)) {
            activeHudLayout = "Default";
            loadHudLayout(activeHudLayout);
        } else {
            save();
        }
        return true;
    }

    private static String normalizeLayoutName(String name) {
        if (name == null) return "";
        String normalized = name.trim();
        return normalized.length() > 32 ? normalized.substring(0, 32) : normalized;
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String name, T fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Unknown {} value '{}' in Tidal config; using {}",
                    type.getSimpleName(), name, fallback);
            return fallback;
        }
    }

    private static void separateDefaultOverlaps() {
        java.util.List<TidalModule> overlappingDefaults = new java.util.ArrayList<>();
        for (TidalModule module : TidalModules.all()) {
            if (module.enabled && module.implemented && module.x == 12 && module.y == 12) {
                overlappingDefaults.add(module);
            }
        }
        if (overlappingDefaults.size() < 2) return;

        int nextY = 12;
        for (TidalModule module : overlappingDefaults) {
            while (occupiedByEnabledModule(12, nextY, overlappingDefaults, module)) {
                nextY += 26;
            }
            module.x = 12;
            module.y = nextY;
            nextY += 26;
        }
    }

    private static boolean occupiedByEnabledModule(int x, int y,
                                                    java.util.List<TidalModule> relocating,
                                                    TidalModule current) {
        for (TidalModule candidate : TidalModules.all()) {
            if (candidate == current || relocating.contains(candidate)) continue;
            if (candidate.enabled && candidate.implemented
                    && candidate.x == x && candidate.y == y) return true;
        }
        return false;
    }

    public static void save() {
        fpsBoostEnabled = false;
        enableAquaCosmetics = false;
        showEquippedSkinIntegration = false;
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("snapToGrid", snapToGrid);
            root.addProperty("snapToEdges", snapToEdges);
            root.addProperty("hudEnabled", hudEnabled);
            root.addProperty("useCefUi", useCefUi);
            root.addProperty("uiAnimations", uiAnimations);
            root.addProperty("uiTextShadow", uiTextShadow);
            JsonArray favoriteMods = new JsonArray();
            FAVORITE_MODS.stream().sorted().forEach(favoriteMods::add);
            root.add("favoriteMods", favoriteMods);
            root.addProperty("advancedDeveloperMode", advancedDeveloperMode);
            root.addProperty("fpsBoostEnabled", false);
            root.addProperty("zoomSmoothness", zoomSmoothness);
            root.addProperty("zoomScrollEnabled", zoomScrollEnabled);
            root.addProperty("enableAquaCosmetics", false);
            root.addProperty("showEquippedSkinIntegration", false);
            root.addProperty("uiAntialiasing", aaQuality(uiAntialiasing));
            root.addProperty("defaultHudStyle", defaultHudStyle.name());
            root.addProperty("defaultHudAccent", defaultHudAccent);
            root.addProperty("lightTheme", lightTheme);
            root.addProperty("themeAccent", themeAccent);
            writeCrosshairSettings(root);
            JsonObject modules = new JsonObject();
            for (TidalModule module : TidalModules.all()) {
                JsonObject state = new JsonObject();
                state.addProperty("enabled", module.enabled);
                state.addProperty("favorite", module.favorite);
                state.addProperty("hidden", module.hidden);
                state.addProperty("keyCode", module.keyCode);
                state.addProperty("x", module.x);
                state.addProperty("y", module.y);
                state.addProperty("normalizedX", module.normalizedX);
                state.addProperty("normalizedY", module.normalizedY);
                state.addProperty("scale", module.scale);
                state.addProperty("anchor", module.anchor.name());
                state.addProperty("locked", module.locked);
                state.addProperty("hudStyle", module.hudStyle.name());
                state.addProperty("textColor", module.textColor);
                state.addProperty("accentColor", module.accentColor);
                state.addProperty("backgroundColor", module.backgroundColor);
                state.addProperty("backgroundOpacity", module.backgroundOpacity);
                state.addProperty("surfaceGroupId", module.surfaceGroupId);
                state.addProperty("intensity", module.intensity);
                modules.add(module.id, state);
            }

            root.add("modules", modules);
            JsonObject layouts = new JsonObject();
            for (Map.Entry<String, JsonObject> entry : HUD_LAYOUTS.entrySet()) {
                layouts.add(entry.getKey(), entry.getValue());
            }
            root.add("hudLayouts", layouts);
            root.addProperty("activeHudLayout", activeHudLayout);
            JsonObject profiles = new JsonObject();
            for (Map.Entry<String, JsonObject> entry : CONFIG_PROFILES.entrySet()) {
                profiles.add(entry.getKey(), entry.getValue());
            }
            root.add("configProfiles", profiles);
            root.addProperty("activeConfigProfile", activeConfigProfile);
            Path temp = FILE.resolveSibling("tidal.json.tmp");
            try (Writer writer = Files.newBufferedWriter(temp)) {
                writer.write(root.toString());
            }
            try {
                Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not save Tidal configuration at {}", FILE, exception);
        }
    }

    static boolean isFavoriteMod(String id) {
        return FAVORITE_MODS.contains(id);
    }

    static void setFavoriteMod(String id, boolean favorite) {
        if (favorite) FAVORITE_MODS.add(id);
        else FAVORITE_MODS.remove(id);
        save();
    }
}
