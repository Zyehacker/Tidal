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

package cc.tidal.tidal.internal.ui.sound;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
//? if >= 26.3
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;
//? if >= 26.3
import java.util.stream.Stream;

public final class TidalSoundPackSource implements RepositorySource {
    public static final TidalSoundPackSource INSTANCE = new TidalSoundPackSource();

    public static final Path PACK_ROOT = Paths.get("tidal", "sounds");

    private static final String PACK_ID = "tidal_sounds";

    private TidalSoundPackSource() {
    }

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        if (!Files.isRegularFile(PACK_ROOT.resolve("pack.mcmeta"))) return;

        PackLocationInfo location = new PackLocationInfo(
                PACK_ID,
                Component.literal("Tidal Sounds"),
                PackSource.BUILT_IN,
                Optional.empty()
        );
        //? if >= 26.3 {
        Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
            @Override
            public PackMetadataResources openMetadata(PackLocationInfo info) {
                return new PathPackResources(info, PACK_ROOT);
            }

            @Override
            public Stream<PackResources> openResources(PackLocationInfo info, Pack.Metadata metadata) {
                return Stream.of(new PathPackResources(info, PACK_ROOT));
            }
        };
        //?} else {
        /*Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo info) {
                return new PathPackResources(info, PACK_ROOT);
            }

            @Override
            public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                return new PathPackResources(info, PACK_ROOT);
            }
        };
        *///?}
        PackSelectionConfig selection = new PackSelectionConfig(true, Pack.Position.BOTTOM, false);

        Pack pack = Pack.readMetaAndCreate(location, resources, PackType.CLIENT_RESOURCES, selection);
        if (pack != null) {
            consumer.accept(pack);
        }
    }
}
