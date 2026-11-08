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

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":modules:hud"))
    api(project(":modules:events"))
    api(project(":modules:commands"))
    api(project(":modules:notifications"))
    api(libs.jetbrains.compose.foundation)
    api(libs.jetbrains.compose.material)
    api(libs.jetbrains.compose.runtime)
    api(libs.jetbrains.compose.ui)
    api(libs.jetbrains.compose.ui.tooling.preview)
    api(libs.jetbrains.compose.ui.util)
    api(libs.jetbrains.compose.navigation)
    api(libs.jetbrains.lifecycle)
    api(libs.jetbrains.viewmodel)
    api(project(":modules:poly-compose"))
    implementation(libs.jetbrains.skiko.awt.runtime.windows.x64)

    implementation(libs.commonmark)
}

kotlin {
    jvmToolchain(21)
}
