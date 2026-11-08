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

package cc.tidal.tidal.utils.v1.dsl

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.client.gui.screens.Screen
import cc.tidal.tidal.api.commands.v1.ClientCommandSource
import cc.tidal.tidal.api.commands.v1.CommandManager
import cc.tidal.tidal.api.config.v1.Config
import cc.tidal.tidal.api.config.v1.Tree
import cc.tidal.tidal.api.platform.v1.Platform
import cc.tidal.tidal.internal.ui.api.ConfigRegistry
import cc.tidal.tidal.internal.ui.api.ConfigSource
import cc.tidal.tidal.internal.ui.compose.impls.TidalUIScreen

fun Screen.openScreen(frames: Int = 1) = Platform.screen().display(this, frames)

fun Tree.createScreen(): Screen {
    val treeId = id
    if (treeId != null) {
        ConfigRegistry.registerTree(this, ConfigSource.OC)
        return TidalUIScreen(treeId, null, this)
    }
    return TidalUIScreen.open()
}

fun Tree.createScreen(initialCategory: String): Screen {
    val treeId = id
    if (treeId != null) {
        ConfigRegistry.registerTree(this, ConfigSource.OC)
        return TidalUIScreen(treeId, initialCategory, this)
    }
    return TidalUIScreen.open()
}

fun Tree.openUI() = Platform.screen().display(createScreen())

fun Tree.openUI(initialCategory: String) = Platform.screen().display(createScreen(initialCategory))

fun Config.createScreen(): Screen {
    val tree = tree ?: run {
        preload()
        this.tree
    }
    return tree?.createScreen() ?: TidalUIScreen.open()
}

fun Config.createScreen(initialCategory: String): Screen {
    val tree = tree ?: run {
        preload()
        this.tree
    }
    return tree?.createScreen(initialCategory) ?: TidalUIScreen.open()
}

fun Config.openUI() = Platform.screen().display(createScreen())

fun Config.openUI(initialCategory: String) = Platform.screen().display(createScreen(initialCategory))

fun Config.addDefaultCommand(command: String = this.title.lowercase()): LiteralArgumentBuilder<ClientCommandSource> {
    return CommandManager.literal(command).executes {
        openUI()
        1
    }
}
