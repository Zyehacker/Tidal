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

package cc.tidal.tidal.internal.ui.sound

import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import cc.tidal.tidal.utils.v1.JsonUtils
import cc.tidal.tidal.utils.v1.Multithreading
import cc.tidal.tidal.utils.v1.NetworkUtils
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Downloads the large UI sound files kept out of the jar (issue #627) and exposes them to Minecraft's
 * sound engine via [TidalSoundPackSource] which is a built-in pack rooted at [TidalSoundPackSource.PACK_ROOT]
 *
 * The manifest `assets/tidal/sounds/external_sounds.json` gives a SHA-1 per file and a file is
 * downloaded only when missing or hash-mismatched so later launches work offline
 */
object ExternalSounds {
    private val LOGGER = LogManager.getLogger("Tidal/Sounds")
    private const val MANIFEST = "/assets/tidal/sounds/external_sounds.json"

    /** Where downloaded oggs live inside the pack `<PACK_ROOT>/assets/tidal/sounds/<rel>` */
    private val assetsRoot: Path get() = TidalSoundPackSource.PACK_ROOT.resolve("assets/tidal/sounds")

    private val started = AtomicBoolean(false)

    fun ensureDownloaded() {
        if (!started.compareAndSet(false, true)) return
        try {
            writePackMeta()
        } catch (t: Throwable) {
            LOGGER.warn("Failed to write sound pack metadata", t)
        }
        Multithreading.submit {
            try {
                val changed = download()
                if (changed) {
                    val mc = Minecraft.getInstance()
                    mc.execute {
                        try {
                            mc.reloadResourcePacks()
                        } catch (t: Throwable) {
                            LOGGER.warn("Failed to reload resources after downloading sounds", t)
                        }
                    }
                }
            } catch (t: Throwable) {
                LOGGER.warn("Failed to prepare downloaded sounds", t)
            }
        }
    }

    private fun download(): Boolean {
        val manifest = javaClass.getResourceAsStream(MANIFEST)?.use {
            JsonUtils.parse(it.readBytes().toString(Charsets.UTF_8)).asJsonObject
        } ?: run {
            LOGGER.warn("Missing sound manifest $MANIFEST")
            return false
        }

        val baseUrl = manifest["baseUrl"].asString
        val files = manifest.getAsJsonObject("files")
        var changed = false

        for ((rel, value) in files.entrySet()) {
            val entry = value as JsonObject
            val sha1 = entry["sha1"].asString
            val target = assetsRoot.resolve(rel)

            if (Files.exists(target) && sha1Of(target).equals(sha1, ignoreCase = true)) continue

            Files.createDirectories(target.parent)
            val url = baseUrl + rel
            LOGGER.info("Downloading sound {} -> {}", url, target)
            if (!NetworkUtils.downloadFile(url, target, NetworkUtils.DEF_AGENT, 30000, false)) {
                LOGGER.warn("Failed to download sound from {}", url)
                continue
            }
            if (!sha1Of(target).equals(sha1, ignoreCase = true)) {
                LOGGER.warn("Hash mismatch for downloaded sound {}, discarding", rel)
                runCatching { Files.deleteIfExists(target) }
                continue
            }
            changed = true
        }
        return changed
    }

    private fun writePackMeta() {
        val meta = TidalSoundPackSource.PACK_ROOT.resolve("pack.mcmeta")
        Files.createDirectories(meta.parent)
        Files.write(meta, PACK_META.toByteArray(Charsets.UTF_8))
    }

    private const val PACK_META =
        """{"pack":{"description":"Tidal downloaded sounds","pack_format":64,"supported_formats":{"min_inclusive":0,"max_inclusive":2147483647},"min_format":0,"max_format":2147483647}}"""

    private fun sha1Of(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-1")
        Files.newInputStream(path).use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
