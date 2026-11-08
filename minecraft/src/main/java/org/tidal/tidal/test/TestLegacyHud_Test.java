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

//~ gui_graphics
package cc.tidal.tidal.test;

import kotlin.Pair;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import cc.tidal.tidal.api.config.v1.annotations.RadioButton;
import cc.tidal.tidal.api.config.v1.annotations.Slider;
import cc.tidal.tidal.api.hud.v1.LegacyHud;

public class TestLegacyHud_Test extends LegacyHud {
    private static final float W = 20f;
    private static final float H = 20f;

    @Slider(
            title = "Test Slider",
            min = 0f,
            max = 100f,
            step = 1f
    )
    public static int testSlider = 50;

    @RadioButton(
            title = "Really long title, a reference to _One fish, two fish, red fish, blue fish_. A classical piece from Dr. Suess",
            options = { "One", "Two", "Red", "Blue" }
    )
    public static int testRadio = 0;

    private transient ItemStack stack;

    public TestLegacyHud_Test() {
        super("test-legacy-hud", "Item HUD", Category.Companion.getINFO());
    }

    @Override
    public float getWidth() {
        return W;
    }

    @Override
    public float getHeight() {
        return H;
    }

    @Override
    public boolean update() {
        return false;
    }

    @NotNull
    @Override
    public Pair<Float, Float> defaultPosition() {
        return new Pair<>(10f, 120f);
    }

    @Override
    public boolean showByDefault() {
        return true;
    }

    @Override
    public void render(@NotNull GuiGraphicsExtractor graphics) {
        if (stack == null) stack = new ItemStack(Items.DIAMOND_SWORD);

        graphics.fill(0, 0, (int) W, (int) H, 0x80000000);

        //~ if >= 26.1 'renderItem' -> 'item'
        graphics.item(stack, 2, 2);
    }
}
