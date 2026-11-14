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

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class TidalLoadingOverlay extends AbstractWidget {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            "tidal", "textures/gui/loading/loadingbg.png");
    private static final int TEXTURE_WIDTH = 1920;
    private static final int TEXTURE_HEIGHT = 1017;
    private final String label;
    private final Screen loadingScreen;
    private long readyAtNanos;

    TidalLoadingOverlay(int width, int height, Screen loadingScreen) {
        super(0, 0, width, height, Component.literal("Tidal loading overlay"));
        this.loadingScreen = loadingScreen;
        String screenName = loadingScreen.getClass().getSimpleName();
        this.label = screenName.equals("ConnectScreen") ? "Connecting"
                : screenName.equals("ReceivingLevelScreen")
                ? "Joining world" : "Loading terrain";
        active = false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX,
                                            int mouseY, float delta) {
        float alpha = 1.0f;
        if (loadingScreen instanceof LevelLoadingScreen levelLoading
                && ((cc.tidal.tidal.mixin.TidalLevelLoadingScreenAccessor) levelLoading)
                .tidal$getLoadTracker().isLevelReady()) {
            if (readyAtNanos == 0) readyAtNanos = System.nanoTime();
            alpha = Math.max(0.0f, 1.0f
                    - (System.nanoTime() - readyAtNanos) / 300_000_000.0f);
        } else {
            readyAtNanos = 0;
        }
        double sourceAspect = TEXTURE_WIDTH / (double) TEXTURE_HEIGHT;
        double targetAspect = getWidth() / (double) Math.max(1, getHeight());
        int sourceX = 0;
        int sourceY = 0;
        int sourceWidth = TEXTURE_WIDTH;
        int sourceHeight = TEXTURE_HEIGHT;
        if (sourceAspect > targetAspect) {
            sourceWidth = (int) Math.round(TEXTURE_HEIGHT * targetAspect);
            sourceX = (TEXTURE_WIDTH - sourceWidth) / 2;
        } else {
            sourceHeight = (int) Math.round(TEXTURE_WIDTH / targetAspect);
            sourceY = (TEXTURE_HEIGHT - sourceHeight) / 2;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                0, 0, sourceX, sourceY, getWidth(), getHeight(),
                sourceWidth, sourceHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TidalUi.withAlpha(0xFFFFFFFF, Math.round(alpha * 255)));
        graphics.fill(0, 0, getWidth(), getHeight(),
                TidalUi.withAlpha(0x42000000, Math.round(alpha * 0x42)));
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        TidalUi.centeredLabel(graphics, font, "TIDAL", centerX, centerY - 25,
                TidalUi.withAlpha(0xFFFFFFFF, Math.round(alpha * 255)));
        TidalUi.centeredLabel(graphics, font, label, centerX, centerY - 7,
                TidalUi.withAlpha(0xFFE0E6F3, Math.round(alpha * 255)));

        int trackWidth = 96;
        int trackX = centerX - trackWidth / 2;
        int trackY = centerY + 18;
        graphics.fill(trackX, trackY, trackX + trackWidth, trackY + 2,
                TidalUi.withAlpha(0x805FFFFF, Math.round(alpha * 0x80)));
        if (loadingScreen instanceof LevelLoadingScreen levelLoading
                && ((cc.tidal.tidal.mixin.TidalLevelLoadingScreenAccessor) levelLoading)
                .tidal$getLoadTracker().hasProgress()) {
            float progress = Math.max(0.0f, Math.min(1.0f,
                    ((cc.tidal.tidal.mixin.TidalLevelLoadingScreenAccessor) levelLoading)
                            .tidal$getLoadTracker().serverProgress()));
            graphics.fill(trackX, trackY, trackX + Math.round(trackWidth * progress),
                    trackY + 2,
                    TidalUi.withAlpha(TidalTheme.ACCENT, Math.round(alpha * 255)));
            TidalUi.centeredLabel(graphics, font, Math.round(progress * 100) + "%",
                    centerX, trackY + 7,
                    TidalUi.withAlpha(0xFFE0E6F3, Math.round(alpha * 255)));
        } else {
            int segmentWidth = 24;
            int travel = trackWidth - segmentWidth;
            int position = (int) ((System.nanoTime() / 6_000_000L) % (travel * 2L));
            int segmentX = trackX + (position <= travel ? position : travel * 2 - position);
            graphics.fill(segmentX, trackY, segmentX + segmentWidth, trackY + 2,
                    TidalUi.withAlpha(TidalTheme.ACCENT, Math.round(alpha * 255)));
        }
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
