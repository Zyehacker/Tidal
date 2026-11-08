/*
 * This file is part of Tidal.
 * Tidal - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Tidal.
 *   <https://tidal.org> <https://github.com/Tidal/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   Tidal is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to Tidal, as published by Tidal,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to Tidal, as published by Tidal. If not, see
 * <https://tidal.org/legal/tidal/additional-terms>
 */

package cc.tidal.tidal.internal.ui.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import cc.tidal.tidal.api.config.v1.ConfigManager;
import cc.tidal.tidal.api.config.v1.Properties;
import cc.tidal.tidal.api.config.v1.Property;
import cc.tidal.tidal.api.config.v1.Tree;
import cc.tidal.tidal.api.ui.v1.keybind.KeyModifiers;
import cc.tidal.tidal.api.ui.v1.keybind.TidalKeybind;
import cc.tidal.tidal.api.ui.v1.keybind.internal.MinecraftKeybindCodec;
import cc.tidal.tidal.internal.TidalConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cc.tidal.tidal.api.config.v1.Tree.tree;

/**
 * Clears keybinds that would conflict with Tidal's own open keybind
 * <br>
 * Each keybind is considered once on the launch it is first seen and the considered identifiers are
 * remembered across launches so a mod installed later still gets cleared while a bind the user moved
 * onto Tidal's key themselves is left alone
 * <br>
 * Keybinds outside the vanilla keybind system are reported here by the owning mod's compat layer
 * through {@link #isNew(String)} and {@link #save()}
 */
public final class KeybindConflicts {
    private static final Logger LOGGER = LogManager.getLogger("Tidal/Keybinds");
    private static final String STATE_FILE = "keybind-conflicts.json";
    private static final String CHECKED_KEYBINDS = "checkedKeybinds";

    private static Property<?> stateProp;
    private static Set<String> checked;
    private static boolean dirty;

    private KeybindConflicts() {
    }

    /**
     * The key Tidal's open keybind occupies and which conflicting keybinds are cleared from
     * <br>
     * {@link InputConstants#UNKNOWN} when the keybind is unbound or uses modifiers, mouse buttons or a
     * key combo, as those cannot clash with a plain single-key bind
     */
    public static InputConstants.Key key() {
        TidalKeybind keybind = TidalConfig.oneConfigKeybind;
        if (keybind == null || keybind.getMods() != KeyModifiers.NONE) return InputConstants.UNKNOWN;
        int[] keys = keybind.getKeyCodes();
        int[] mouse = keybind.getMouseBtns();
        if (keys == null || keys.length != 1 || (mouse != null && mouse.length != 0)) return InputConstants.UNKNOWN;
        return MinecraftKeybindCodec.keysym(keys[0]);
    }

    /**
     * Records {@code id} as checked and returns whether it had never been checked on a previous launch
     * <br>
     * Identifiers outside the vanilla keybind system should be prefixed with the owning mod's id and
     * {@link #save()} must be called once done to persist them
     */
    public static synchronized boolean isNew(String id) {
        load();
        if (!checked.add(id)) return false;
        dirty = true;
        return true;
    }

    /**
     * Persists the identifiers recorded through {@link #isNew(String)}
     */
    public static synchronized void save() {
        if (!dirty) return;
        dirty = false;
        stateProp.setAs(checked.toArray(new String[0]));
        ConfigManager.internal().save(STATE_FILE);
    }

    /**
     * Unbinds Minecraft keybinds that conflict with Tidal's keybind
     */
    public static void unbindMinecraftKeybinds() {
        InputConstants.Key key = key();
        if (key == InputConstants.UNKNOWN) return;
        String keyName = key.getName();
        List<String> unbound = new ArrayList<>();

        for (KeyMapping keyMapping : MinecraftKeybindProvider.INSTANCE.managedMappings()) {
            if (!isNew(keyMapping.getName())) continue;
            if (!keyName.equals(keyMapping.saveString())) continue;
            keyMapping.setKey(InputConstants.UNKNOWN);
            unbound.add(keyMapping.getName());
        }

        if (!unbound.isEmpty()) {
            KeyMapping.resetMapping();
            Minecraft.getInstance().options.save();
            LOGGER.info("Unbound {} Minecraft keybind(s) using {}: {}", unbound.size(), keyName, unbound);
        }
        save();
    }

    private static void load() {
        if (checked != null) return;
        Tree state = ConfigManager.internal().register(
                tree(STATE_FILE).put(
                        Properties.simple(CHECKED_KEYBINDS, "Checked Keybinds",
                                "Keybinds which have already been checked against Tidal's keybind.",
                                new String[0], String[].class)
                )
        ).get();
        stateProp = state.getProp(CHECKED_KEYBINDS);

        Object stored = stateProp.get();
        checked = new LinkedHashSet<>();
        if (stored instanceof Object[]) {
            for (Object name : (Object[]) stored) {
                if (name != null) checked.add(name.toString());
            }
        } else if (stored instanceof Iterable<?>) {
            for (Object name : (Iterable<?>) stored) {
                if (name != null) checked.add(name.toString());
            }
        }
    }
}
