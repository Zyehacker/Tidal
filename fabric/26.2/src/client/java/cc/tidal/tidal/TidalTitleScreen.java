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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.SharedConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

import net.fabricmc.loader.api.FabricLoader;

import java.util.Set;

final class TidalTitleScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("Tidal Title Screen");
    private static final Identifier BRAND_LOGO = Identifier.fromNamespaceAndPath(
            "tidal", "textures/gui/liquid-glass/tidal/brand/logo.png");
    private static final long FADE_DURATION_NANOS = 650_000_000L;

    private static boolean capturingVanillaActions;
    private long openedAt;
    private TitleButton singleplayerButton;
    private TitleButton multiplayerButton;
    private TitleButton profileButton;
    private TitleButton quitButton;
    private TitleButton featuresButton;
    private TitleButton cosmeticsButton;
    private TitleButton optionsButton;
    private TitleButton languageButton;

    TidalTitleScreen() {
        super(Component.literal("Tidal"));
    }

    @Override
    protected void init() {
        openedAt = System.nanoTime();
        TitleScreen vanilla = new TitleScreen();
        capturingVanillaActions = true;
        try {
            vanilla.init(width, height);
        } finally {
            capturingVanillaActions = false;
        }

        Button singleplayer = findButton(vanilla, "menu.singleplayer", "menu.playdemo");
        Button multiplayer = findButton(vanilla, "menu.multiplayer", "menu.online");
        Button options = findButton(vanilla, "menu.options");
        Button language = findButton(vanilla, "options.language");
        Button quit = findButton(vanilla, "menu.quit");

        if (singleplayer == null || multiplayer == null || options == null
                || language == null || quit == null) {
            LOGGER.error("Unable to capture all required vanilla title-screen actions "
                    + "(singleplayer={}, multiplayer={}, options={}, language={}, quit={})",
                    singleplayer != null, multiplayer != null, options != null,
                    language != null, quit != null);
        }

        Layout layout = calculateLayout();
        profileButton = addRenderableWidget(new TitleButton(layout.edge, layout.edge,
                layout.iconButtonSize, layout.iconButtonSize, "Profile", "icon_profile",
                () -> Minecraft.getInstance().gui.setScreen(new TidalProfileScreen(this))));
        profileButton.setTooltip(Tooltip.create(Component.literal("Profile")));
        quitButton = addRenderableWidget(new TitleButton(
                width - layout.edge - layout.iconButtonSize, layout.edge,
                layout.iconButtonSize, layout.iconButtonSize, "Quit game", "icon_close",
                () -> pressVanilla(quit, "quit")));
        quitButton.setTooltip(Tooltip.create(Component.literal("Quit game")));

        singleplayerButton = addRenderableWidget(new TitleButton(layout.primaryX,
                layout.primaryY, layout.primaryWidth, layout.primaryHeight, "SINGLEPLAYER", null,
                () -> pressVanilla(singleplayer, "singleplayer")));
        singleplayerButton.active = singleplayer != null && singleplayer.isActive();
        if (!singleplayerButton.active) {
            singleplayerButton.setTooltip(Tooltip.create(
                    Component.literal("Singleplayer is unavailable")));
        }
        multiplayerButton = addRenderableWidget(new TitleButton(layout.primaryX,
                layout.secondaryY, layout.primaryWidth, layout.primaryHeight, "MULTIPLAYER", null,
                () -> pressVanilla(multiplayer, "multiplayer")));
        multiplayerButton.active = multiplayer != null && multiplayer.isActive();
        if (!multiplayerButton.active) {
            multiplayerButton.setTooltip(Tooltip.create(
                    Component.literal("Multiplayer is unavailable")));
        }

        featuresButton = addRenderableWidget(new TitleButton(layout.navStart, layout.bottomY,
                layout.navWidth, layout.iconButtonSize, "Features", "icon_features",
                () -> Minecraft.getInstance().gui.setScreen(new TidalScreen(this))));
        cosmeticsButton = addRenderableWidget(new TitleButton(
                layout.navStart + layout.navStep, layout.bottomY, layout.navWidth,
                layout.iconButtonSize, "Coming soon", "icon_cosmetics", () -> {}));
        cosmeticsButton.active = false;
        cosmeticsButton.setTooltip(Tooltip.create(Component.literal("Coming soon")));
        optionsButton = addRenderableWidget(new TitleButton(
                layout.navStart + layout.navStep * 2, layout.bottomY, layout.navWidth,
                layout.iconButtonSize, "Options", "icon_options",
                () -> pressVanilla(options, "options")));
        languageButton = addRenderableWidget(new TitleButton(
                layout.navStart + layout.navStep * 3, layout.bottomY, layout.navWidth,
                layout.iconButtonSize, "Language", "icon_language",
                () -> pressVanilla(language, "language")));
        featuresButton.setTooltip(Tooltip.create(Component.literal("Features")));
        optionsButton.setTooltip(Tooltip.create(Component.translatable("menu.options")));
        languageButton.setTooltip(Tooltip.create(Component.translatable("options.language")));
        setInitialFocus(singleplayerButton);
    }

    static boolean isCapturingVanillaActions() {
        return capturingVanillaActions;
    }

    private static Button findButton(TitleScreen screen, String... translationKeys) {
        Set<String> keys = Set.of(translationKeys);
        for (var child : screen.children()) {
            if (child instanceof Button button
                    && button.getMessage().getContents() instanceof TranslatableContents text
                    && keys.contains(text.getKey())) {
                return button;
            }
        }
        return null;
    }

    private static void pressVanilla(Button button, String action) {
        if (button == null) {
            LOGGER.error("Vanilla title-screen action '{}' is unavailable", action);
            return;
        }
        if (!button.isActive()) return;
        button.onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float delta) {
        float fade = Math.min(1.0f,
                (System.nanoTime() - openedAt) / (float) FADE_DURATION_NANOS);
        TidalUi.setFrameAlpha(fade);
        extractPanorama(graphics, delta);
        graphics.fill(0, 0, width, height,
                TidalUi.withAlpha(0x00070A12, Math.round(89 * fade)));

        Layout layout = calculateLayout();
        int centerX = width / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BRAND_LOGO,
                centerX - layout.logoSize / 2, layout.logoY, 0, 0,
                layout.logoSize, layout.logoSize, 1920, 1920, 1920, 1920,
                TidalUi.withAlpha(0xFFFFFFFF, Math.round(fade * 255)));
        drawSpacedText(graphics, Minecraft.getInstance().font, "TIDAL",
                centerX, layout.titleY, 0xFFFFFFFF, 1.6f, true);

        TidalUi.drawAsset(graphics, "components/menu_card", layout.cardX, layout.cardY,
                layout.cardWidth, layout.cardHeight);
        int cardLogoSize = Math.min(layout.cardLogoSize, Math.max(1, layout.cardHeight / 3));
        int contentHeight = cardLogoSize + layout.cardContentGap
                + Minecraft.getInstance().font.lineHeight;
        int contentY = layout.cardY + (layout.cardHeight - contentHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BRAND_LOGO,
                layout.cardX + (layout.cardWidth - cardLogoSize) / 2,
                contentY, 0, 0, cardLogoSize, cardLogoSize,
                1920, 1920, 1920, 1920,
                TidalUi.withAlpha(0xFFFFFFFF, Math.round(fade * 255)));
        drawSpacedText(graphics, Minecraft.getInstance().font, "TIDAL",
                layout.cardX + layout.cardWidth / 2,
                contentY + cardLogoSize + layout.cardContentGap,
                TidalUi.withAlpha(0xFFEAF4FF, Math.round(fade * 255)), 1.0f, true);

        String tidalVersion = FabricLoader.getInstance().getModContainer("tidal")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        if (tidalVersion.equals("1.0.0-beta")) tidalVersion = "1.0 Beta";
        String version = "Tidal " + tidalVersion + " \u00b7 Minecraft "
                + SharedConstants.getCurrentVersion().name();
        TidalTextRenderer.draw(graphics, Minecraft.getInstance().font, version,
                layout.edge, layout.copyrightY,
                TidalUi.withAlpha(0xFFA8B0BD, Math.round(fade * 180)));
        String copyright = "Copyright Mojang AB. Do not distribute!";
        int copyrightWidth = TidalUi.textWidth(Minecraft.getInstance().font, copyright);
        int copyrightX = Math.max(layout.edge, width - layout.edge - copyrightWidth);
        drawFitText(graphics, Minecraft.getInstance().font, copyright, copyrightX,
                layout.copyrightY, width - layout.edge - copyrightX,
                TidalUi.withAlpha(0xFFA8B0BD, Math.round(fade * 180)), false);

        try {
            super.extractRenderState(graphics, mouseX, mouseY, delta);
        } finally {
            TidalUi.setFrameAlpha(1.0f);
        }
    }

    private Layout calculateLayout() {
        Font font = Minecraft.getInstance().font;
        int edge = Math.max(physicalPixels(12), Math.round(Math.min(width, height) * 0.025f));
        int iconButtonSize = physicalPixels(32);
        int logoSize = Math.max(1, Math.round(height * 0.11f));
        int titleGap = physicalPixels(8);
        int headingButtonsGap = physicalPixels(12);
        int buttonGap = physicalPixels(16);
        int buttonHeight = physicalPixels(32);
        int buttonWidth = Math.max(1, Math.min(Math.round(width * 0.21f), width - edge * 2));
        int groupHeight = logoSize + titleGap + font.lineHeight + headingButtonsGap
                + buttonHeight * 2 + buttonGap;
        int logoY = Math.max(0, (height - groupHeight) / 2);
        int titleY = logoY + logoSize + titleGap;
        int primaryY = titleY + font.lineHeight + headingButtonsGap;
        int navGap = physicalPixels(16);
        int navWidth = Math.max(1, Math.min(iconButtonSize,
                (width - edge * 2 - navGap * 3) / 4));
        int navStep = navWidth + navGap;
        int navStart = (width - (navWidth * 4 + navGap * 3)) / 2;
        int bottomY = height - edge - iconButtonSize;
        int cardWidth = Math.max(1, Math.round(width * 0.11f));
        int cardHeight = Math.max(1, Math.round(cardWidth * 94.0f / 220.0f));
        int copyrightY = height - edge - font.lineHeight;
        int cardY = copyrightY - physicalPixels(4) - cardHeight;
        return new Layout(edge, iconButtonSize, logoSize, logoY, titleY,
                width / 2 - buttonWidth / 2, primaryY, primaryY + buttonHeight + buttonGap,
                buttonWidth, buttonHeight, navStart, navStep, navWidth, bottomY,
                width - edge - cardWidth, cardY, cardWidth, cardHeight,
                physicalPixels(30), physicalPixels(2), copyrightY);
    }

    private static int physicalPixels(int pixels) {
        var window = Minecraft.getInstance().getWindow();
        int framebufferHeight = window.getHeight();
        if (framebufferHeight <= 0) return pixels;
        return Math.max(1, Math.round(pixels * window.getGuiScaledHeight()
                / (float) framebufferHeight));
    }

    private record Layout(int edge, int iconButtonSize, int logoSize, int logoY, int titleY,
                          int primaryX, int primaryY, int secondaryY, int primaryWidth,
                          int primaryHeight, int navStart, int navStep, int navWidth,
                          int bottomY, int cardX, int cardY, int cardWidth, int cardHeight,
                          int cardLogoSize, int cardContentGap, int copyrightY) {}

    private static void drawSpacedText(GuiGraphicsExtractor graphics, Font font, String text,
                                       int centerX, int y, int color, float spacing,
                                       boolean bold) {
        int[] codePoints = text.codePoints().toArray();
        float totalWidth = spacing * Math.max(0, codePoints.length - 1);
        for (int codePoint : codePoints) {
            totalWidth += TidalUi.textWidth(font, new String(Character.toChars(codePoint)));
        }
        float x = centerX - totalWidth / 2.0f;
        for (int codePoint : codePoints) {
            String glyph = new String(Character.toChars(codePoint));
            TidalTextRenderer.draw(graphics, font, glyph, x, y, color, bold);
            x += TidalUi.textWidth(font, glyph) + spacing;
        }
    }

    private static void drawFitText(GuiGraphicsExtractor graphics, Font font, String text,
                                    int x, int y, int width, int color, boolean bold) {
        int textWidth = TidalUi.textWidth(font, text);
        if (textWidth <= width || textWidth <= 0) {
            TidalTextRenderer.draw(graphics, font, text, x, y, color, bold);
            return;
        }
        float scale = Math.max(0.1f, width / (float) textWidth);
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x * (1.0f - scale), 0);
        pose.scale(scale, 1.0f);
        TidalTextRenderer.draw(graphics, font, text, x, y, color, bold);
        pose.popMatrix();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {}

    private static final class TitleButton extends TidalClickableWidget {
        private final String icon;
        private final boolean iconAvailable;
        private final Runnable action;
        private float hover;

        private TitleButton(int x, int y, int width, int height, String message,
                            String icon, Runnable action) {
            super(x, y, width, height, Component.literal(message));
            this.icon = icon;
            this.iconAvailable = icon == null
                    || LiquidGlassAssets.contains("icons/ui/" + icon);
            if (icon != null && !iconAvailable) {
                LOGGER.error("Missing Tidal title-screen icon asset: icons/ui/{}", icon);
            }
            this.action = action;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX,
                                                int mouseY, float delta) {
            hover = TidalMotion.approach(hover,
                    active && isHoveredOrFocused() ? 1.0f : 0.0f, 14.0f);
            String surface = icon == null
                    ? "components/menu_button" : "components/menu_icon_button";
            String hoveredSurface = icon == null
                    ? "components/menu_button_hover" : "components/menu_icon_button_hover";
            int surfaceAlpha = active ? 255 : 128;
            TidalUi.drawAsset(graphics, surface, getX(), getY(), getWidth(), getHeight(),
                    TidalUi.withAlpha(0xFFFFFFFF, surfaceAlpha));
            if (active && hover > 0.01f) {
                TidalUi.drawAsset(graphics, hoveredSurface, getX(), getY(),
                        getWidth(), getHeight(), TidalUi.withAlpha(0xFFFFFFFF,
                                Math.round(hover * 255)));
            }

            if (icon != null) {
                if (iconAvailable) {
                    int iconSize = Math.min(physicalPixels(18),
                            Math.min(getWidth() - 4, getHeight() - 4));
                    if (iconSize > 0) {
                        int color = TidalUi.withAlpha(active ? 0xFFE6EDF8 : 0xFF828892,
                                Math.round(TidalUi.frameAlpha() * 255));
                        TidalUi.drawAsset(graphics, "icons/ui/" + icon,
                                getX() + (getWidth() - iconSize) / 2,
                                getY() + (getHeight() - iconSize) / 2,
                                iconSize, iconSize, color);
                    }
                }
            } else {
                Font font = Minecraft.getInstance().font;
                String label = getMessage().getString();
                float textWidth = 0.45f * Math.max(0, label.length() - 1);
                for (int i = 0; i < label.length(); i++) {
                    textWidth += TidalUi.textWidth(font, String.valueOf(label.charAt(i)));
                }
                float scale = Math.min(1.0f, Math.max(0.1f,
                        (getHeight() - 1.0f) / font.lineHeight));
                if (textWidth > getWidth() - 6) {
                    scale = Math.min(scale, Math.max(0.1f, (getWidth() - 6) / textWidth));
                }
                var pose = graphics.pose();
                pose.pushMatrix();
                float centerX = getX() + getWidth() / 2.0f;
                float centerY = getY() + getHeight() / 2.0f;
                pose.translate(centerX * (1.0f - scale), centerY * (1.0f - scale));
                pose.scale(scale, scale);
                drawSpacedText(graphics, font, label, getX() + getWidth() / 2,
                        getY() + (getHeight() - font.lineHeight) / 2,
                        TidalUi.withAlpha(active ? 0xFFF0F4FA : 0xFF898D94,
                                Math.round(TidalUi.frameAlpha() * 255)),
                        0.45f, false);
                pose.popMatrix();
            }
        }

        @Override
        protected void onActivated(MouseButtonEvent event, boolean doubleClick) {
            if (!active) return;
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
