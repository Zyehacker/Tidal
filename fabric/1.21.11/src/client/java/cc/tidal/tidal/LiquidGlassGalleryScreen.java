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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

final class LiquidGlassGalleryScreen extends Screen {
    private static final int COLUMNS = 4;
    private static final int TILE_GAP = 8;
    private static final int TILE_HEIGHT = 96;
    private final Screen parent;
    private final List<LiquidGlassAssets.Asset> assets = LiquidGlassAssets.all().stream()
            .sorted((first, second) -> first.path().compareTo(second.path()))
            .toList();
    private final TidalUi.ActionButton[] scaleButtons = new TidalUi.ActionButton[4];
    private int galleryX;
    private int galleryY;
    private int galleryWidth;
    private int galleryHeight;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private int scrollRow;
    private int firstVisibleAsset;
    private boolean draggingScrollbar;

    LiquidGlassGalleryScreen(Screen parent) {
        super(Component.literal("Tidal Liquid Glass Gallery"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        galleryWidth = Math.min(760, Math.max(1, width - 16));
        galleryHeight = Math.min(460, Math.max(1, height - 16));
        galleryX = (width - galleryWidth) / 2;
        galleryY = (height - galleryHeight) / 2;
        contentX = galleryX + 10;
        contentY = galleryY + 56;
        contentWidth = galleryWidth - 20;
        contentHeight = galleryHeight - 66;
        int scaleX = galleryX + galleryWidth - 4 * 38 - 42;
        for (int i = 0; i < scaleButtons.length; i++) {
            int scale = i + 1;
            scaleButtons[i] = addRenderableWidget(new TidalUi.ActionButton(
                    scaleX + i * 38, galleryY + 12, 34, 24, scale + "x",
                    TidalUi.ActionButton.Style.CONTROL, () -> {
                LiquidGlassAssets.setGalleryScale(scale);
                for (int buttonIndex = 0; buttonIndex < scaleButtons.length; buttonIndex++) {
                    scaleButtons[buttonIndex].highlighted = buttonIndex + 1 == scale;
                }
            }));
        }
        scaleButtons[0].highlighted = true;
        LiquidGlassAssets.setGalleryScale(1);
        addRenderableWidget(new TidalUi.ActionButton(
                galleryX + 10, galleryY + 12, 64, 24, "Back",
                TidalUi.ActionButton.Style.QUIET, this::returnToParent));
        calculateScroll();
    }

    private void calculateScroll() {
        int visibleRows = Math.max(1, contentHeight / (TILE_HEIGHT + TILE_GAP));
        int totalRows = (assets.size() + COLUMNS - 1) / COLUMNS;
        scrollRow = Math.max(0, Math.min(scrollRow, Math.max(0, totalRows - visibleRows)));
        firstVisibleAsset = scrollRow * COLUMNS;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                                   float delta) {
        if (TidalTheme.Glass.BLUR_RADIUS > 0) renderBlurredBackground(graphics);
        TidalMotion.beginFrame();
        TidalUi.drawAsset(graphics, "tidal/ui/polyglass_window_frame", galleryX, galleryY,
                galleryWidth, galleryHeight,
                TidalUi.withAlpha(0xFFFFFFFF, TidalTheme.Glass.SURFACE_OPACITY));
        TidalUi.strongLabel(graphics, font, "LIQUID GLASS ASSET GALLERY",
                galleryX + 88, galleryY + 15, TidalUi.PRIMARY);
        TidalUi.label(graphics, font, LiquidGlassAssets.assetCount() + " manifest SVGs  ·  "
                        + "Raster scale " + (selectedScale() + "x"),
                galleryX + 88, galleryY + 29, TidalUi.SECONDARY);
        int visibleRows = Math.max(1, contentHeight / (TILE_HEIGHT + TILE_GAP));
        graphics.enableScissor(contentX, contentY, contentX + contentWidth,
                contentY + contentHeight);
        boolean hasScrollbar = assets.size() > visibleRows * COLUMNS;
        int tileAreaWidth = contentWidth - (hasScrollbar ? 12 : 0);
        int tileWidth = (tileAreaWidth - (COLUMNS - 1) * TILE_GAP) / COLUMNS;
        int lastVisibleAsset = Math.min(assets.size(), firstVisibleAsset + visibleRows * COLUMNS);
        for (int index = firstVisibleAsset; index < lastVisibleAsset; index++) {
            LiquidGlassAssets.Asset asset = assets.get(index);
            int localIndex = index - firstVisibleAsset;
            int column = localIndex % COLUMNS;
            int row = localIndex / COLUMNS;
            int x = contentX + column * (tileWidth + TILE_GAP);
            int y = contentY + row * (TILE_HEIGHT + TILE_GAP);
            drawTile(graphics, asset, x, y, tileWidth);
        }
        if (hasScrollbar) drawScrollbar(graphics, mouseX);
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, delta);
    }

    private void drawScrollbar(GuiGraphics graphics, int mouseX) {
        int trackX = contentX + contentWidth - 6;
        int totalRows = (assets.size() + COLUMNS - 1) / COLUMNS;
        int visibleRows = Math.max(1, contentHeight / (TILE_HEIGHT + TILE_GAP));
        int thumbHeight = Math.max(24, contentHeight * visibleRows / totalRows);
        int scrollableRows = Math.max(1, totalRows - visibleRows);
        int thumbY = contentY + (contentHeight - thumbHeight) * scrollRow / scrollableRows;
        boolean hovered = mouseX >= trackX - 2 && mouseX <= trackX + 8;
        TidalUi.drawAsset(graphics, "controls/scrollbar-track",
                trackX, contentY, 6, contentHeight);
        TidalUi.drawAsset(graphics, hovered ? "controls/scrollbar-thumb-hover"
                        : "controls/scrollbar-thumb",
                trackX, thumbY, 6, thumbHeight);
    }

    private void setScrollFromY(double mouseY) {
        int totalRows = (assets.size() + COLUMNS - 1) / COLUMNS;
        int visibleRows = Math.max(1, contentHeight / (TILE_HEIGHT + TILE_GAP));
        int thumbHeight = Math.max(24, contentHeight * visibleRows / totalRows);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        double ratio = Math.max(0, Math.min(1,
                (mouseY - contentY - thumbHeight / 2.0)
                        / Math.max(1, contentHeight - thumbHeight)));
        scrollRow = (int) Math.round(ratio * maxScroll);
        calculateScroll();
    }

    private void drawTile(GuiGraphics graphics, LiquidGlassAssets.Asset asset,
                          int x, int y, int tileWidth) {
        TidalUi.drawAsset(graphics, "surfaces/tooltip", x, y, tileWidth, TILE_HEIGHT);
        TidalUi.label(graphics, font,
                TidalUi.fitText(font, asset.path(), tileWidth - 14),
                x + 7, y + 6, TidalUi.PRIMARY);
        int maxWidth = tileWidth - 14;
        int maxHeight = TILE_HEIGHT - 30;
        double scale = Math.min(1, Math.min(maxWidth / (double) asset.width(),
                maxHeight / (double) asset.height()));
        int drawWidth = Math.max(1, (int) Math.round(asset.width() * scale));
        int drawHeight = Math.max(1, (int) Math.round(asset.height() * scale));
        if (asset.scaling() == LiquidGlassAssets.Scaling.FIXED) {
            drawWidth = asset.width();
            drawHeight = asset.height();
        }
        int minimumWidth = asset.nineSlice().left() + asset.nineSlice().right();
        int minimumHeight = asset.nineSlice().top() + asset.nineSlice().bottom();
        if (asset.scaling() == LiquidGlassAssets.Scaling.NINE_SLICE
                || asset.scaling() == LiquidGlassAssets.Scaling.NINE_SLICE_X
                || asset.scaling() == LiquidGlassAssets.Scaling.NINE_SLICE_Y) {
            drawWidth = Math.max(drawWidth, minimumWidth + 1);
            drawHeight = Math.max(drawHeight, minimumHeight + 1);
        }
        int drawX = x + (tileWidth - drawWidth) / 2;
        int drawY = y + 24 + Math.max(0, (maxHeight - drawHeight) / 2);
        TidalUi.drawAsset(graphics, asset.path(), drawX, drawY,
                drawWidth, drawHeight, 0xFFFFFFFF);
    }

    private int selectedScale() {
        for (int i = 0; i < scaleButtons.length; i++) {
            if (scaleButtons[i] != null && scaleButtons[i].highlighted) return i + 1;
        }
        return 1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                 double verticalAmount) {
        if (mouseX < contentX || mouseX > contentX + contentWidth
                || mouseY < contentY || mouseY > contentY + contentHeight) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        scrollRow += verticalAmount < 0 ? 1 : -1;
        calculateScroll();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int trackX = contentX + contentWidth - 6;
        if (event.button() == 0 && assets.size() > 0
                && event.x() >= trackX - 2 && event.x() <= trackX + 8
                && event.y() >= contentY && event.y() <= contentY + contentHeight) {
            draggingScrollbar = true;
            setScrollFromY(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingScrollbar && event.button() == 0) {
            setScrollFromY(event.y());
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingScrollbar && event.button() == 0) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    private void returnToParent() {
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    @Override
    public void removed() {
        LiquidGlassAssets.clearGalleryScale();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
