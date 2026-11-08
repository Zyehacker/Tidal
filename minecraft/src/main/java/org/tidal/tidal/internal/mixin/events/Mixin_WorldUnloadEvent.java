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

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import cc.tidal.tidal.api.event.v1.EventManager;
import cc.tidal.tidal.api.event.v1.events.WorldEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_WorldUnloadEvent {

    @Shadow public ClientLevel level;

    @Inject(
            method = "setLevel"
            , at = @At("HEAD"))
    private void onWorldUnloadCallback(CallbackInfo ci) {
        tidal$postWorldUnload();
    }

    //? if >= 1.21.11 {
    @Inject(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V"
            , at = @At("HEAD"))
    //?} else {
    /*@Inject(
            method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V"
            , at = @At("HEAD"))
    *///?}
    private void onDisconnectCallback(CallbackInfo ci) {
        tidal$postWorldUnload();
    }

    @Inject(
            method = "clearClientLevel"
            , at = @At("HEAD"))
    private void onClearClientLevelCallback(CallbackInfo ci) {
        tidal$postWorldUnload();
    }

    @Unique
    private void tidal$postWorldUnload() {
        if (this.level != null) {
            EventManager.INSTANCE.post(new WorldEvent.Unload(this.level));
        }
    }

}
