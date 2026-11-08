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

package cc.tidal.tidal.internal.mixin;

import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import cc.tidal.tidal.internal.ui.sound.TidalSoundPackSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(PackRepository.class)
public abstract class Mixin_PackRepository {
    @Mutable
    @Shadow
    @Final
    private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tidal$addSoundPack(RepositorySource[] sources, CallbackInfo ci) {
        boolean isClientResources = false;
        for (RepositorySource source : sources) {
            if (source instanceof ClientPackSource) {
                isClientResources = true;
                break;
            }
        }
        if (!isClientResources) return;

        Set<RepositorySource> combined = new LinkedHashSet<>(this.sources);
        combined.add(TidalSoundPackSource.INSTANCE);
        this.sources = combined;
    }
}
