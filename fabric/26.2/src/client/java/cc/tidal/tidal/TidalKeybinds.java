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

import cc.tidal.tidal.mixin.KeyMappingAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

import java.util.IdentityHashMap;
import java.util.Map;

final class TidalKeybinds {
    private static final Logger LOGGER = LoggerFactory.getLogger("Tidal Keybinds");
    static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tidal", "main"));

    private static final Map<TidalModule, KeyMapping> MODULE_KEYS = new IdentityHashMap<>();
    private static boolean registered;

    private TidalKeybinds() {}

    static void register() {
        if (registered) return;
        for (TidalModule module : TidalModules.all()) {
            if (!module.implemented) continue;
            InputConstants.Key defaultKey = inputKey(defaultKeyCode(module));
            KeyMapping mapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.tidal.module." + module.id, defaultKey.getType(),
                    defaultKey.getValue(), CATEGORY));
            mapping.setKey(inputKey(module.keyCode));
            MODULE_KEYS.put(module, mapping);
        }
        KeyMapping.resetMapping();
        registered = true;
        LOGGER.info("Registered {} module key mappings in the Tidal controls category",
                MODULE_KEYS.size());
    }

    static void setKeyCode(TidalModule module, int keyCode) {
        module.keyCode = keyCode;
        KeyMapping mapping = MODULE_KEYS.get(module);
        if (mapping != null) {
            mapping.setKey(inputKey(keyCode));
            KeyMapping.resetMapping();
            Minecraft.getInstance().options.save();
        }
        TidalConfig.save();
    }

    static void applyConfiguredKey(TidalModule module) {
        KeyMapping mapping = MODULE_KEYS.get(module);
        if (mapping == null) return;
        mapping.setKey(inputKey(module.keyCode));
        KeyMapping.resetMapping();
    }

    static void resetDefaults() {
        for (Map.Entry<TidalModule, KeyMapping> entry : MODULE_KEYS.entrySet()) {
            int keyCode = defaultKeyCode(entry.getKey());
            entry.getKey().keyCode = keyCode;
            entry.getValue().setKey(inputKey(keyCode));
        }
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        TidalConfig.save();
    }

    static void tick(Minecraft client) {
        boolean configChanged = false;
        for (Map.Entry<TidalModule, KeyMapping> entry : MODULE_KEYS.entrySet()) {
            TidalModule module = entry.getKey();
            KeyMapping mapping = entry.getValue();
            int mappedCode = codeFor(((KeyMappingAccessor) (Object) mapping)
                    .tidal$getKey());
            if (mappedCode != module.keyCode) {
                module.keyCode = mappedCode;
                configChanged = true;
            }

            if (module.id.equals("zoom")) {
                while (mapping.consumeClick()) {
                    // Zoom uses the held state rather than a queued toggle click.
                }
                continue;
            }
            if (client.player == null || client.gui.screen() != null) {
                while (mapping.consumeClick()) {
                    // Prevent key presses made in menus from toggling modules later.
                }
                continue;
            }
            while (mapping.consumeClick()) {
                module.enabled = !module.enabled;
                if (module.enabled && module.group == TidalModule.Group.HUD) {
                    TidalHud.placeInAvailableSpace(module);
                }
                configChanged = true;
            }
        }
        if (configChanged) TidalConfig.save();
    }

    static boolean isDown(TidalModule module) {
        KeyMapping mapping = MODULE_KEYS.get(module);
        Minecraft client = Minecraft.getInstance();
        return mapping != null && client.player != null && client.gui.screen() == null
                && mapping.isDown();
    }

    static String conflict(TidalModule module) {
        KeyMapping mapping = MODULE_KEYS.get(module);
        if (mapping == null || mapping.isUnbound()) return "";
        InputConstants.Key key = ((KeyMappingAccessor) (Object) mapping).tidal$getKey();
        for (TidalModule other : TidalModules.all()) {
            KeyMapping otherMapping = MODULE_KEYS.get(other);
            if (other == module || otherMapping == null
                    || !((KeyMappingAccessor) (Object) otherMapping).tidal$getKey().equals(key)) {
                continue;
            }
            return "Conflict: " + other.name;
        }
        for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
            if (other == mapping || MODULE_KEYS.containsValue(other)) continue;
            if (!((KeyMappingAccessor) (Object) other).tidal$getKey().equals(key)) continue;
            return "Conflict: " + net.minecraft.network.chat.Component
                    .translatable(other.getName()).getString();
        }
        return "";
    }

    static String displayName(int keyCode) {
        if (keyCode == 0) return "Unbound";
        InputConstants.Key key = inputKey(keyCode);
        return key.getDisplayName().getString();
    }

    private static InputConstants.Key inputKey(int keyCode) {
        if (keyCode < 0) return InputConstants.Type.MOUSE.getOrCreate(-keyCode - 1);
        return InputConstants.Type.KEYSYM.getOrCreate(
                keyCode == 0 ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
    }

    private static int defaultKeyCode(TidalModule module) {
        return module.id.equals("zoom") ? GLFW.GLFW_KEY_C : 0;
    }

    private static int codeFor(InputConstants.Key key) {
        if (key.getType() == InputConstants.Type.MOUSE) return -key.getValue() - 1;
        return key.getValue() == GLFW.GLFW_KEY_UNKNOWN ? 0 : key.getValue();
    }
}
