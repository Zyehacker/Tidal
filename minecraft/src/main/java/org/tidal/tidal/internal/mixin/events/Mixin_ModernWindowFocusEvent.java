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

package cc.tidal.tidal.internal.mixin.events;

//? if >= 26.1 {
import com.mojang.blaze3d.platform.Window;
//? } else {
/*import net.minecraft.client.Minecraft;
*///? }
import cc.tidal.tidal.api.event.v1.EventManager;
import cc.tidal.tidal.api.event.v1.events.WindowFocusEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


//? if >= 26.1 {
@Mixin(Window.class)
public class Mixin_ModernWindowFocusEvent {
    @Inject(method = "onFocus", at = @At("TAIL"))
    //~ if >= 26.3 'long window, boolean focused' -> 'boolean focused'
    private void onGameFocused(boolean focused, CallbackInfo ci) {
        if (focused) EventManager.INSTANCE.post(WindowFocusEvent.Gained.INSTANCE);
        else EventManager.INSTANCE.post(WindowFocusEvent.Lost.INSTANCE);
    }
}
//? } else {
/*@Mixin(Minecraft.class)
public class Mixin_ModernWindowFocusEvent {
    @Inject(method = "setWindowActive", at = @At("HEAD"))
    private void onGameFocused(boolean focused, CallbackInfo ci) {
        if (focused) EventManager.INSTANCE.post(WindowFocusEvent.Gained.INSTANCE);
        else EventManager.INSTANCE.post(WindowFocusEvent.Lost.INSTANCE);
    }
}
*///? }
