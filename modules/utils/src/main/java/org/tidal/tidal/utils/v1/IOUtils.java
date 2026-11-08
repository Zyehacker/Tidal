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

package cc.tidal.utils.v1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for I/O operations
 */
public final class IOUtils {
    private static final Logger LOGGER = LogManager.getLogger("Tidal/IO");

    private IOUtils() {
    }

    /**
     * Gets the SHA-256 hash of a file
     *
     * @param path The file to hash
     * @return The SHA-256 hash of the file
     */
    public static String getFileChecksum(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytesBuffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                digest.update(bytesBuffer, 0, bytesRead);
            }

            return convertByteArrayToHexString(digest.digest());
        } catch (Exception e) {
            LOGGER.error("Failed to get {} checksum", path.toString(), e);
        }
        return "";
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuilder stringBuffer = new StringBuilder();
        var format = HexFormat.of();
        for (byte arrayByte : arrayBytes) {
            stringBuffer.append(format.toHighHexDigit(arrayByte));
            stringBuffer.append(format.toLowHexDigit(arrayByte));
        }
        return stringBuffer.toString();
    }
}