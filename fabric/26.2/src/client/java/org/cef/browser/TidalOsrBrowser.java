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

package org.cef.browser;

import org.cef.CefClient;
import org.cef.CefBrowserSettings;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** CEF OSR browser that sends rendered frames to a custom handler. */
public final class TidalOsrBrowser extends CefBrowser_N implements CefRenderHandler {
    private final CopyOnWriteArrayList<Consumer<CefPaintEvent>> paintListeners =
            new CopyOnWriteArrayList<>();
    private final Consumer<CefPaintEvent> paintListener;
    private volatile int width;
    private volatile int height;
    private volatile double scale;
    private boolean focusCallInProgress;
    private boolean focusStateKnown;
    private boolean focused;

    public TidalOsrBrowser(CefClient client, String url, CefRequestContext context,
                           int width, int height, double scale,
                           Consumer<CefPaintEvent> paintListener) {
        super(client, url, context, null, null, (CefBrowserSettings) null);
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.scale = Math.max(1.0, scale);
        this.paintListener = paintListener;
    }

    @Override
    public void createImmediately() {
        createBrowser(getClient(), 0L, getUrl(), true, false, null, getRequestContext());
    }

    @Override
    public Component getUIComponent() {
        throw new UnsupportedOperationException("A windowless browser has no UI component.");
    }

    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean isDevTools) {
        return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Screenshots are not supported by this browser."));
    }

    public void resize(int width, int height, double scale) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.scale = Math.max(1.0, scale);
        wasResized(this.width, this.height);
    }

    public void requestRepaint() {
        invalidate();
    }

    @Override
    public synchronized void setFocus(boolean enable) {
        if (focusCallInProgress || focusStateKnown && focused == enable) return;
        focusCallInProgress = true;
        focusStateKnown = true;
        focused = enable;
        try {
            super.setFocus(enable);
        } finally {
            focusCallInProgress = false;
        }
    }

    public void mouseEvent(int id, int x, int y, int modifiers, int button, int clickCount) {
        int awtButton = button < 0 ? MouseEvent.NOBUTTON
                : button == 0 ? MouseEvent.BUTTON1
                : button == 1 ? MouseEvent.BUTTON3 : MouseEvent.BUTTON2;
        sendMouseEvent(new MouseEvent(new Component() {}, id, System.currentTimeMillis(),
                modifiers, x, y, clickCount, false, awtButton));
    }

    public void wheelEvent(int x, int y, int modifiers, int rotation) {
        sendMouseWheelEvent(new MouseWheelEvent(new Component() {}, MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(), modifiers, x, y, 0, false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, rotation));
    }

    public void keyEvent(int id, int modifiers, int keyCode, char character) {
        sendKeyEvent(new KeyEvent(new Component() {}, id, System.currentTimeMillis(),
                modifiers, keyCode, character));
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return this;
    }

    @Override
    protected CefBrowser_N createDevToolsBrowser(CefClient client, String url,
            CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        TidalOsrBrowser devTools = new TidalOsrBrowser(
                client, url, context, width, height, scale, paintListener);
        devTools.createDevTools(parent, client, 0L, true, false, null, inspectAt);
        return devTools;
    }

    @Override public Rectangle getViewRect(CefBrowser browser) {
        return new Rectangle(0, 0, width, height);
    }

    @Override public boolean getScreenInfo(CefBrowser browser, CefScreenInfo info) {
        info.Set(scale, 32, 8, false, getViewRect(browser), getViewRect(browser));
        return true;
    }

    @Override public Point getScreenPoint(CefBrowser browser, Point point) {
        return new Point(point);
    }

    @Override public void onPopupShow(CefBrowser browser, boolean show) {}
    @Override public void onPopupSize(CefBrowser browser, Rectangle rect) {}

    @Override public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects,
                                  ByteBuffer buffer, int width, int height) {
        CefPaintEvent event = new CefPaintEvent(browser, popup, dirtyRects, buffer, width, height);
        paintListener.accept(event);
        for (Consumer<CefPaintEvent> listener : paintListeners) listener.accept(event);
    }

    @Override public void addOnPaintListener(Consumer<CefPaintEvent> listener) {
        paintListeners.add(listener);
    }

    @Override public void setOnPaintListener(Consumer<CefPaintEvent> listener) {
        paintListeners.clear();
        paintListeners.add(listener);
    }

    @Override public void removeOnPaintListener(Consumer<CefPaintEvent> listener) {
        paintListeners.remove(listener);
    }

    @Override public boolean onCursorChange(CefBrowser browser, int cursorType) { return false; }
    @Override public boolean startDragging(CefBrowser browser, CefDragData data,
                                           int mask, int x, int y) { return false; }
    @Override public void updateDragCursor(CefBrowser browser, int operation) {}
}
