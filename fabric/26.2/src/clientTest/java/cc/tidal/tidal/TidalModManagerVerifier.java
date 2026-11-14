/*
 * tidal — proprietary source-viewing license
 * maintained by graphine studios
 * see LICENSE and CREDITS.md for licensing and attribution.
 *
 * provided here for inspection only.
 * this notice does not claim ownership of other contributors' material.
 * no general permission is granted by graphine studios to reuse, copy,
 * modify, redistribute or create derivative works from this file.
 */

package cc.tidal.tidal;

import cc.tidal.tidal.api.TidalRuntimeToggle;
import com.mojang.blaze3d.platform.NativeImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TidalModManagerVerifier {
    private TidalModManagerVerifier() {}

    static void verify() throws Exception {
        TidalData.recordFrame();
        Thread.sleep(20);
        TidalData.recordFrame();
        require(!"0.0 ms".equals(TidalData.value("frame_time")),
                "Frame-time HUD value did not update from rendered-frame samples");

        require(TidalModManager.isSafeIconPath("assets/example/icon.png"),
                "Safe relative mod icon path was rejected");
        require(!TidalModManager.isSafeIconPath("../outside.png"),
                "Parent-relative mod icon path was accepted");
        require(!TidalModManager.isSafeIconPath("icons/../../outside.png"),
                "Normalized traversal mod icon path was accepted");
        require(!TidalModManager.isSafeIconPath("C:/outside.png"),
                "Absolute mod icon path was accepted");
        require(!TidalModManager.isSafeIconPath("icons\\outside.png"),
                "Backslash mod icon path was accepted");
        require(!TidalModManager.isSafeIconPath(""),
                "Empty mod icon path was accepted");
        verifyIconDecoding();

        TidalModManager.ModEntry searchable = new TidalModManager.ModEntry(null,
                "Example Client Mod", "example-client", "2.4.1", "Adds useful overlays.",
                null, null, null);
        require(TidalModManager.matches(searchable, "client overlays"),
                "Tokenized mod search missed metadata");
        require(TidalModManager.matches(searchable, "2.4.1"),
                "Mod search missed version metadata");
        require(!TidalModManager.matches(searchable, "server map"),
                "Mod search accepted unrelated tokens");
        require(new TidalModManager.ModEntry(null, "Unsupported", "unsupported",
                "1", "", null, null, null).status().equals("Restart required"),
                "Unsupported mod status was not explicit");

        FakeTidalToggle tidalToggle = new FakeTidalToggle();
        TidalModManager.ModEntry tidalEntry = new TidalModManager.ModEntry(null,
                "Tidal API Mod", "tidal-api-mod", "1", "",
                TidalModManager.tidalToggleAccess(tidalToggle), null, null);
        TidalModManager.requestToggle(tidalEntry);
        require(tidalEntry.pending && tidalEntry.pendingEnabled,
                "Cooperative toggle did not expose a pending target state");
        Thread.sleep(150);
        TidalModManager.tick();
        require(tidalToggle.enabled && tidalEntry.enabled && !tidalEntry.pending,
                "Tidal cooperative toggle did not apply on the client tick");

        FakeTidalToggle failingToggle = new FakeTidalToggle();
        failingToggle.fail = true;
        TidalModManager.ModEntry failingEntry = new TidalModManager.ModEntry(null,
                "Failing Mod", "failing-mod", "1", "",
                TidalModManager.tidalToggleAccess(failingToggle), null, null);
        TidalModManager.applyToggle(failingEntry);
        require("Toggle failed".equals(failingEntry.failure) && failingEntry.canToggle(),
                "Toggle failure was hidden or prevented a safe retry");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void verifyIconDecoding() throws IOException {
        Path validIcon = Files.createTempFile("tidal-mod-icon", ".png");
        Path invalidIcon = Files.createTempFile("tidal-invalid-mod-icon", ".png");
        try {
            BufferedImage source = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
            source.setRGB(0, 0, 0xFFFF0000);
            source.setRGB(1, 0, 0xFF00FF00);
            try {
                require(ImageIO.write(source, "png", validIcon.toFile()),
                        "Could not encode verifier icon");
            } finally {
                source.flush();
            }
            Files.writeString(invalidIcon, "not an image");
            try (NativeImage decoded = TidalModManager.decodeIconImage(validIcon)) {
                require(decoded.getWidth() == 32 && decoded.getHeight() == 32,
                        "Decoded mod icon was not normalized to the cached texture size");
            }
            boolean rejected = false;
            try {
                TidalModManager.decodeIconImage(invalidIcon).close();
            } catch (IOException expected) {
                rejected = true;
            }
            require(rejected, "Invalid mod icon data was accepted");
        } finally {
            Files.deleteIfExists(validIcon);
            Files.deleteIfExists(invalidIcon);
        }
    }

    private static final class FakeTidalToggle implements TidalRuntimeToggle {
        private boolean enabled;
        private boolean fail;

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            if (fail) throw new IllegalStateException("Expected verifier failure");
            this.enabled = enabled;
        }
    }
}
