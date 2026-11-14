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

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import cc.tidal.tidal.mixin.KeyMappingAccessor;
import org.lwjgl.glfw.GLFW;

public final class TidalClient implements ClientModInitializer {
    private static final boolean CEF_ENABLED = false;
    private static KeyMapping openDrawer;
    private static KeyMapping openHudEditor;
    private static TidalAquaIntegration aquaIntegration;
    private static boolean viewBobbingWasEnabled;
    private static boolean originalViewBobbing;
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tidal", "main"));

    @Override
    public void onInitializeClient() {
        TidalConfig.load();
        if (!CEF_ENABLED) TidalConfig.useCefUi = false;
        Minecraft minecraft = Minecraft.getInstance();
        TidalData.refresh(minecraft);
        aquaIntegration = TidalAquaIntegration.start();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
                TidalConfig.initializeHudLayouts(client.getWindow().getGuiScaledWidth(),
                        client.getWindow().getGuiScaledHeight());
                LiquidGlassAssets.preload(LiquidGlassAssets.all().stream()
                        .map(LiquidGlassAssets.Asset::path).toList());
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            restoreVisualOverrides(client);
            if (aquaIntegration != null) aquaIntegration.close();
        });
        openDrawer = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tidal.open_drawer", InputConstants.Type.KEYSYM,
                InputConstants.KEY_RSHIFT, CATEGORY));
        openHudEditor = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tidal.open_hud_editor", InputConstants.Type.KEYSYM,
                InputConstants.KEY_H, CATEGORY));

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            String screenName = screen.getClass().getSimpleName();
            if (screenName.equals("ConnectScreen") || screenName.equals("ReceivingLevelScreen")
                    || screenName.equals("LevelLoadingScreen")) {
                TidalLoadingOverlay overlay =
                        new TidalLoadingOverlay(scaledWidth, scaledHeight, screen);
                ScreenEvents.afterExtract(screen).register((currentScreen, graphics, mouseX,
                                                            mouseY, tickProgress) ->
                        overlay.extractRenderState(graphics, mouseX, mouseY, tickProgress));
            }
            if (TidalWatermark.supports(screenName)) {
                ScreenEvents.afterExtract(screen).register((currentScreen, graphics, mouseX,
                                                            mouseY, tickProgress) ->
                        TidalWatermark.render(graphics));
            }
            if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
                int buttonWidth = 92;
                Screens.getWidgets(screen).add(new TidalUi.ActionButton(
                        Math.max(8, scaledWidth - buttonWidth - 12), 8,
                        buttonWidth, 24, "TIDAL",
                        TidalUi.ActionButton.Style.CONTROL,
                        () -> openTidal(client, screen)));
            }
            ScreenKeyboardEvents.allowKeyPress(screen).register((currentScreen, event) -> {
                if (!openDrawer.matches(event)
                        || currentScreen.getFocused() instanceof EditBox) return true;
                openTidal(client, currentScreen);
                return false;
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            TidalModManager.tick();
            if (aquaIntegration != null && client.player != null) {
                for (TidalAquaIntegration.NotificationEvent event
                        : aquaIntegration.drainNotifications(client.player.getUUID())) {
                    net.minecraft.client.gui.components.toasts.SystemToast.add(
                            client.gui.toastManager(),
                            new net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId(),
                            net.minecraft.network.chat.Component.literal(event.title()),
                            net.minecraft.network.chat.Component.literal(event.message()));
                }
            }
            while (openHudEditor.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new TidalHudEditorScreen(null));
                }
            }
            while (openDrawer.consumeClick()) {
                if (client.gui.screen() == null) {
                    openTidal(client, null);
                }
            }
            if (client.level != null && client.level.getGameTime() % 10 == 0) {
                TidalData.refresh(client);
            }
            TidalData.refreshInput(client);
            updateVisualOverrides(client);
            if (client.gui.screen() == null) {
                for (TidalModule module : TidalModules.all()) {
                    if (!module.implemented || module.keyCode == 0) continue;
                    boolean down = module.keyCode < 0
                            ? GLFW.glfwGetMouseButton(client.getWindow().handle(),
                            -module.keyCode - 1) == GLFW.GLFW_PRESS
                            : InputConstants.isKeyDown(client.getWindow(), module.keyCode);
                    if (down && !module.wasDown) {
                        module.enabled = !module.enabled;
                        if (module.enabled) TidalHud.placeInAvailableSpace(module);
                        TidalConfig.save();
                    }
                    module.wasDown = down;
                }
            } else {
                for (TidalModule module : TidalModules.all()) module.wasDown = false;
            }
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("tidal", "modules"),
                (graphics, deltaTracker) -> TidalHud.render(graphics));
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla ->
                (graphics, deltaTracker) -> {
                    TidalModule crosshair = TidalModules.byId("custom_crosshair");
                    if (crosshair != null && crosshair.enabled) {
                        TidalCrosshair.render(graphics);
                    } else {
                        vanilla.extractRenderState(graphics, deltaTracker);
                    }
                });
    }

    private static void openTidal(Minecraft client, Screen previousScreen) {
        client.gui.setScreen(new TidalScreen(previousScreen));
    }

    static TidalAquaIntegration.AccountStatus aquaAccountStatus() {
        return aquaIntegration == null
                ? TidalAquaIntegration.AccountStatus.CONNECTION_UNAVAILABLE
                : aquaIntegration.accountStatus();
    }

    static String authenticatedAquaUsername() {
        return aquaIntegration == null ? null : aquaIntegration.authenticatedUsername();
    }

    static void refreshAquaIntegration() {
        if (aquaIntegration != null) aquaIntegration.refresh();
    }

    private static void updateVisualOverrides(Minecraft client) {
        TidalModule viewBobbing = TidalModules.byId("view_bobbing");
        if (viewBobbing == null) return;

        if (viewBobbing.enabled) {
            if (!viewBobbingWasEnabled) originalViewBobbing = client.options.bobView().get();
            client.options.bobView().set(false);
        } else if (viewBobbingWasEnabled) {
            client.options.bobView().set(originalViewBobbing);
        }
        viewBobbingWasEnabled = viewBobbing.enabled;

        TidalModule sprint = TidalModules.byId("toggle_sprint");
        TidalModule sneak = TidalModules.byId("toggle_sneak");
        client.options.keySprint.setDown(sprint != null && sprint.enabled
                || InputConstants.isKeyDown(client.getWindow(),
                ((KeyMappingAccessor) (Object) client.options.keySprint)
                        .tidal$getKey().getValue()));
        client.options.keyShift.setDown(sneak != null && sneak.enabled
                || InputConstants.isKeyDown(client.getWindow(),
                ((KeyMappingAccessor) (Object) client.options.keyShift)
                        .tidal$getKey().getValue()));
    }

    private static void restoreVisualOverrides(Minecraft client) {
        if (viewBobbingWasEnabled) client.options.bobView().set(originalViewBobbing);
        client.options.keySprint.setDown(false);
        client.options.keyShift.setDown(false);
        viewBobbingWasEnabled = false;
    }
}
