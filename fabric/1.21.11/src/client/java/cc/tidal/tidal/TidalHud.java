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

import java.util.Arrays;
import java.util.List;

public final class TidalHud {
    private static final List<TidalModule> MODULES = TidalModules.all();
    private static final String[] CACHED_VALUES = new String[MODULES.size()];
    private static final String[] CURRENT_VALUES = new String[MODULES.size()];
    private static final int[] CACHED_WIDTHS = new int[MODULES.size()];
    private static final int[] CACHED_NAME_WIDTHS = new int[MODULES.size()];
    private static final int[] CACHED_VALUE_WIDTHS = new int[MODULES.size()];
    private static final int HORIZONTAL_PADDING = 8;
    private static final int LABEL_GAP = 6;
    private static final int PANEL_HEIGHT = 22;
    private static final int KEYSTROKES_WIDTH = 80;
    private static final int KEYSTROKES_HEIGHT = 124;
    private static final int[] SEEN_SURFACE_GROUPS = new int[MODULES.size()];
    private TidalHud() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (!TidalConfig.hudEnabled || client.player == null || client.gui.hud.isHidden()) return;
        if (client.gui.screen() instanceof TidalHudEditorScreen
                || client.gui.screen() instanceof TidalScreen) return;
        TidalData.recordFrame();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        updateFrameValues(null, client);
        drawMergedSurfaces(graphics, client, width, height, null, CURRENT_VALUES);
        for (int i = 0; i < MODULES.size(); i++) {
            TidalModule module = MODULES.get(i);
            if (!module.enabled || !module.implemented || module.group != TidalModule.Group.HUD) {
                continue;
            }
            String value = CURRENT_VALUES[i];
            if (value.isEmpty()) continue;
            renderModule(graphics, client, module, value, i, width, height,
                    module.surfaceGroupId == 0);
        }
    }

    static void renderEditorPreview(GuiGraphicsExtractor graphics, TidalModule module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.hud.isHidden()) return;
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        updateFrameValues(module, client);
        drawMergedSurfaces(graphics, client, width, height, module, CURRENT_VALUES);
        for (int i = 0; i < MODULES.size(); i++) {
            TidalModule enabledModule = MODULES.get(i);
            if (!enabledModule.enabled || !enabledModule.implemented
                    || enabledModule.group != TidalModule.Group.HUD || enabledModule == module) {
                continue;
            }
            String value = CURRENT_VALUES[i];
            if (!value.isEmpty()) renderModule(graphics, client, enabledModule, value, i,
                    width, height, enabledModule.surfaceGroupId == 0);
        }
        int index = MODULES.indexOf(module);
        if (index < 0 || !module.implemented || module.group != TidalModule.Group.HUD) return;
        String value = CURRENT_VALUES[index];
        if (value.isEmpty()) value = "Preview";
        renderModule(graphics, client, module, value, index,
                width, height, module.surfaceGroupId == 0);
    }

    private static void updateFrameValues(TidalModule preview, Minecraft client) {
        Arrays.fill(CURRENT_VALUES, "");
        for (int i = 0; i < MODULES.size(); i++) {
            TidalModule module = MODULES.get(i);
            if (!module.implemented || module.group != TidalModule.Group.HUD
                    || !module.enabled && module != preview) continue;
            String value = TidalData.value(module.id);
            if (value.isEmpty() && module == preview) value = "Preview";
            CURRENT_VALUES[i] = value;
            if (!value.isEmpty() && !module.id.equals("keystrokes")) {
                measure(client, module, value, i);
            }
        }
    }

    private static void renderModule(GuiGraphicsExtractor graphics, Minecraft client,
                                     TidalModule module, String value, int index,
                                     int viewportWidth, int viewportHeight,
                                     boolean drawSurface) {
        if (module.id.equals("keystrokes")) {
            renderKeystrokes(graphics, client, module, viewportWidth, viewportHeight);
            return;
        }
        measure(client, module, value, index);
        int rawWidth = CACHED_WIDTHS[index];
        int rawHeight = PANEL_HEIGHT;
        int scaledWidth = (int) Math.ceil(rawWidth * module.scale);
        int scaledHeight = height(module);
        int x = Math.max(0, Math.min(x(module, viewportWidth), viewportWidth - scaledWidth));
        int y = Math.max(0, Math.min(y(module, viewportHeight), viewportHeight - scaledHeight));
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(module.scale, module.scale);
        if (drawSurface) drawBackground(graphics, module, rawWidth, rawHeight);
        int textY = (rawHeight - client.font.lineHeight) / 2;
        TidalUi.strongLabel(graphics, client.font, module.name, HORIZONTAL_PADDING,
                textY, module.textColor);
        TidalUi.label(graphics, client.font, value,
                HORIZONTAL_PADDING + CACHED_NAME_WIDTHS[index] + LABEL_GAP,
                textY, module.textColor == TidalTheme.PRIMARY
                        ? TidalTheme.SECONDARY : module.textColor);
        pose.popMatrix();
    }

    private static void renderKeystrokes(GuiGraphicsExtractor graphics, Minecraft client,
                                         TidalModule module, int viewportWidth,
                                         int viewportHeight) {
        int scaledWidth = (int) Math.ceil(KEYSTROKES_WIDTH * module.scale);
        int scaledHeight = (int) Math.ceil(KEYSTROKES_HEIGHT * module.scale);
        int x = Math.max(0, Math.min(x(module, viewportWidth), viewportWidth - scaledWidth));
        int y = Math.max(0, Math.min(y(module, viewportHeight), viewportHeight - scaledHeight));
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(module.scale, module.scale);
        drawKey(graphics, client, module, "W", 28, 0, 24, 24, TidalData.KEY_W);
        drawKey(graphics, client, module, "A", 0, 28, 24, 24, TidalData.KEY_A);
        drawKey(graphics, client, module, "S", 28, 28, 24, 24, TidalData.KEY_S);
        drawKey(graphics, client, module, "D", 56, 28, 24, 24, TidalData.KEY_D);
        drawKey(graphics, client, module, "SNK", 0, 56, 38, 18, TidalData.KEY_SNEAK);
        drawKey(graphics, client, module, "SPR", 42, 56, 38, 18, TidalData.KEY_SPRINT);
        drawMouseKey(graphics, client, module, "LMB", 0, 78, 38, 26, 0);
        drawMouseKey(graphics, client, module, "RMB", 42, 78, 38, 26, 1);
        drawKey(graphics, client, module, "SPACE", 0, 108, 80, 16, TidalData.KEY_SPACE);
        pose.popMatrix();
    }

    private static void drawKey(GuiGraphicsExtractor graphics, Minecraft client,
                                TidalModule module, String label, int x, int y,
                                int width, int height, int key) {
        boolean pressed = TidalData.inputDown(key);
        int color = pressed ? module.accentColor
                : TidalUi.withOpacity(module.backgroundColor,
                Math.max(0.35f, module.backgroundOpacity / 100.0f));
        TidalUi.panel(graphics, x, y, width, height, color, 4);
        TidalUi.centeredLabel(graphics, client.font, label, x + width / 2,
                y + (height - client.font.lineHeight) / 2,
                pressed ? TidalTheme.PRIMARY : module.textColor);
    }

    private static void drawMouseKey(GuiGraphicsExtractor graphics, Minecraft client,
                                     TidalModule module, String label, int x, int y,
                                     int width, int height, int button) {
        int key = button == 0 ? TidalData.KEY_LMB : TidalData.KEY_RMB;
        boolean pressed = TidalData.inputDown(key);
        int color = pressed ? module.accentColor
                : TidalUi.withOpacity(module.backgroundColor,
                Math.max(0.35f, module.backgroundOpacity / 100.0f));
        TidalUi.panel(graphics, x, y, width, height, color, 4);
        int textColor = pressed ? TidalTheme.PRIMARY : module.textColor;
        TidalUi.centeredLabel(graphics, client.font, label, x + width / 2, y + 2, textColor);
        TidalUi.centeredLabel(graphics, client.font,
                TidalData.clicksPerSecond(button) + " CPS",
                x + width / 2, y + 13, textColor);
    }

    private static void drawMergedSurfaces(GuiGraphicsExtractor graphics, Minecraft client,
                                           int viewportWidth, int viewportHeight,
                                           TidalModule preview, String[] values) {
        int seenCount = 0;
        for (int i = 0; i < MODULES.size(); i++) {
            TidalModule first = MODULES.get(i);
            int groupId = first.surfaceGroupId;
            if (groupId == 0 || !isSurfaceVisible(first, preview)) continue;
            boolean alreadyDrawn = false;
            for (int seen = 0; seen < seenCount; seen++) {
                if (SEEN_SURFACE_GROUPS[seen] == groupId) {
                    alreadyDrawn = true;
                    break;
                }
            }
            if (alreadyDrawn) continue;
            SEEN_SURFACE_GROUPS[seenCount++] = groupId;

            int left = Integer.MAX_VALUE;
            int top = Integer.MAX_VALUE;
            int right = Integer.MIN_VALUE;
            int bottom = Integer.MIN_VALUE;
            for (int memberIndex = 0; memberIndex < MODULES.size(); memberIndex++) {
                TidalModule member = MODULES.get(memberIndex);
                if (member.surfaceGroupId != groupId || !isSurfaceVisible(member, preview)) {
                    continue;
                }
                String value = values[memberIndex];
                if (value.isEmpty()) continue;
                int x = x(member, viewportWidth);
                int y = y(member, viewportHeight);
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x + width(member));
                bottom = Math.max(bottom, y + height(member));
            }
            if (left == Integer.MAX_VALUE) continue;
            graphics.pose().pushMatrix();
            graphics.pose().translate(left, top);
            drawBackground(graphics, first, right - left, bottom - top);
            graphics.pose().popMatrix();
        }
    }

    private static boolean isSurfaceVisible(TidalModule module, TidalModule preview) {
        return module.implemented && module.group == TidalModule.Group.HUD
                && (module.enabled || module == preview);
    }

    private static void drawBackground(GuiGraphicsExtractor graphics, TidalModule module,
                                       int width, int height) {
        float opacity = module.backgroundOpacity / 100.0f;
        int background = TidalUi.withOpacity(module.backgroundColor, opacity);
        switch (module.hudStyle) {
            case MINIMAL, TRANSPARENT -> {
            }
            case SUBTLE -> TidalUi.panel(graphics, 0, 0, width, height,
                    TidalUi.withOpacity(module.backgroundColor, opacity * 0.55f), 4);
            case ROUNDED -> TidalUi.panel(graphics, 0, 0, width, height, background, 5);
            case SQUARE -> TidalUi.panel(graphics, 0, 0, width, height, background, 0);
            case BORDERED -> TidalUi.borderedPanel(graphics, 0, 0, width, height,
                    background, 5, false);
        }
    }

    public static int x(TidalModule module, int viewportWidth) {
        int free = Math.max(0, viewportWidth - width(module));
        initializeNormalizedPosition(module, free, Math.max(0,
                Minecraft.getInstance().getWindow().getGuiScaledHeight() - height(module)));
        return Math.round(module.normalizedX * free);
    }

    public static int y(TidalModule module, int viewportHeight) {
        int free = Math.max(0, viewportHeight - height(module));
        initializeNormalizedPosition(module, Math.max(0,
                Minecraft.getInstance().getWindow().getGuiScaledWidth() - width(module)), free);
        return Math.round(module.normalizedY * free);
    }

    public static void setPosition(TidalModule module, int x, int y,
                                   int viewportWidth, int viewportHeight) {
        int freeX = Math.max(0, viewportWidth - width(module));
        int freeY = Math.max(0, viewportHeight - height(module));
        module.x = x;
        module.y = y;
        module.normalizedX = freeX == 0 ? 0 : Math.max(0, Math.min(1, x / (float) freeX));
        module.normalizedY = freeY == 0 ? 0 : Math.max(0, Math.min(1, y / (float) freeY));
        module.normalizedPosition = true;
    }

    public static void setAnchor(TidalModule module, TidalModule.Anchor anchor,
                                 int viewportWidth, int viewportHeight) {
        module.anchor = anchor;
        int freeX = Math.max(0, viewportWidth - width(module));
        int freeY = Math.max(0, viewportHeight - height(module));
        setPosition(module, Math.round(freeX * anchor.horizontal / 2.0f),
                Math.round(freeY * anchor.vertical / 2.0f), viewportWidth, viewportHeight);
    }

    public static void initializePositions(int viewportWidth, int viewportHeight) {
        for (TidalModule module : MODULES) {
            initializeNormalizedPosition(module,
                    Math.max(0, viewportWidth - width(module)),
                    Math.max(0, viewportHeight - height(module)));
        }
    }

    private static void initializeNormalizedPosition(TidalModule module, int freeX, int freeY) {
        if (module.normalizedPosition) return;
        module.normalizedX = freeX == 0 ? 0 : Math.max(0, Math.min(1, module.x / (float) freeX));
        module.normalizedY = freeY == 0 ? 0 : Math.max(0, Math.min(1, module.y / (float) freeY));
        module.normalizedPosition = true;
    }

    public static void cycleColor(TidalModule module, boolean text) {
        int current = text ? module.textColor : module.accentColor;
        int[] colors = text ? textColors() : accentColors();
        int index = 0;
        while (index < colors.length && colors[index] != current) index++;
        int next = colors[(index + 1) % colors.length];
        if (text) module.textColor = next;
        else module.accentColor = next;
    }

    public static void cycleBackground(TidalModule module) {
        int[] colors = backgroundColors();
        int index = 0;
        while (index < colors.length && colors[index] != module.backgroundColor) index++;
        module.backgroundColor = colors[(index + 1) % colors.length];
    }

    private static int[] textColors() {
        return new int[]{TidalTheme.PRIMARY, TidalTheme.SECONDARY, TidalTheme.ACCENT,
                TidalTheme.SUCCESS, TidalTheme.WARNING, TidalTheme.ERROR};
    }

    private static int[] accentColors() {
        return new int[]{TidalTheme.ACCENT, TidalTheme.SUCCESS, TidalTheme.WARNING,
                TidalTheme.ERROR, TidalTheme.PRIMARY};
    }

    private static int[] backgroundColors() {
        return new int[]{TidalTheme.SURFACE, TidalTheme.BACKGROUND,
                TidalTheme.INTERACTIVE, TidalTheme.ELEVATED};
    }

    public static int width(TidalModule module) {
        if (module.id.equals("keystrokes")) {
            return (int) Math.ceil(KEYSTROKES_WIDTH * module.scale);
        }
        int index = MODULES.indexOf(module);
        if (index < 0) return 0;
        String value = CACHED_VALUES[index];
        if (value == null) {
            value = TidalData.value(module.id);
            measure(Minecraft.getInstance(), module, value.isEmpty() ? "Preview" : value, index);
        }
        return (int) Math.ceil(CACHED_WIDTHS[index] * module.scale);
    }

    public static int height(TidalModule module) {
        if (module.id.equals("keystrokes")) {
            return (int) Math.ceil(KEYSTROKES_HEIGHT * module.scale);
        }
        return Math.max(18, (int) Math.ceil(PANEL_HEIGHT * module.scale));
    }

    static void placeInAvailableSpace(TidalModule module) {
        if (module.group != TidalModule.Group.HUD) return;
        Minecraft client = Minecraft.getInstance();
        int viewportWidth = client.getWindow().getGuiScaledWidth();
        int viewportHeight = client.getWindow().getGuiScaledHeight();
        int maxX = Math.max(0, viewportWidth - width(module));
        int maxY = Math.max(0, viewportHeight - height(module));
        int preferredX = Math.max(0, Math.min(x(module, viewportWidth), maxX));
        int preferredY = Math.max(0, Math.min(y(module, viewportHeight), maxY));
        if (fits(module, preferredX, preferredY, viewportWidth, viewportHeight)) {
            setPosition(module, preferredX, preferredY, viewportWidth, viewportHeight);
            return;
        }

        for (int y = 12; y <= maxY; y += 8) {
            for (int x = 12; x <= maxX; x += 8) {
                if (!fits(module, x, y, viewportWidth, viewportHeight)) continue;
                setPosition(module, x, y, viewportWidth, viewportHeight);
                return;
            }
        }
        setPosition(module, preferredX, preferredY, viewportWidth, viewportHeight);
    }

    private static boolean fits(TidalModule module, int x, int y, int viewportWidth,
                                int viewportHeight) {
        int right = x + width(module);
        int bottom = y + height(module);
        for (TidalModule other : MODULES) {
            if (other == module || !other.enabled || !other.implemented) continue;
            int otherX = x(other, viewportWidth);
            int otherY = y(other, viewportHeight);
            int otherRight = otherX + width(other);
            int otherBottom = otherY + height(other);
            if (x < otherRight && right > otherX && y < otherBottom && bottom > otherY) {
                return false;
            }
        }
        return x >= 0 && y >= 0 && right <= viewportWidth && bottom <= viewportHeight;
    }

    private static void measure(Minecraft client, TidalModule module, String value, int index) {
        if (value.equals(CACHED_VALUES[index])) return;
        CACHED_VALUES[index] = value;
        CACHED_NAME_WIDTHS[index] = TidalUi.strongWidth(client.font, module.name);
        CACHED_VALUE_WIDTHS[index] = TidalUi.textWidth(client.font, value);
        CACHED_WIDTHS[index] = Math.max(74, HORIZONTAL_PADDING + CACHED_NAME_WIDTHS[index]
                + LABEL_GAP + CACHED_VALUE_WIDTHS[index] + HORIZONTAL_PADDING);
    }
}
