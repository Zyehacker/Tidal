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

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TidalCefScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("Tidal CEF");
    private static final long FRAME_DISPLAY_TIMEOUT_NANOS = 5_000_000_000L;
    private static final Identifier TEXTURE_ID =
            Identifier.fromNamespaceAndPath("tidal", "cef/surface");
    private static DynamicTexture texture;
    private static int textureWidth;
    private static int textureHeight;

    private final Screen parent;
    private final long openedAtNanos = System.nanoTime();
    private boolean fallbackHandled;
    private boolean renderFailureLogged;
    private int lastKeyModifiers;

    private TidalCefScreen(Screen parent) {
        super(Component.literal("Tidal"));
        this.parent = parent;
    }

    static boolean open(Minecraft minecraft, Screen parent) {
        if (!TidalConfig.useCefUi) return false;
        TidalModManager.initialize();
        int guiWidth = Math.max(1, minecraft.getWindow().getGuiScaledWidth());
        int guiHeight = Math.max(1, minecraft.getWindow().getGuiScaledHeight());
        double scaleX = minecraft.getWindow().getWidth() / (double) guiWidth;
        double scaleY = minecraft.getWindow().getHeight() / (double) guiHeight;
        if (!TidalCefManager.activate(guiWidth, guiHeight,
                scaleX, scaleY)) {
            return false;
        }
        minecraft.gui.setScreen(new TidalCefScreen(parent));
        return true;
    }

    static void releaseTexture() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || texture == null) return;
        minecraft.getTextureManager().release(TEXTURE_ID);
        texture = null;
        textureWidth = 0;
        textureHeight = 0;
    }

    @Override
    protected void init() {
        TidalCefManager.refreshForScreenOpen(width, height,
                minecraft.getWindow().getWidth() / (double) Math.max(1, width),
                minecraft.getWindow().getHeight() / (double) Math.max(1, height));
    }

    @Override
    public void added() {
        TidalCefManager.setVisible(true);
    }

    @Override
    public void removed() {
        TidalCefManager.setVisible(false);
    }

    @Override
    public void tick() {
        TidalCefManager.tick();
        if (!fallbackHandled && !TidalCefManager.isFailed()
                && !TidalCefManager.isRestartRequired() && !TidalCefManager.hasFrame()
                && TidalCefManager.hasDisplayRefreshAttemptSince(openedAtNanos)
                && System.nanoTime() - openedAtNanos >= FRAME_DISPLAY_TIMEOUT_NANOS) {
            TidalCefManager.failForNoDisplayedFrame();
        }
        if (!fallbackHandled && (TidalCefManager.isFailed()
                || TidalCefManager.isRestartRequired())) {
            requestNativeFallback();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        resizeBrowser();
    }

    private void resizeBrowser() {
        TidalCefManager.resize(width, height,
                minecraft.getWindow().getWidth() / (double) Math.max(1, width),
                minecraft.getWindow().getHeight() / (double) Math.max(1, height));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float delta) {
        try {
            if (fallbackHandled) {
                super.extractRenderState(graphics, mouseX, mouseY, delta);
                return;
            }
            boolean paintArrived = TidalCefManager.hasPaint();
            if (paintArrived) {
                ensureTexture(TidalCefManager.frameWidth(), TidalCefManager.frameHeight());
                if (texture != null) TidalCefManager.uploadDirtyFrame(texture);
            }
            boolean frameReady = TidalCefManager.hasFrame();
            if (frameReady && texture != null) {
                int framebufferWidth = Math.max(1, minecraft.getWindow().getWidth());
                int framebufferHeight = Math.max(1, minecraft.getWindow().getHeight());
                int drawWidth = Math.min(width, Math.max(1,
                        (int) Math.round(textureWidth * width / (double) framebufferWidth)));
                int drawHeight = Math.min(height, Math.max(1,
                        (int) Math.round(textureHeight * height / (double) framebufferHeight)));
                int drawX = (width - drawWidth) / 2;
                int drawY = (height - drawHeight) / 2;
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, drawX, drawY, 0, 0,
                        drawWidth, drawHeight, textureWidth, textureHeight,
                        textureWidth, textureHeight, 0xFFFFFFFF);
            }
            super.extractRenderState(graphics, mouseX, mouseY, delta);
        } catch (RuntimeException | LinkageError failure) {
            if (!renderFailureLogged) {
                renderFailureLogged = true;
                TidalCefManager.failForRenderException(failure);
                LOGGER.error("CEF screen render path failed; switching to the native interface.",
                        failure);
            }
            requestNativeFallback();
        }
    }

    private void ensureTexture(int width, int height) {
        if (width <= 0 || height <= 0
                || texture != null && width == textureWidth && height == textureHeight) return;
        releaseTexture();
        texture = new CefTexture(width, height);
        minecraft.getTextureManager().register(TEXTURE_ID, texture);
        textureWidth = width;
        textureHeight = height;
    }

    private void requestNativeFallback() {
        if (fallbackHandled) return;
        fallbackHandled = true;
        minecraft.execute(() -> {
            TidalCefManager.setVisible(false);
            releaseTexture();
            minecraft.gui.setScreen(parent == null
                    ? TidalScreen.settingsScreen() : parent);
        });
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        lastKeyModifiers = event.modifiers();
        return TidalCefManager.dispatchKey(
                event.key(), event.scancode(), event.modifiers(), true);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        lastKeyModifiers = event.modifiers();
        return TidalCefManager.dispatchKey(
                event.key(), event.scancode(), event.modifiers(), false);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return TidalCefManager.dispatchCharacter(event.codepoint(), lastKeyModifiers);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        TidalCefManager.dispatchMouse(java.awt.event.MouseEvent.MOUSE_MOVED,
                mouseX, mouseY, -1, 1, 0, width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return TidalCefManager.dispatchMouse(java.awt.event.MouseEvent.MOUSE_PRESSED,
                event.x(), event.y(), event.button(), doubleClick ? 2 : 1,
                event.modifiers(), width, height);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return TidalCefManager.dispatchMouse(java.awt.event.MouseEvent.MOUSE_RELEASED,
                event.x(), event.y(), event.button(), 1, event.modifiers(), width, height);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return TidalCefManager.dispatchMouse(java.awt.event.MouseEvent.MOUSE_DRAGGED,
                event.x(), event.y(), event.button(), 1, event.modifiers(), width, height);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                 double verticalAmount) {
        return TidalCefManager.dispatchScroll(
                mouseX, mouseY, verticalAmount, 0, width, height);
    }

    @Override
    public void onClose() {
        TidalCefManager.setVisible(false);
        minecraft.gui.setScreen(parent);
    }

    static void closeFromPage() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof TidalCefScreen screen) screen.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class CefTexture extends DynamicTexture {
        private CefTexture(int width, int height) {
            super("Tidal CEF surface", width, height, false);
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
    }
}
