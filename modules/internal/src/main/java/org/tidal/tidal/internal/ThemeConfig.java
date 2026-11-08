package cc.tidal.internal;

import org.tidal.compose.render.PolyColor;
import cc.tidal.api.config.v1.Config;
import cc.tidal.api.config.v1.annotations.Color;
import cc.tidal.api.config.v1.annotations.Switch;
import cc.tidal.api.config.v1.annotations.Text;

public class ThemeConfig extends Config {
    @Text(title = "Active Theme")
    public static String activeTheme = "TidalGlass Dark";

    @Color(title = "Accent Color", description = "The accent color of the Tidal Interface", icon = "paintbrush")
    public static PolyColor accentColor = new PolyColor(0xFF00E5FF);

    @Switch(title = "Animations", description = "Smooth, clean animations in the Tidal Interface", icon = "refresh")
    public static boolean animations = true;

    public ThemeConfig() {
        super("themes.json", "assets/tidal/brand/tidal-icon.svg", "Themes", Category.QOL);
    }

    @Override
    protected void initialize(boolean byConfigManager) {
        super.initialize(byConfigManager);
        hideIf("activeTheme", () -> true);
    }
}
