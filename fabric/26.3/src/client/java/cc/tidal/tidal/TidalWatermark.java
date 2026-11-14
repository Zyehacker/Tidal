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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

final class TidalWatermark {
    private static final Identifier LOGO = Identifier.fromNamespaceAndPath(
            "tidal", "textures/gui/liquid-glass/tidal/brand/logo.png");

    private TidalWatermark() {}

    static boolean supports(String screenName) {
        return screenName.equals("PauseScreen") || screenName.equals("InventoryScreen")
                || screenName.equals("DeathScreen");
    }

    static void render(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        int x = 10;
        int y = client.getWindow().getGuiScaledHeight() - 31;
        int logoSize = 22;
        graphics.blit(RenderPipelines.GUI_TEXTURED, LOGO, x, y, 0, 0,
                logoSize, logoSize, 1920, 1920, 1920, 1920, 0xA6FFFFFF);
        TidalUi.strongLabel(graphics, client.font, "TIDAL", x + logoSize + 7, y + 1,
                TidalUi.withAlpha(TidalUi.PRIMARY, 190));
        TidalUi.label(graphics, client.font, "by Graphine Studios", x + logoSize + 7,
                y + 14, TidalUi.withAlpha(TidalUi.SECONDARY, 170));
    }
}
