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

package cc.tidal.tidal.test;

import cc.tidal.tidal.api.commands.v1.CommandManager;
import cc.tidal.tidal.api.event.v1.EventManager;
import cc.tidal.tidal.api.event.v1.events.InitializationEvent;
import cc.tidal.tidal.api.hud.v1.HudManager;
import cc.tidal.tidal.internal.ui.api.ConfigRegistry;
import cc.tidal.tidal.internal.ui.api.ConfigSource;

public final class TestMod_Test {
    public static void initialize() {
        EventManager.register(InitializationEvent.class, () -> {
            System.err.println("TestMod::init");
            CommandManager.register(new TestCommand_Test());
            TestConfig_Test config = TestConfig_Test.getInstance();
            if (config.getTree() == null) {
                config.preload();
            }
            ConfigRegistry.INSTANCE.registerTree(config.getTree(), ConfigSource.OC);
            TestKtConfig.INSTANCE.preload();
            ConfigRegistry.INSTANCE.registerTree(TestKtConfig.INSTANCE.getTree(), ConfigSource.OC);

            HudManager.register(new TestLegacyHud_Test(), "test_mod", "combat");
            HudManager.register(new TestItemHud_Test(), "test_mod");
            HudManager.register(new PingDisplayHud_Test(), "test_mod", "qol");
        });
    }
}
