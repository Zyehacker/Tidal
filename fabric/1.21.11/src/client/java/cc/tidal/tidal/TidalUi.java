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

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.FormattedCharSequence;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

final class TidalUi {
    static int BACKGROUND = TidalTheme.BACKGROUND;
    static int SURFACE = TidalTheme.SURFACE;
    static int ELEVATED = TidalTheme.ELEVATED;
    static int INTERACTIVE = TidalTheme.INTERACTIVE;
    static int HOVER = TidalTheme.HOVER;
    static int PRIMARY = TidalTheme.PRIMARY;
    static int SECONDARY = TidalTheme.SECONDARY;
    static int MUTED = TidalTheme.MUTED;
    static int DISABLED = TidalTheme.DISABLED;
    static int ACCENT = TidalTheme.ACCENT;
    static int ACCENT_HOVER = TidalTheme.ACCENT_HOVER;
    static int ACCENT_PRESSED = TidalTheme.ACCENT_PRESSED;
    static final int SUCCESS = TidalTheme.SUCCESS;
    static final int WARNING = TidalTheme.WARNING;
    static final int ERROR = TidalTheme.ERROR;
    static final int CARD_CONTENT_INSET =
            LiquidGlassAssets.require("tidal/ui/polyglass_module_card").safeArea().left();
    static final int PLANNED_BADGE_WIDTH = 48;
    private static int BORDER = TidalTheme.BORDER;
    private static int BORDER_ELEVATED = TidalTheme.BORDER_ELEVATED;
    private static int BORDER_FOCUS = ACCENT;
    private static final Map<String, Component> STRONG_TEXT = new HashMap<>();
    private static String wrappedText;
    private static int wrappedWidth;
    private static List<String> wrappedLines = List.of();
    private static float frameAlpha = 1.0f;

    private TidalUi() {}

    static void applyThemePalette() {
        BACKGROUND = TidalTheme.BACKGROUND;
        SURFACE = TidalTheme.SURFACE;
        ELEVATED = TidalTheme.ELEVATED;
        INTERACTIVE = TidalTheme.INTERACTIVE;
        HOVER = TidalTheme.HOVER;
        PRIMARY = TidalTheme.PRIMARY;
        SECONDARY = TidalTheme.SECONDARY;
        MUTED = TidalTheme.MUTED;
        DISABLED = TidalTheme.DISABLED;
        ACCENT = TidalTheme.ACCENT;
        ACCENT_HOVER = TidalTheme.ACCENT_HOVER;
        ACCENT_PRESSED = TidalTheme.ACCENT_PRESSED;
        BORDER = TidalTheme.BORDER;
        BORDER_ELEVATED = TidalTheme.BORDER_ELEVATED;
        BORDER_FOCUS = ACCENT;
    }

    static long nowMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    static void setFrameAlpha(float alpha) {
        frameAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    static float frameAlpha() { return frameAlpha; }

    static void drawAsset(GuiGraphics graphics, String path, int x, int y,
                          int width, int height) {
        drawAsset(graphics, path, x, y, width, height, 0xFFFFFFFF);
    }

    static void drawIcon(GuiGraphics graphics, String name, int x, int y, int tint) {
        String polyGlassIcon = polyGlassUiIcon(name);
        drawAsset(graphics, polyGlassIcon == null ? "icons/" + name : polyGlassIcon,
                x, y, 16, 16, tint);
    }

    static String moduleIconPath(TidalModule module) {
        String type = module.group == TidalModule.Group.HUD ? "hud" : "gameplay";
        return "tidal/modules/" + type + "/" + module.id;
    }

    static void drawAsset(GuiGraphics graphics, String path, int x, int y,
                          int width, int height, int tint) {
        if (width <= 0 || height <= 0) return;
        tint = withOpacity(tint, frameAlpha);
        LiquidGlassAssets.Asset asset = LiquidGlassAssets.require(path);
        if (asset.scaling() == LiquidGlassAssets.Scaling.FIXED
                && (width != asset.width() || height != asset.height())) {
            throw new IllegalArgumentException("Fixed texture " + path + " must be drawn at "
                    + asset.width() + "x" + asset.height());
        }
        if (asset.scaling() == LiquidGlassAssets.Scaling.STRETCH_X && height != asset.height()) {
            throw new IllegalArgumentException("Stretch-X texture " + path
                    + " must preserve its manifest height");
        }
        if (asset.scaling() == LiquidGlassAssets.Scaling.STRETCH_Y && width != asset.width()) {
            throw new IllegalArgumentException("Stretch-Y texture " + path
                    + " must preserve its manifest width");
        }
        net.minecraft.resources.Identifier texture = asset.currentColorTint()
                ? LiquidGlassAssets.texture(asset, tint) : LiquidGlassAssets.texture(asset);
        if (texture == null) return;
        if (asset.path().startsWith("tidal/")) {
            tint = (tint & 0xFF000000) | 0x00FFFFFF;
        }
        double scaleX = LiquidGlassAssets.deviceScaleX();
        double scaleY = LiquidGlassAssets.deviceScaleY();
        int deviceX = snapDevicePixel(x, scaleX);
        int deviceY = snapDevicePixel(y, scaleY);
        int deviceRight = snapDevicePixel(x + width, scaleX);
        int deviceBottom = snapDevicePixel(y + height, scaleY);
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.scale((float) (1.0 / scaleX), (float) (1.0 / scaleY));
        try {
            drawAssetDevice(graphics, asset, texture, deviceX, deviceY,
                    deviceRight - deviceX, deviceBottom - deviceY, tint);
        } finally {
            pose.popMatrix();
        }
    }

    static int snapDevicePixel(double logicalCoordinate, double deviceScale) {
        if (!Double.isFinite(logicalCoordinate) || !Double.isFinite(deviceScale)
                || deviceScale <= 0) {
            throw new IllegalArgumentException("Invalid device coordinate or scale");
        }
        return (int) Math.round(logicalCoordinate * deviceScale);
    }

    private static void drawAssetDevice(GuiGraphics graphics,
                                        LiquidGlassAssets.Asset asset,
                                        net.minecraft.resources.Identifier texture,
                                        int x, int y, int width, int height, int tint) {
        int deviceWidth = LiquidGlassAssets.deviceWidth(asset);
        int deviceHeight = LiquidGlassAssets.deviceHeight(asset);
        int sourceLeft = asset.scaledInsetX(asset.nineSlice().left(), deviceWidth);
        int sourceRight = asset.scaledInsetX(asset.nineSlice().right(), deviceWidth);
        int sourceTop = asset.scaledInsetY(asset.nineSlice().top(), deviceHeight);
        int sourceBottom = asset.scaledInsetY(asset.nineSlice().bottom(), deviceHeight);
        int left = asset.scaling() == LiquidGlassAssets.Scaling.NINE_SLICE_Y ? 0 : sourceLeft;
        int right = asset.scaling() == LiquidGlassAssets.Scaling.NINE_SLICE_Y ? 0 : sourceRight;
        int top = asset.scaling() == LiquidGlassAssets.Scaling.NINE_SLICE_X ? 0 : sourceTop;
        int bottom = asset.scaling() == LiquidGlassAssets.Scaling.NINE_SLICE_X ? 0 : sourceBottom;
        if (asset.scaling() == LiquidGlassAssets.Scaling.FIXED
                || asset.scaling() == LiquidGlassAssets.Scaling.STRETCH_X
                || asset.scaling() == LiquidGlassAssets.Scaling.STRETCH_Y) {
            blitAssetSlice(graphics, texture, deviceWidth, deviceHeight, 0, 0,
                    deviceWidth, deviceHeight, x, y, width, height, tint);
            return;
        }
        if (width < left + right || height < top + bottom) {
            throw new IllegalArgumentException("Texture " + asset.path() + " is smaller than its "
                    + "nine-slice insets at " + width + "x" + height);
        }
        for (int row = 0; row < 3; row++) {
            int sourceY = row == 0 ? 0 : row == 1 ? sourceTop : deviceHeight - sourceBottom;
            int sourceHeight = row == 0 ? sourceTop
                    : row == 1 ? deviceHeight - sourceTop - sourceBottom : sourceBottom;
            int destY = row == 0 ? y : row == 1 ? y + top : y + height - bottom;
            int destHeight = row == 0 ? top : row == 1 ? height - top - bottom : bottom;
            for (int column = 0; column < 3; column++) {
                int sourceX = column == 0 ? 0 : column == 1 ? sourceLeft
                        : deviceWidth - sourceRight;
                int sourceWidth = column == 0 ? sourceLeft
                        : column == 1 ? deviceWidth - sourceLeft - sourceRight : sourceRight;
                int destX = column == 0 ? x : column == 1 ? x + left : x + width - right;
                int destWidth = column == 0 ? left : column == 1 ? width - left - right : right;
                if (sourceWidth > 0 && sourceHeight > 0 && destWidth > 0 && destHeight > 0) {
                    blitAssetSlice(graphics, texture, deviceWidth, deviceHeight,
                            sourceX, sourceY, sourceWidth, sourceHeight,
                            destX, destY, destWidth, destHeight, tint);
                }
            }
        }
    }

    private static void blitAssetSlice(GuiGraphics graphics,
                                       net.minecraft.resources.Identifier texture,
                                       int textureWidth, int textureHeight,
                                       int u, int v, int sourceWidth, int sourceHeight,
                                       int x, int y, int width, int height, int tint) {
        if (width <= 0 || height <= 0) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height,
                sourceWidth, sourceHeight, textureWidth, textureHeight, tint);
    }

