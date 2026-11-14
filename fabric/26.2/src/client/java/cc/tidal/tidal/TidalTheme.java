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

import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.FontDescription;

final class TidalTheme {
    static final int DEFAULT_ACCENT = 0xFF2B52FB;
    static final int DEFAULT_ACCENT_HOVER = 0xFF3B60FF;
    static final int DEFAULT_ACCENT_PRESSED = 0xFF2243D6;

    static final class Spacing {
        static final int XS = 4;
        static final int SM = 8;
        static final int MD = 12;
        static final int LG = 16;
        static final int XL = 24;
        static final int XXL = 32;

        private Spacing() {}
    }

    static final class Radius {
        static final int WINDOW = 14;
        static final int PANEL = 8;
        static final int CARD = 8;
        static final int CONTROL = 6;
        static final int PILL = 6;
        static final int TOGGLE = 999;

        private Radius() {}
    }

    static final class Elevation {
        static final int NONE = 0;
        static final int RAISED = 1;
        static final int OVERLAY = 2;
        static final int MODAL = 3;

        private Elevation() {}
    }

    static final class Motion {
        static final float HOVER_STIFFNESS = 220.0f;
        static final float CONTROL_STIFFNESS = 260.0f;
        static final float PANEL_STIFFNESS = 190.0f;
        static final float SPRING_DAMPING = 0.82f;
        static final int MENU_DURATION_MILLIS = 210;

        private Motion() {}
    }

    static final class Glass {
        static final int SURFACE_OPACITY = 224;
        static final int SIDEBAR_OPACITY = 240;
        static final int OVERLAY_OPACITY = 235;
        static final int SHADOW_OPACITY = 72;
        static final int BLUR_RADIUS = 8;

        private Glass() {}
    }

    // Theme values and GUI texture tint colors use Minecraft's ARGB integer format.
    static int BACKGROUND = 0xE011141C;
    static int SURFACE = 0xF00B0D12;
    static int ELEVATED = 0xFF181B24;
    static int INTERACTIVE = 0xFF232836;
    static int HOVER = 0xFF1E222E;
    static int PRIMARY = 0xFFF3F5F8;
    static int SECONDARY = 0xFF8D95A5;
    static int MUTED = 0xFF596070;
    static int DISABLED = 0xFF596070;
    static int BORDER = 0x14FFFFFF;
    static int BORDER_ELEVATED = 0x24FFFFFF;
    static int ACCENT = DEFAULT_ACCENT;
    static int ACCENT_HOVER = DEFAULT_ACCENT_HOVER;
    static int ACCENT_PRESSED = DEFAULT_ACCENT_PRESSED;
    static final int SUCCESS = 0xFF22C55E;
    static final int WARNING = 0xFFF59E0B;
    static final int ERROR = 0xFFEF4444;
    static boolean light;
    static final Identifier FONT = Identifier.fromNamespaceAndPath("tidal", "ui");
    static final FontDescription FONT_DESCRIPTION = new FontDescription.Resource(FONT);

    private TidalTheme() {}

    static void apply(boolean useLight, int accent) {
        light = useLight;
        BACKGROUND = useLight ? 0xFFF2F5F9 : 0xE011141C;
        SURFACE = useLight ? 0xFFFFFFFF : 0xF00B0D12;
        ELEVATED = useLight ? 0xFFF8FAFC : 0xFF181B24;
        INTERACTIVE = useLight ? 0xFFE9EEF5 : 0xFF232836;
        HOVER = useLight ? 0xFFDEE6F0 : 0xFF1E222E;
        PRIMARY = useLight ? 0xFF17202D : 0xFFF3F5F8;
        SECONDARY = useLight ? 0xFF465466 : 0xFF8D95A5;
        MUTED = useLight ? 0xFF657387 : 0xFF596070;
        DISABLED = useLight ? 0xFF9AA5B4 : 0xFF5C6373;
        BORDER = useLight ? 0xFFD5DDE8 : 0x14FFFFFF;
        BORDER_ELEVATED = useLight ? 0xFFB8C4D3 : 0x24FFFFFF;
        ACCENT = accent;
        ACCENT_HOVER = accent == DEFAULT_ACCENT ? DEFAULT_ACCENT_HOVER
                : mix(accent, 0xFFFFFFFF, useLight ? 0.12f : 0.16f);
        ACCENT_PRESSED = accent == DEFAULT_ACCENT ? DEFAULT_ACCENT_PRESSED
                : mix(accent, 0xFF000000, 0.12f);
        TidalUi.applyThemePalette();
    }

    private static int mix(int first, int second, float amount) {
        int red = Math.round(((first >>> 16) & 255) * (1 - amount)
                + ((second >>> 16) & 255) * amount);
        int green = Math.round(((first >>> 8) & 255) * (1 - amount)
                + ((second >>> 8) & 255) * amount);
        int blue = Math.round((first & 255) * (1 - amount) + (second & 255) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