    static void panel(GuiGraphics graphics, int x, int y, int width, int height,
                      int color, int radius) {
        if (width <= 0 || height <= 0) return;
        if (radius <= 1 || width < 22 || height < 22) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }
        if (TidalTheme.light) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }
        drawAsset(graphics, "tidal/ui/polyglass_module_card", x, y, width, height);
    }

    static void cleanCard(GuiGraphics graphics, int x, int y, int width, int height) {
        cleanCard(graphics, x, y, width, height, false);
    }

    static void cleanCard(GuiGraphics graphics, int x, int y, int width, int height,
                          boolean hovered) {
        drawAsset(graphics, hovered ? "tidal/ui/polyglass_card_hover"
                        : "tidal/ui/polyglass_card",
                x, y, width, height,
                withAlpha(0xFFFFFFFF, TidalTheme.Glass.SURFACE_OPACITY));
    }

    private static String polyGlassUiIcon(String name) {
        return switch (name) {
            case "search" -> "tidal/ui/search";
            case "close" -> "tidal/ui/close";
            case "left", "back", "arrow_back" -> "tidal/ui/arrow_back";
            case "right", "forward", "arrow_forward" -> "tidal/ui/arrow_forward";
            case "down", "chevron_down" -> "tidal/ui/chevron_down";
            case "settings", "settings_gear" -> "tidal/ui/settings_gear";
            case "lock", "unlock", "hud_lock" -> "tidal/ui/hud_lock";
            case "merge", "hud_group_merge" -> "tidal/ui/hud_group_merge";
            case "reset", "hud_reset" -> "tidal/ui/hud_reset";
            case "hud-position", "nav_hud_editor" -> "tidal/ui/nav_edit_hud";
            case "keybind" -> "tidal/ui/nav_keybinds";
            case "fps_boost" -> "tidal/modules/gameplay/fps_boost";
            case "pref_appearance", "pref_graphics", "pref_typography",
                    "pref_input", "pref_advanced" -> "tidal/ui/" + name;
            case "nav_edit_hud", "nav_mods", "nav_profiles", "nav_keybinds",
                    "nav_themes", "nav_preferences" -> "tidal/ui/" + name;
            case "action_back", "action_configure", "action_copy", "action_favorite_add",
                    "action_favorite_remove", "action_folder_open",
                    "action_liquid_glass_gallery", "action_project_page",
                    "action_restore_defaults" ->
                    "tidal/actions/" + name.substring("action_".length());
            default -> null;
        };
    }

    static void borderedPanel(GuiGraphics graphics, int x, int y, int width, int height,
                              int color, int radius, boolean focused) {
        panel(graphics, x, y, width, height, color, radius);
        drawAsset(graphics, focused ? "borders/focus-ring-r8"
                        : "borders/selected-outline-r8",
                x - 2, y - 2, width + 4, height + 4,
                withAlpha(focused ? BORDER_FOCUS : BORDER_ELEVATED, 180));
    }

    static void label(GuiGraphics graphics, Font font, String text, int x, int y,
                      int color) {
        TidalTextRenderer.draw(graphics, font, text, x, y, color);
    }

    static Component uiText(String text) {
        return Component.literal(text).withStyle(
                Style.EMPTY.withFont(TidalTheme.FONT_DESCRIPTION));
    }

    static Component uiText(Component text) {
        return text.copy().withStyle(style -> style.withFont(TidalTheme.FONT_DESCRIPTION));
    }

    static int textWidth(Font font, String text) {
        return TidalTextRenderer.width(text);
    }

    static int buttonWidth(Font font, String text, int horizontalPadding, int minimumWidth) {
        return Math.max(minimumWidth, textWidth(font, text) + horizontalPadding * 2);
    }

    static String fitText(Font font, String text, int maxWidth) {
        return TidalTextRenderer.fit(font, text, maxWidth);
    }

    static String displayName(Enum<?> value) {
        return displayWords(value.name());
    }

    static String displayWords(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).replace('_', ' ')
                .replace('-', ' ').split("\\s+");
        StringBuilder displayName = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!displayName.isEmpty()) displayName.append(' ');
            displayName.append(Character.toUpperCase(word.charAt(0)))
                    .append(word, 1, word.length());
        }
        return displayName.toString();
    }

    static void centeredLabel(GuiGraphics graphics, Font font, String text,
                              int centerX, int y, int color) {
        TidalTextRenderer.draw(graphics, font, text,
                centerX - textWidth(font, text) / 2.0f, y, color);
    }

    static int wrappedLabel(GuiGraphics graphics, Font font, String text,
                            int x, int y, int width, int color) {
        int boundedWidth = Math.max(1, width);
        if (!text.equals(wrappedText) || boundedWidth != wrappedWidth) {
            wrappedText = text;
            wrappedWidth = boundedWidth;
            wrappedLines = wrapText(font, text, boundedWidth);
        }
        for (int i = 0; i < wrappedLines.size(); i++) {
            TidalTextRenderer.draw(graphics, font, wrappedLines.get(i),
                    x, y + i * font.lineHeight, color);
        }
        return wrappedLines.size();
    }

    static List<String> wrapText(Font font, String text, int width) {
        List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && textWidth(font, candidate) > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.isEmpty() && textWidth(font, word) > width) {
                StringBuilder fragment = new StringBuilder();
                for (int offset = 0; offset < word.length();) {
                    int cp = word.codePointAt(offset);
                    String part = new String(Character.toChars(cp));
                    if (!fragment.isEmpty() && textWidth(font, fragment + part) > width) {
                        lines.add(fragment.toString());
                        fragment.setLength(0);
                    }
                    fragment.append(part);
                    offset += Character.charCount(cp);
                }
                line.append(fragment);
            } else if (line.isEmpty()) line.append(word);
            else line.append(' ').append(word);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return List.copyOf(lines);
    }

    static void strongLabel(GuiGraphics graphics, Font font, String text, int x, int y,
                            int color) {
        TidalTextRenderer.draw(graphics, font, text, x, y, color, true);
    }

    static int strongWidth(Font font, String text) {
        return TidalTextRenderer.width(text);
    }

    private static Component strongText(String text) {
        return STRONG_TEXT.computeIfAbsent(text,
                value -> Component.literal(value).withStyle(ChatFormatting.BOLD));
    }

    static void sectionHeader(GuiGraphics graphics, Font font, String title,
                              String detail, int x, int y, int width) {
        int textY = y + Math.max(0, (18 - font.lineHeight) / 2);
        label(graphics, font, title, x + 4, textY, PRIMARY);
        if (!detail.isEmpty()) {
            int detailX = x + width - 4 - textWidth(font, detail);
            label(graphics, font, detail, detailX, textY, SECONDARY);
        }
        panel(graphics, x, y + 18, width, 1, 0x20FFFFFF, 0);
    }

    static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    static int withOpacity(int color, float opacity) {
        float boundedOpacity = Math.max(0.0f, Math.min(1.0f, opacity));
        int sourceAlpha = (color >>> 24) & 255;
        return withAlpha(color, Math.round(sourceAlpha * boundedOpacity));
    }

    static int mix(int from, int to, float amount) {
        amount = Math.max(0.0f, Math.min(1.0f, amount));
        int a = (from >>> 24) & 255;
        int r = (from >>> 16) & 255;
        int g = (from >>> 8) & 255;
        int b = from & 255;
        int ta = (to >>> 24) & 255;
        int tr = (to >>> 16) & 255;
        int tg = (to >>> 8) & 255;
        int tb = to & 255;
        return (lerp(a, ta, amount) << 24) | (lerp(r, tr, amount) << 16)
                | (lerp(g, tg, amount) << 8) | lerp(b, tb, amount);
    }

    private static int lerp(int from, int to, float t) {
        return Math.max(0, Math.min(255, Math.round(from + (to - from) * t)));
    }

    static final class SearchField extends EditBox {
        private final int outerX;
        private final int outerY;
        private final int outerWidth;
        private final int outerHeight;
        private final boolean searchIcon;
        private boolean error;
        private Font uiFont;
        private String hintText = "";

        SearchField(Font font, int x, int y, int width, int height, Component label) {
            this(font, x, y, width, height, label, false);
        }

        SearchField(Font font, int x, int y, int width, int height, Component label,
                    boolean searchIcon) {
            super(font, x + textInsetLeft(searchIcon),
                    y + (height - Math.min(height, font.lineHeight + 2)) / 2,
                    Math.max(1, width - textInsetLeft(searchIcon) - textInsetRight()),
                    Math.min(height, font.lineHeight + 2), label);
            outerX = x;
            outerY = y;
            outerWidth = width;
            outerHeight = height;
            this.searchIcon = searchIcon;
            uiFont = font;
            setBordered(false);
            setTextColor(PRIMARY);
            setTextColorUneditable(SECONDARY);
            addFormatter((text, cursor) -> FormattedCharSequence.forward(text,
                    Style.EMPTY.withColor(PRIMARY)
                            .withFont(TidalTheme.FONT_DESCRIPTION)));
        }

        private static int textInsetLeft(boolean searchIcon) {
            int safe = LiquidGlassAssets.require("inputs/field-normal").safeArea().left();
            return safe + (searchIcon ? 20 : 0);
        }

        private static int textInsetRight() {
            return LiquidGlassAssets.require("inputs/field-normal").safeArea().right();
        }

        void setError(boolean error) {
            this.error = error;
        }

        @Override
        public void setHint(Component hint) {
            hintText = hint.getString();
            super.setHint(uiText(hint));
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= outerX && mouseX < outerX + outerWidth
                    && mouseY >= outerY && mouseY < outerY + outerHeight;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                             float delta) {
            String state = !active ? "disabled" : error ? "error" : isFocused() ? "focused"
                    : isMouseOver(mouseX, mouseY) ? "hover" : "normal";
            String texture = switch (state) {
                case "disabled" -> "inputs/field-disabled";
                case "error" -> "inputs/field-error";
                case "focused" -> "inputs/field-focused";
                case "hover" -> "inputs/field-hover";
                default -> "inputs/field-normal";
            };
            drawAsset(graphics, texture, outerX, outerY, outerWidth, outerHeight);
            if (searchIcon) {
                int iconInset = LiquidGlassAssets.require("inputs/field-normal")
                        .safeArea().left();
                drawIcon(graphics, "search", outerX + iconInset,
                        outerY + (outerHeight - 16) / 2, SECONDARY);
            }
            if (isFocused()) {
                drawAsset(graphics, "borders/focus-ring-r7", outerX - 2, outerY - 2,
                        outerWidth + 4, outerHeight + 4);
            }
            int textY = outerY + (outerHeight - uiFont.lineHeight) / 2;
            int left = getX();
            int right = getX() + getWidth();
            graphics.enableScissor(left, outerY + 2, right, outerY + outerHeight - 2);
            String value = getValue();
            if (value.isEmpty()) {
                TidalTextRenderer.draw(graphics, uiFont, hintText, left, textY, MUTED);
            } else {
                String highlighted = getHighlighted();
                if (!highlighted.isEmpty()) {
                    int start = value.indexOf(highlighted);
                    if (start >= 0) {
                        int selectionX = left + textWidth(uiFont, value.substring(0, start));
                        int selectionWidth = textWidth(uiFont, highlighted);
                        graphics.fill(selectionX, textY - 1, selectionX + selectionWidth,
                                textY + uiFont.lineHeight, withOpacity(ACCENT, 0.55f));
                    }
                }
                TidalTextRenderer.draw(graphics, uiFont, value, left, textY,
                        active ? PRIMARY : SECONDARY);
            }
            if (isFocused() && (nowMillis() / 500) % 2 == 0) {
                int cursor = Math.max(0, Math.min(getCursorPosition(), value.length()));
                String prefix = value.substring(0, cursor);
                int cursorX = left + textWidth(uiFont, prefix);
                graphics.fill(cursorX, textY - 1, cursorX + 1,
                        textY + uiFont.lineHeight, PRIMARY);
            }
            graphics.disableScissor();
        }
    }

    static class ActionButton extends AbstractWidget {
        enum Style {
            NAVIGATION, CONTROL, PRIMARY, QUIET, CLOSE, PAGER, CENTERED, DESTRUCTIVE, ICON,
            MENU_ITEM
        }

        private final Runnable action;
        private final Style style;
        private float hover;
        private float highlightProgress;
        private float clickProgress;
        private String cachedSourceLabel;
        private String cachedLabel;
        private String cachedIcon;
        private int cachedLabelWidth = -1;
        private int cachedLabelTextWidth;
        boolean highlighted;

        ActionButton(int x, int y, int width, int height, String label, Style style,
                     Runnable action) {
            super(x, y, width, height, Component.literal(label));
            this.style = style;
            this.action = action;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                                                int mouseY, float delta) {
            hover = TidalMotion.approach(hover,
                    active && isHoveredOrFocused() ? 1.0f : 0.0f, 14.0f);
            highlightProgress = TidalMotion.approach(highlightProgress,
                    highlighted ? 1.0f : 0.0f, 11.0f);
            clickProgress = TidalMotion.approach(clickProgress, 0.0f, 22.0f);

            drawButtonSurface(graphics);
            if (isFocused()) {
                drawAsset(graphics, "borders/focus-ring-r7", getX() - 2, getY() - 2,
                        getWidth() + 4, getHeight() + 4);
            }
            Font font = net.minecraft.client.Minecraft.getInstance().font;
            int textY = getY() + (getHeight() - font.lineHeight) / 2;
            String sourceLabel = getMessage().getString();
            if (!sourceLabel.equals(cachedSourceLabel)) {
                cachedSourceLabel = sourceLabel;
                cachedIcon = iconForLabel(sourceLabel);
                cachedLabel = null;
            }
            String icon = cachedIcon;
            boolean iconOnly = style == Style.CLOSE || style == Style.PAGER
                    || style == Style.ICON;
            int iconSize = 16;
            String normalTexture = style == Style.NAVIGATION || style == Style.MENU_ITEM
                    ? "tidal/ui/nav_pill_active" : buttonTexture("normal");
            LiquidGlassAssets.Insets safeArea = LiquidGlassAssets.require(normalTexture).safeArea();
            int iconGap = style == Style.MENU_ITEM ? 10 : 8;
            int horizontalInset = style == Style.MENU_ITEM ? 10
                    : icon == null ? 0 : 8;
            int availableTextWidth = icon == null ? getWidth() - safeArea.left()
                    - safeArea.right() : getWidth() - horizontalInset * 2 - iconSize - iconGap;
            if (cachedLabel == null || availableTextWidth != cachedLabelWidth) {
                cachedLabelWidth = availableTextWidth;
                cachedLabel = fitText(font, sourceLabel, Math.max(0, availableTextWidth));
                cachedLabelTextWidth = textWidth(font, cachedLabel);
            }
            if (icon != null) {
                int iconX = iconOnly ? getX() + (getWidth() - iconSize) / 2
                        : style == Style.MENU_ITEM ? getX() + horizontalInset
                        : getX() + Math.max(0,
                        (getWidth() - iconSize - iconGap - cachedLabelTextWidth) / 2);
                int iconColor = !active ? DISABLED
                        : style == Style.DESTRUCTIVE ? ERROR
                        : (style == Style.MENU_ITEM || style == Style.NAVIGATION)
                        && highlightProgress > 0.01f ? PRIMARY : SECONDARY;
                drawIcon(graphics, icon, iconX,
                        getY() + (getHeight() - iconSize) / 2, iconColor);
            }
            if (iconOnly) return;
            String label = cachedLabel;
            int color = !active ? DISABLED
                    : style == Style.QUIET || style == Style.MENU_ITEM ? SECONDARY
                    : style == Style.DESTRUCTIVE ? ERROR : PRIMARY;
            if (style == Style.MENU_ITEM || style == Style.NAVIGATION) {
                color = mix(SECONDARY, PRIMARY, Math.max(highlightProgress, hover * 0.35f));
            }
            int textX = icon == null
                    ? getX() + (getWidth() - cachedLabelTextWidth) / 2
                    : style == Style.MENU_ITEM
                    ? getX() + horizontalInset + iconSize + iconGap
                    : getX() + Math.max(0, (getWidth()
                    - iconSize - iconGap - cachedLabelTextWidth) / 2) + iconSize + iconGap;
            TidalTextRenderer.draw(graphics, font, label, textX, textY,
                    !active ? DISABLED : style == Style.DESTRUCTIVE
                            ? mix(ERROR, PRIMARY, hover * 0.35f) : mix(color, PRIMARY, hover));
        }

        private void drawButtonSurface(GuiGraphics graphics) {
            if (style == Style.NAVIGATION) {
                if (highlightProgress > 0.01f) {
                    drawAsset(graphics, "tidal/ui/nav_pill_active", getX(), getY(),
                            getWidth(), getHeight(), withAlpha(0xFFFFFFFF,
                                    Math.round(highlightProgress * 255)));
                }
                return;
            }
            if (style == Style.MENU_ITEM) {
                if (highlightProgress > 0.01f) {
                    drawAsset(graphics, "tidal/ui/nav_pill_active", getX(), getY(),
                            getWidth(), getHeight(), withAlpha(0xFFFFFFFF,
                                    Math.round(highlightProgress * 255)));
                }
                if (hover > 0.01f && highlightProgress < 0.99f) {
                    drawAsset(graphics, "tidal/ui/polyglass_card_hover", getX(), getY(),
                            getWidth(), getHeight(), withAlpha(0xFFFFFFFF,
                                    Math.round(hover * (1.0f - highlightProgress) * 180)));
                }
                return;
            }
            if (style == Style.ICON && highlighted) {
                drawAsset(graphics, "buttons/icon-button-selected",
                        getX(), getY(), getWidth(), getHeight());
                if (hover > 0.01f) {
                    drawAsset(graphics, "buttons/button-secondary-hover",
                            getX(), getY(), getWidth(), getHeight(),
                            withAlpha(0xFFFFFFFF, Math.round(hover * 255)));
                }
                return;
            }
            if (!active) {
                drawAsset(graphics, buttonTexture("normal"), getX(), getY(),
                        getWidth(), getHeight(), withAlpha(0xFFFFFFFF, 128));
                return;
            }
            drawAsset(graphics, buttonTexture("normal"), getX(), getY(),
                    getWidth(), getHeight());
            if (hover > 0.01f) {
                drawAsset(graphics, buttonTexture("hover"), getX(), getY(),
                        getWidth(), getHeight(), withAlpha(0xFFFFFFFF,
                                Math.round(hover * 255)));
            }
            if (clickProgress > 0.01f) {
                drawAsset(graphics, buttonTexture("pressed"), getX(), getY(),
                        getWidth(), getHeight(), withAlpha(0xFFFFFFFF,
                                Math.round(clickProgress * 255)));
            }
        }

        private String buttonTexture(String state) {
            return switch (style) {
                case PRIMARY -> switch (state) {
                    case "hover" -> "tidal/ui/button_primary_hover";
                    case "pressed" -> "tidal/ui/button_primary_pressed";
                    default -> "tidal/ui/button_primary";
                };
                case CONTROL -> state.equals("hover")
                        ? "tidal/ui/polyglass_card_hover"
                        : "tidal/ui/button_secondary";
                case QUIET -> switch (state) {
                    case "disabled" -> "buttons/button-ghost-disabled";
                    case "hover" -> "buttons/button-ghost-hover";
                    case "pressed" -> "buttons/button-ghost-pressed";
                    default -> "buttons/button-ghost-normal";
                };
                case DESTRUCTIVE -> switch (state) {
                    case "disabled" -> "buttons/button-destructive-disabled";
                    case "hover" -> "buttons/button-destructive-hover";
                    case "pressed" -> "buttons/button-destructive-pressed";
                    default -> "buttons/button-destructive-normal";
                };
                default -> switch (state) {
                    case "disabled" -> "buttons/button-secondary-disabled";
                    case "hover" -> "buttons/button-secondary-hover";
                    case "pressed" -> "buttons/button-secondary-pressed";
                    default -> "buttons/button-secondary-normal";
                };
            };
        }

        private String iconForLabel(String label) {
            if (style == Style.CLOSE) return "close";
            if (style == Style.PAGER) return label.equals("‹") ? "left" : "right";
            String normalized = label.toLowerCase(Locale.ROOT);
            if (normalized.startsWith("copy ")
                    || normalized.startsWith("back up configuration")) {
                return "action_copy";
            }
            if (normalized.startsWith("add favorite")) return "action_favorite_add";
            if (normalized.startsWith("remove favorite")) return "action_favorite_remove";
            if (normalized.equals("configure")) return "action_configure";
            if (normalized.equals("project page")) return "action_project_page";
            if (normalized.startsWith("open mods folder")) return "action_folder_open";
            if (normalized.equals("liquid glass gallery")) {
                return "action_liquid_glass_gallery";
            }
            if (normalized.startsWith("restore")) return "action_restore_defaults";
            if (normalized.equals("back")) return "action_back";
            if (style == Style.MENU_ITEM) {
                String preferenceIcon = switch (normalized) {
                    case "appearance" -> "pref_appearance";
                    case "graphics" -> "pref_graphics";
                    case "text & fonts" -> "pref_typography";
                    case "input" -> "pref_input";
                    case "advanced" -> "pref_advanced";
                    default -> null;
                };
                if (preferenceIcon != null) return preferenceIcon;
                String navigationIcon = switch (normalized) {
                    case "edit hud" -> "nav_edit_hud";
                    case "modules", "mods" -> "nav_mods";
                    case "settings" -> "nav_preferences";
                    default -> null;
                };
                if (navigationIcon != null) return navigationIcon;
            }
            if (normalized.equals("elements") || normalized.equals("hud studio")) {
                return "nav_edit_hud";
            }
            if (normalized.equals("layouts") || normalized.equals("profiles")) {
                return "nav_profiles";
            }
            if (normalized.equals("input") || normalized.equals("keybinds")) {
                return "nav_keybinds";
            }
            if (normalized.equals("appearance") || normalized.equals("themes")) {
                return "nav_themes";
            }
            if (normalized.equals("general") || normalized.equals("settings")
                    || normalized.equals("preferences")) {
                return "nav_preferences";
            }
            if (style == Style.NAVIGATION) {
                if (normalized.equals("hud studio")) return "nav_edit_hud";
                if (normalized.equals("modules") || normalized.equals("mods")) {
                    return "nav_mods";
                }
                if (normalized.equals("settings")) return "nav_preferences";
            }
            if (normalized.equals("profiles")) return "nav_profiles";
            if (normalized.equals("keybinds")) return "nav_keybinds";
            if (normalized.equals("themes")) return "nav_themes";
            if (normalized.startsWith("merge")) return "hud_group_merge";
            if (normalized.equals("collapse")) return "collapse";
            if (normalized.equals("expand")) return "expand";
            if (normalized.equals("left")) return "left";
            if (normalized.equals("right")) return "right";
            if (normalized.equals("up")) return "up";
            if (normalized.equals("down")) return "down";
            if (normalized.startsWith("selected")) return "select";
            if (normalized.startsWith("anchor")) return "hud-position";
            if (normalized.startsWith("locked")) return "lock";
            if (normalized.startsWith("unlocked")) return "unlock";
            if (normalized.startsWith("visible  on")) return "visibility";
            if (normalized.startsWith("visible  off")) return "hidden";
            if (normalized.startsWith("hud:")) {
                return normalized.endsWith("off") ? "hidden" : "visibility";
            }
            if (normalized.startsWith("reset")) return "reset";
            if (normalized.startsWith("save")) return "check";
            if (normalized.startsWith("rename")) return "edit";
            if (normalized.startsWith("delete")) return "delete";
            if (normalized.equals("done")) return "back";
            if (normalized.startsWith("edit hud")) return "hud-position";
            if (normalized.startsWith("unreleased / roadmap")
                    || normalized.startsWith("roadmap")) {
                return highlighted ? "collapse" : "add";
            }
            if (normalized.startsWith("scale")) return "expand";
            if (normalized.startsWith("opacity") && normalized.endsWith("100%")) {
                return "minus";
            }
            if (normalized.startsWith("snap") || normalized.startsWith("grid")
                    || normalized.startsWith("edges") || normalized.startsWith("style")
                    || normalized.startsWith("text") || normalized.startsWith("accent")
                    || normalized.startsWith("surface") || normalized.startsWith("opacity")) {
                if (normalized.startsWith("snap")) {
                    return normalized.endsWith("off") ? "minus" : "check";
                }
                if (normalized.startsWith("grid")) return "select";
                if (normalized.startsWith("edges")) return "expand";
                if (normalized.startsWith("style")) return "settings";
                if (normalized.startsWith("text")) return "edit";
                if (normalized.startsWith("accent")) return "info";
                if (normalized.startsWith("surface")) return "visibility";
                return "hidden";
            }
            if (normalized.startsWith("load")) return "expand";
            return null;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (active) {
                clickProgress = 1.0f;
                action.run();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    static final class CheckOption extends AbstractWidget {
        private final String icon;
        private final Runnable action;
        private boolean checked;
        private float hover;

        CheckOption(int x, int y, int width, int height, String label, String icon,
                    boolean checked, Runnable action) {
            super(x, y, width, height, Component.literal(label));
            this.icon = icon;
            this.checked = checked;
            this.action = action;
        }

        void setChecked(boolean checked) {
            this.checked = checked;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                                                int mouseY, float delta) {
            hover = TidalMotion.approach(hover,
                    active && isHoveredOrFocused() ? 1.0f : 0.0f, 14.0f);
            String state = !active ? checked ? "checked-disabled" : "unchecked-disabled"
                    : hover > 0.01f ? checked ? "checked-hover" : "unchecked-hover"
                    : checked ? "checked" : "unchecked";
            if (isFocused()) {
                drawAsset(graphics, "borders/focus-ring-r7",
                        getX() - 2, getY() - 2, getWidth() + 4, getHeight() + 4);
            }
            drawAsset(graphics, "controls/checkbox-" + state, getX(),
                    getY() + (getHeight() - 13) / 2, 13, 13);
            drawIcon(graphics, icon, getX() + 18,
                    getY() + (getHeight() - 16) / 2,
                    !active ? DISABLED : SECONDARY);
            Font font = net.minecraft.client.Minecraft.getInstance().font;
            String text = fitText(font, getMessage().getString(),
                    Math.max(0, getWidth() - 38));
            label(graphics, font, text, getX() + 38,
                    getY() + (getHeight() - font.lineHeight) / 2,
                    !active ? DISABLED : PRIMARY);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (!active) return;
            checked = !checked;
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    static final class PreferenceToggle extends AbstractWidget {
        private final String description;
        private final Runnable action;
        private boolean checked;
        private float hover;
        private float progress;

        PreferenceToggle(int x, int y, int width, int height, String label,
                         String description, boolean checked, Runnable action) {
            super(x, y, width, height, Component.literal(label));
            this.description = description;
            this.checked = checked;
            this.action = action;
        }

        void setChecked(boolean checked) {
            this.checked = checked;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                                                int mouseY, float delta) {
            hover = TidalMotion.approach(hover,
                    active && isHoveredOrFocused() ? 1.0f : 0.0f, 14.0f);
            progress = TidalMotion.approach(progress, checked ? 1.0f : 0.0f, 16.0f);
            drawAsset(graphics, "tidal/ui/polyglass_module_card", getX(), getY(),
                    getWidth(), getHeight(),
                    withAlpha(0xFFFFFFFF, TidalTheme.Glass.SURFACE_OPACITY));
            if (hover > 0.01f) {
                drawAsset(graphics, "tidal/ui/polyglass_module_card_hover", getX(), getY(),
                        getWidth(), getHeight(), withAlpha(0xFFFFFFFF,
                                Math.round(hover * 160)));
            }
            if (isFocused()) {
                drawAsset(graphics, "borders/focus-ring-r8", getX() - 2, getY() - 2,
                        getWidth() + 4, getHeight() + 4);
            }

            Font font = net.minecraft.client.Minecraft.getInstance().font;
            int textWidth = Math.max(0, getWidth() - 84);
            int titleY = getY() + 10;
            TidalTextRenderer.draw(graphics, font,
                    fitText(font, getMessage().getString(), textWidth),
                    getX() + 14, titleY, active ? PRIMARY : DISABLED, true);
            TidalTextRenderer.draw(graphics, font, fitText(font, description, textWidth),
                    getX() + 14, titleY + font.lineHeight + 3, SECONDARY);
            int toggleX = getRight() - 14 - 42;
            int toggleY = getY() + (getHeight() - 22) / 2;
            drawAsset(graphics, "tidal/ui/toggle_off", toggleX, toggleY, 42, 22,
                    withAlpha(0xFFFFFFFF, Math.round((1.0f - progress) * 255)));
            drawAsset(graphics, "tidal/ui/toggle_on", toggleX, toggleY, 42, 22,
                    withAlpha(0xFFFFFFFF, Math.round(progress * 255)));
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (!active) return;
            checked = !checked;
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    static final class ChoiceStrip extends AbstractWidget {
        private final String label;
        private final List<String> options;
        private final IntSupplier selectedIndex;
        private final IntConsumer onSelect;
        private int pressedIndex = -1;
        private float pressedProgress;

        ChoiceStrip(int x, int y, int width, int height, String label, List<String> options,
                    IntSupplier selectedIndex, IntConsumer onSelect) {
            super(x, y, width, height, Component.literal(label));
            this.label = label;
            this.options = List.copyOf(options);
            this.selectedIndex = selectedIndex;
            this.onSelect = onSelect;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                                                int mouseY, float delta) {
            Font font = net.minecraft.client.Minecraft.getInstance().font;
            pressedProgress = TidalMotion.approach(pressedProgress, 0.0f, 22.0f);
            drawIcon(graphics, "settings", getX(), getY() - 2, SECONDARY);
            label(graphics, font, label, getX() + 18, getY(), SECONDARY);
            int controlHeight = Math.max(21, getHeight() - 13);
            int controlY = getY() + getHeight() - controlHeight;
            int selected = Math.max(0, Math.min(options.size() - 1, selectedIndex.getAsInt()));
            if (options.isEmpty()) return;
            int baseWidth = getWidth() / options.size();
            for (int i = 0; i < options.size(); i++) {
                int x = getX() + i * baseWidth;
                int width = i == options.size() - 1 ? getRight() - x : baseWidth;
                boolean activeSegment = i == selected;
                drawAsset(graphics, activeSegment ? "buttons/button-primary-normal"
                        : "buttons/button-secondary-normal", x, controlY, width, controlHeight);
                if (isHoveredOrFocused() && mouseX >= x && mouseX < x + width) {
                    drawAsset(graphics, activeSegment ? "buttons/button-primary-hover"
                                    : "buttons/button-secondary-hover",
                            x, controlY, width, controlHeight,
                            withAlpha(0xFFFFFFFF, 150));
                }
                if (pressedIndex == i && pressedProgress > 0.01f) {
                    drawAsset(graphics, activeSegment ? "buttons/button-primary-pressed"
                                    : "buttons/button-secondary-pressed",
                            x, controlY, width, controlHeight,
                            withAlpha(0xFFFFFFFF, Math.round(pressedProgress * 255)));
                }
                centeredLabel(graphics, font,
                        fitText(font, options.get(i), Math.max(0, width - 8)),
                        x + width / 2, controlY + (controlHeight - font.lineHeight) / 2,
                        activeSegment ? PRIMARY : SECONDARY);
            }
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (options.isEmpty()) return;
            int controlY = getY() + getHeight() - Math.max(21, getHeight() - 13);
            if (event.y() < controlY) return;
            int segmentWidth = Math.max(1, getWidth() / options.size());
            int index = Math.min(options.size() - 1,
                    Math.max(0, ((int) event.x() - getX()) / segmentWidth));
            pressedIndex = index;
            pressedProgress = 1.0f;
            onSelect.accept(index);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    static final class ColorPicker extends AbstractWidget {
        private final IntSupplier selectedColor;
        private final IntConsumer onSelect;

        ColorPicker(int x, int y, int width, int height, IntSupplier selectedColor,
                    IntConsumer onSelect) {
            super(x, y, width, height, Component.literal("Accent color"));
            this.selectedColor = selectedColor;
            this.onSelect = onSelect;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                                                int mouseY, float delta) {
            Font font = net.minecraft.client.Minecraft.getInstance().font;
            int[] colors = colors();
            drawIcon(graphics, "info", getX(), getY() - 2, SECONDARY);
            label(graphics, font, "Accent", getX() + 18, getY(), SECONDARY);
            int swatchSize = Math.min(16, Math.max(1, getHeight() - 15));
            int rowY = getY() + 14;
            int cellWidth = getWidth() / colors.length;
            for (int i = 0; i < colors.length; i++) {
                int centerX = getX() + cellWidth * i + cellWidth / 2;
                int swatchX = centerX - swatchSize / 2;
                int swatchY = rowY + (getHeight() - 14 - swatchSize) / 2;
                graphics.fill(swatchX, swatchY, swatchX + swatchSize,
                        swatchY + swatchSize, colors[i]);
                boolean selected = colors[i] == selectedColor.getAsInt();
                boolean hovered = mouseX >= getX() + cellWidth * i
                        && mouseX < getX() + cellWidth * (i + 1);
                int markerColor = selected ? ACCENT : hovered ? PRIMARY : SECONDARY;
                drawAsset(graphics, "components/slider_thumb", centerX - 7,
                        swatchY + (swatchSize - 13) / 2, 13, 13, markerColor);
            }
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            int[] colors = colors();
            int index = Math.min(colors.length - 1,
                    Math.max(0, ((int) event.x() - getX()) * colors.length
                            / Math.max(1, getWidth())));
            onSelect.accept(colors[index]);
        }

        private static int[] colors() {
            return new int[]{ACCENT, ACCENT_HOVER, SUCCESS, WARNING, ERROR,
                    PRIMARY, SECONDARY, 0xFF9B8CFF};
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    static final class ValueSlider extends AbstractWidget {
        private final IntSupplier value;
        private final IntConsumer onChange;
        private final int minimum;
        private final int maximum;
        private final String label;
        private boolean dragging;

        ValueSlider(int x, int y, int width, int height, String label, IntSupplier value,
                    IntConsumer onChange, int minimum, int maximum) {
            super(x, y, width, height, Component.literal(label));
            this.label = label;
            this.value = value;
            this.onChange = onChange;
            this.minimum = minimum;
            this.maximum = maximum;
        }

        void setValueFromX(double mouseX) {
            float ratio = (float) Math.max(0.0, Math.min(1.0,
                    (mouseX - getX() - 5) / Math.max(1, getWidth() - 10)));
            onChange.accept(Math.round(minimum + ratio * (maximum - minimum)));
        }

        void setDragging(boolean dragging) {
            this.dragging = dragging;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                                                int mouseY, float delta) {
            Font font = net.minecraft.client.Minecraft.getInstance().font;
            int current = Math.max(minimum, Math.min(maximum, value.getAsInt()));
            drawIcon(graphics, "hidden", getX(), getY() - 2, SECONDARY);
            label(graphics, font, this.label + "  " + current + "%",
                    getX() + 18, getY(), SECONDARY);
            int trackX = getX() + 4;
            int trackY = getY() + getHeight() - 7;
            int trackWidth = Math.max(1, getWidth() - 8);
            boolean enabled = active;
            drawAsset(graphics, enabled ? "controls/slider-track"
                    : "controls/slider-track-disabled", trackX, trackY, trackWidth, 4);
            float progress = (current - minimum) / (float) Math.max(1, maximum - minimum);
            int fillWidth = Math.round(trackWidth * progress);
            if (fillWidth >= 4) {
                drawAsset(graphics, enabled ? "controls/slider-fill"
                        : "controls/slider-fill-disabled", trackX, trackY, fillWidth, 4);
            }
            int knobX = trackX + fillWidth - 4;
            String knob = !enabled ? "controls/knob-disabled"
                    : dragging ? "controls/knob-pressed"
                    : isHoveredOrFocused() ? "controls/knob-hover" : "controls/knob-normal";
            drawAsset(graphics, knob,
                    knobX, trackY - 2, 9, 9);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            setValueFromX(event.x());
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    static final class ModuleRow extends AbstractWidget {
        private TidalModule module;
        private Runnable select;
        private Runnable toggle;
        private float hover;
        private float switchProgress;
        private float selectedProgress;
        private float clickProgress;
        private boolean togglePressed;
        private TidalModule cachedNameModule;
        private int cachedNameWidth = -1;
        private String cachedFittedName;
        private TidalModule cachedDescriptionModule;
        private int cachedDescriptionWidth = -1;
        private String cachedFittedDescription;

        ModuleRow(int x, int y, int width, int height, TidalModule module,
                  Runnable select, Runnable toggle) {
            super(x, y, width, height, Component.literal(module.name));
            setModule(module, select, toggle);
        }

        void setModule(TidalModule module, Runnable select, Runnable toggle) {
            this.module = module;
            this.select = select;
            this.toggle = toggle;
            setMessage(Component.literal(module.name));
            setTooltip(Tooltip.create(Component.literal(module.description)));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                                                int mouseY, float delta) {
            hover = TidalMotion.approach(hover, isHoveredOrFocused() ? 1.0f : 0.0f, 14.0f);
            selectedProgress = TidalMotion.approach(selectedProgress,
                    module == TidalScreen.selectedModule() ? 1.0f : 0.0f, 11.0f);
            switchProgress = TidalMotion.approach(switchProgress,
                    module.enabled ? 1.0f : 0.0f, 16.0f);
            clickProgress = TidalMotion.approach(clickProgress, 0.0f, 22.0f);

            drawAsset(graphics, "tidal/ui/polyglass_module_card", getX(), getY(),
                    getWidth(), getHeight(),
                    withAlpha(0xFFFFFFFF, TidalTheme.Glass.SURFACE_OPACITY));
            if (hover > 0.01f && module.implemented) {
                drawAsset(graphics, "tidal/ui/polyglass_module_card_hover", getX(), getY(),
                        getWidth(), getHeight(), withAlpha(0xFFFFFFFF,
                                Math.round(hover * 255)));
            }
            if (selectedProgress > 0.01f) {
                drawAsset(graphics, "tidal/ui/polyglass_module_card_hover", getX(), getY(),
                        getWidth(), getHeight(), withAlpha(0xFFFFFFFF,
                                Math.round(selectedProgress * 255)));
                drawAsset(graphics, "borders/selected-outline-r8", getX(), getY(),
                        getWidth(), getHeight(), withAlpha(0xFFFFFFFF,
                                Math.round(selectedProgress * 255)));
            }
            if (clickProgress > 0.01f && isHoveredOrFocused()) {
                drawAsset(graphics, "tidal/ui/polyglass_module_card_hover", getX(), getY(),
                        getWidth(), getHeight(), withAlpha(0xFFFFFFFF,
                                Math.round(clickProgress * 255)));
            }
            if (isFocused()) {
                drawAsset(graphics, "borders/focus-ring-r8", getX() - 2, getY() - 2,
                        getWidth() + 4, getHeight() + 4);
            }

            Font font = net.minecraft.client.Minecraft.getInstance().font;
            int iconInset = 30;
            int nameColor = module.implemented ? PRIMARY : SECONDARY;
            int textX = getX() + CARD_CONTENT_INSET + iconInset;
            int textRight = module.implemented ? getRight() - 76
                    : getRight() - CARD_CONTENT_INSET - PLANNED_BADGE_WIDTH - 8;
            int nameWidth = Math.max(0, textRight - textX);
            if (module != cachedNameModule || nameWidth != cachedNameWidth) {
                cachedNameModule = module;
                cachedNameWidth = nameWidth;
                cachedFittedName = fitText(font, module.name, nameWidth);
            }
            if (module != cachedDescriptionModule || nameWidth != cachedDescriptionWidth) {
                cachedDescriptionModule = module;
                cachedDescriptionWidth = nameWidth;
                cachedFittedDescription = fitText(font, module.description, nameWidth);
            }
            int contentHeight = font.lineHeight * 2 + 4;
            int titleY = getY() + (getHeight() - contentHeight) / 2;
            TidalTextRenderer.draw(graphics, font, cachedFittedName,
                    textX, titleY, nameColor, true);
            TidalTextRenderer.draw(graphics, font, cachedFittedDescription,
                    textX, titleY + font.lineHeight + 4, SECONDARY);
            drawAsset(graphics, moduleIconPath(module),
                    getX() + CARD_CONTENT_INSET,
                    getY() + (getHeight() - 16) / 2, 16, 16,
                    module.enabled ? ACCENT : PRIMARY);

            if (module.implemented) {
                int settingsX = getRight() - 63;
                int settingsY = getY() + (getHeight() - 24) / 2;
                boolean settingsHover = mouseX >= settingsX && mouseX < settingsX + 24
                        && mouseY >= settingsY && mouseY < settingsY + 24;
                drawAsset(graphics, settingsHover ? "buttons/button-ghost-hover"
                                : "buttons/button-ghost-normal",
                        settingsX, settingsY, 24, 24,
                        withAlpha(0xFFFFFFFF, settingsHover ? 255 : 150));
                drawIcon(graphics, "settings", settingsX + 4, settingsY + 4,
                        settingsHover ? PRIMARY : SECONDARY);
                int trackX = getRight() - 31;
                int trackY = getY() + (getHeight() - 13) / 2;
                String off = "tidal/ui/toggle_off";
                String on = "tidal/ui/toggle_on";
                drawAsset(graphics, off, trackX, trackY, 25, 13,
                        withAlpha(0xFFFFFFFF, Math.round((1.0f - switchProgress) * 255)));
                drawAsset(graphics, on, trackX, trackY, 25, 13,
                        withAlpha(0xFFFFFFFF, Math.round(switchProgress * 255)));
            } else {
                centeredLabel(graphics, font, "PLANNED",
                        getRight() - CARD_CONTENT_INSET - PLANNED_BADGE_WIDTH / 2,
                        getY() + (getHeight() - font.lineHeight) / 2, SECONDARY);
            }
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            clickProgress = 1.0f;
            boolean settingsPressed = module.implemented
                    && event.x() >= getRight() - 63 && event.x() < getRight() - 39;
            togglePressed = module.implemented && !settingsPressed
                    && event.x() >= getRight() - 38;
            if (togglePressed) toggle.run();
            else select.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

    }

    static final class InstalledModRow extends AbstractWidget {
        private TidalModManager.ModEntry entry;
        private final Consumer<TidalModManager.ModEntry> toggle;
        private final Consumer<TidalModManager.ModEntry> openSettings;
        private final Consumer<TidalModManager.ModEntry> openDetails;
        private float hover;
        private float switchProgress;

        InstalledModRow(int x, int y, int width, int height,
                        TidalModManager.ModEntry entry,
                        Consumer<TidalModManager.ModEntry> toggle,
                        Consumer<TidalModManager.ModEntry> openSettings,
                        Consumer<TidalModManager.ModEntry> openDetails) {
            super(x, y, width, height, Component.literal(entry.name));
            this.entry = entry;
            this.toggle = toggle;
            this.openSettings = openSettings;
            this.openDetails = openDetails;
            updateActions();
        }

        void setEntry(TidalModManager.ModEntry entry) {
            this.entry = entry;
            setMessage(Component.literal(entry.name));
            setTooltip(Tooltip.create(Component.literal(entry.id + " · " + entry.version)));
            updateActions();
        }

        private void updateActions() {
            active = true;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            if (!active || mouseY < getY() || mouseY >= getBottom()) return false;
            if (mouseX >= getX() && mouseX < getRight()) return true;
            int settingsLeft = getRight() - (entry.canToggle() ? 62 : 32);
            int settingsRight = settingsLeft + 24;
            boolean settings = entry.canOpenSettings() && mouseX >= settingsLeft
                    && mouseX < settingsRight;
            boolean toggle = entry.canToggle() && mouseX >= getRight() - 34
                    && mouseX < getRight() - 6;
            return settings || toggle;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                                                int mouseY, float delta) {
            boolean cardHovered = mouseX >= getX() && mouseX < getRight()
                    && mouseY >= getY() && mouseY < getBottom();
            hover = TidalMotion.approach(hover, cardHovered ? 1.0f : 0.0f, 14.0f);
            switchProgress = TidalMotion.approach(switchProgress,
                    (entry.pending ? entry.pendingEnabled : entry.enabled) ? 1.0f : 0.0f,
                    16.0f);
            drawAsset(graphics, "tidal/ui/polyglass_module_card", getX(), getY(),
                    getWidth(), getHeight(),
                    withAlpha(0xFFFFFFFF, TidalTheme.Glass.SURFACE_OPACITY));
            if (hover > 0.01f) {
                drawAsset(graphics, "tidal/ui/polyglass_module_card_hover", getX(), getY(),
                        getWidth(), getHeight(), withAlpha(0xFFFFFFFF,
                                Math.round(hover * 160)));
            }

            int iconX = getX() + CARD_CONTENT_INSET;
            int iconY = getY() + (getHeight() - 32) / 2;
            drawAsset(graphics, "tidal/ui/polyglass_module_card", iconX, iconY, 32, 32);
            if (entry.icon != null) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, entry.icon, iconX + 2, iconY + 2,
                        0, 0, 28, 28, 32, 32, 32, 32);
            } else {
                Font font = net.minecraft.client.Minecraft.getInstance().font;
                String initial = entry.name.isBlank() ? "?" : entry.name.substring(0, 1)
                        .toUpperCase(Locale.ROOT);
                centeredLabel(graphics, font, initial, iconX + 16,
                        iconY + (32 - font.lineHeight) / 2, SECONDARY);
            }

            Font font = net.minecraft.client.Minecraft.getInstance().font;
            int rightInset = entry.canToggle() ? 40 : 8;
            if (entry.canOpenSettings()) rightInset += 30;
            int textX = iconX + 40;
            int textWidth = Math.max(0, getRight() - rightInset - textX);
            int titleY = getY() + 8;
            strongLabel(graphics, font, fitText(font, entry.name, textWidth),
                    textX, titleY, PRIMARY);
            label(graphics, font, fitText(font, entry.id + " · " + entry.version, textWidth),
                    textX, titleY + font.lineHeight + 2, SECONDARY);
            label(graphics, font, fitText(font,
                            entry.authors.isEmpty() ? entry.environment
                                    : entry.authors + " · " + entry.environment,
                            textWidth),
                    textX, titleY + (font.lineHeight + 2) * 2, MUTED);
            label(graphics, font, fitText(font, entry.description, textWidth),
                    textX, titleY + (font.lineHeight + 2) * 3, SECONDARY);

            String status = entry.status() + " · Loaded";
            int statusColor = entry.pending ? WARNING
                    : entry.failure != null || entry.settingsFailure != null ? ERROR
                    : entry.toggle == null ? WARNING
                    : entry.enabled ? SUCCESS : SECONDARY;
            label(graphics, font, fitText(font, status, textWidth),
                    textX, titleY + (font.lineHeight + 2) * 4, statusColor);

            int actionY = getY() + (getHeight() - 24) / 2;
            if (entry.canOpenSettings()) {
                int settingsX = getRight() - (entry.canToggle() ? 62 : 32);
                boolean settingsHover = mouseX >= settingsX && mouseX < settingsX + 24
                        && mouseY >= actionY && mouseY < actionY + 24;
                drawAsset(graphics, settingsHover ? "buttons/button-ghost-hover"
                                : "buttons/button-ghost-normal",
                        settingsX, actionY, 24, 24);
                drawIcon(graphics, "settings", settingsX + 4, actionY + 4,
                        settingsHover ? PRIMARY : SECONDARY);
            }
            if (entry.canToggle()) {
                int trackX = getRight() - 32;
                int trackY = getY() + (getHeight() - 13) / 2;
                drawAsset(graphics, "tidal/ui/toggle_off", trackX, trackY, 25, 13,
                        withAlpha(0xFFFFFFFF, Math.round((1.0f - switchProgress) * 255)));
                drawAsset(graphics, "tidal/ui/toggle_on", trackX, trackY, 25, 13,
                        withAlpha(0xFFFFFFFF, Math.round(switchProgress * 255)));
            }
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (entry.canOpenSettings() && event.x() >= getRight()
                    - (entry.canToggle() ? 62 : 32)
                    && event.x() < getRight() - (entry.canToggle() ? 38 : 8)) {
                openSettings.accept(entry);
            } else if (entry.canToggle() && event.x() >= getRight() - 34) {
                toggle.accept(entry);
            } else {
                openDetails.accept(entry);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
