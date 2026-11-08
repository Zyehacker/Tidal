/*
 * This file is part of Tidal.
 * Tidal - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Tidal.
 *   <https://tidal.org> <https://github.com/Tidal/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   Tidal is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to Tidal, as published by Tidal,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to Tidal, as published by Tidal. If not, see
 * <https://tidal.org/legal/tidal/additional-terms>
 */

//~ gui_graphics
package cc.tidal.tidal.internal;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import cc.tidal.compose.render.RenderContext;
import cc.tidal.tidal.api.commands.v1.CommandManager;
import cc.tidal.tidal.api.config.v1.ConfigManager;
import cc.tidal.tidal.api.event.v1.EventManager;
import cc.tidal.tidal.api.event.v1.events.InitializationEvent;
import cc.tidal.tidal.api.event.v1.events.ResourceFinishedLoading;
import cc.tidal.tidal.api.event.v1.events.ScreenOpenEvent;
import cc.tidal.tidal.api.event.v1.events.ShutdownEvent;
import cc.tidal.tidal.api.event.v1.events.WorldEvent;
import cc.tidal.tidal.api.hud.v1.HudManager;
import cc.tidal.tidal.api.hud.v1.events.HudEditorToggleEvent;
import cc.tidal.tidal.api.hypixel.v1.HypixelUtils;
import cc.tidal.tidal.api.notifications.v1.Notification;
import cc.tidal.tidal.api.notifications.v1.NotificationType;
import cc.tidal.tidal.api.notifications.v1.Notifications;
import cc.tidal.tidal.api.notifications.v1.NotificationsRenderer;
import cc.tidal.tidal.api.platform.v1.ModInfo;
import cc.tidal.tidal.api.platform.v1.Platform;
import cc.tidal.tidal.internal.ui.api.ConfigRegistry;
import cc.tidal.tidal.internal.ui.api.ConfigSource;
import cc.tidal.tidal.internal.ui.api.ThirdPartyModCategories;
import cc.tidal.tidal.internal.ui.compose.McFontService;
import cc.tidal.tidal.internal.ui.compose.SkiaCtx;
import cc.tidal.tidal.internal.ui.compose.impls.HudEditorUIScreen;
import cc.tidal.tidal.internal.ui.compose.impls.TidalUIScreen;
import cc.tidal.tidal.internal.ui.hud.LegacyHudRenderer;
import cc.tidal.tidal.internal.ui.keybind.KeybindProviderRegistry;
import cc.tidal.tidal.internal.ui.keybind.MinecraftKeybindProvider;
import cc.tidal.tidal.internal.ui.keybind.MinecraftKeybindProfiles;
import cc.tidal.tidal.internal.ui.keybind.KeybindConflicts;
import cc.tidal.tidal.internal.ui.search.SearchCorpus;
import cc.tidal.tidal.test.TestMod_Test;

//? neoforge
//@net.neoforged.fml.common.Mod("tidalv1")
public class Tidal
        //? fabric
        implements net.fabricmc.api.ClientModInitializer {
    public static final Tidal INSTANCE = new Tidal();
    private static final Logger LOGGER = LogManager.getLogger("Tidal");
    private boolean initialized = false;

    private static void registerCommands() {
        Command<FabricClientCommandSource> executor = (c) -> {
            TidalUIScreen.openLastSession();
            return Command.SINGLE_SUCCESS;
        };

        var node = CommandManager.literal("tidal")
                .executes(executor)
                .then(CommandManager.literal("delete")
                        .executes((ctx) -> {
                            ctx.getSource()
                                    .sendFeedback(Component.literal(
                                            "Deleted Tidal UI. Please make a report if you were having issues!"));
                            return 1;
                        })
                )
                .then(CommandManager.literal("hud")
                        .executes((ctx) -> {
                            HudManager.INSTANCE.openEditor();
                            return Command.SINGLE_SUCCESS;
                        })
                ).build();
        CommandManager.INSTANCE.register(node);
        CommandManager.INSTANCE.register(CommandManager.literal("ocfg").executes(executor).redirect(node));
    }

    private static void registerKeybinds() {
        // the action lives here rather than on the keybind because it references platform classes and
        // is lost when the keybind is deserialized
        TidalConfig.setOpenAction(pressed -> {
            if (!pressed) {
                return true;
            }
            // the screen may have closed itself on this very press (see notifyKeybindClosedGui) so
            // reopening here would make the keybind look like it does nothing
            if (TidalConfig.consumeKeybindClose()) {
                return true;
            }
            Object screen = Platform.screen().current();
            if (screen != null && !(Platform.compatibility().isDevelopment() && screen instanceof TitleScreen)) {
                return true;
            }
            if (Minecraft.getInstance().level == null && !Platform.compatibility().isDevelopment()) {
                return true;
            }
            try {
                TidalUIScreen.openLastSession();
            } catch (Throwable t) {
                //~ if >= 26.2 'gui.getChat' -> 'gui.hud.getChat'
                Minecraft.getInstance().gui.hud.getChat()
                        //~ if >= 26.1 'addMessage' -> 'addClientSystemMessage'
                        .addClientSystemMessage(Component.literal("Failed to open Tidal UI: " + t.getMessage() + ". Please report this!")
                                .withStyle(
                                        ChatFormatting.RED));
                throw t;
            }
            return true;
        });
    }

    /**
     * Mirrors vanilla's tab list visibility check which is more than the keybind being held
     * <p>
     * In a single player world with no other listed players and no LIST scoreboard objective the
     * tab list stays hidden even while the key is down
     */
    private static boolean isTabListVisible() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.keyPlayerList.isDown()) return false;
        if (minecraft.level == null || minecraft.player == null) return false;
        if (!minecraft.isLocalServer()) return true;

        Scoreboard scoreboard = minecraft.level.getScoreboard();
        return minecraft.player.connection.getListedOnlinePlayers().size() > 1
                || scoreboard.getDisplayObjective(DisplaySlot.LIST) != null;
    }

    public static void render(GuiGraphicsExtractor graphics, float partial) {
        if (!SkiaCtx.INSTANCE.isReady()) {
            return;
        }

        float sw = Platform.screen().guiWidth();
        float sh = Platform.screen().guiHeight();
        // guiWidth()/guiHeight() are already GUI-scaled so do not divide by guiScale again
        HudManager.guiScreenWidth = sw;
        HudManager.guiScreenHeight = sh;

        //? if >= 26.2 {
        HudManager.isGuiHidden = Minecraft.getInstance().gui.hud.isHidden();
        //? } else {
        /*HudManager.isGuiHidden = Minecraft.getInstance().options.hideGui;
        *///? }
        HudManager.isDebugScreenVisible = Minecraft.getInstance().getDebugOverlay().showDebugScreen();
        HudManager.isTabListVisible = isTabListVisible();
        HudManager.isGuiScreenOpen = Platform.screen().current() != null;
        HudManager.isChatScreenOpen = Platform.screen().current() instanceof ChatScreen;
        HudManager.inWorld = true;
        HudManager.targetPixelWidth = Platform.screen().viewportWidth();
        HudManager.targetPixelHeight = Platform.screen().viewportHeight();

        //~ if < 1.21.8 '.suppressInGameHudRender' -> '.shouldSuppressInGameHudRender()'
        boolean hudRendersLive = !SkiaCtx.INSTANCE.suppressInGameHudRender;
        if (hudRendersLive || !cc.tidal.tidal.internal.ui.hud.LegacyHudOffscreen.INSTANCE.render()) {
            LegacyHudRenderer.INSTANCE.renderLive(graphics);
        }
        // records the F3 overlay offscreen so Skia can put it above the Compose UI instead of below the
        // blur and it must run every frame regardless of the HUD dirty gate
        cc.tidal.tidal.internal.ui.hud.DebugOverlayOffscreen.INSTANCE.render();
        if (HudManager.INSTANCE.beginFrame(sw, sh)) {
            SkiaCtx.INSTANCE.queueHudDraw(() -> {
                var ctx = new RenderContext(SkiaCtx.INSTANCE.getCanvas());
                HudManager.INSTANCE.render(ctx, sw, sh);
            });
            // renders into the offscreen TextureTarget which the mixin blits onto MC's render target
            SkiaCtx.INSTANCE.drawNow();
        }
    }

    private static void installNotificationRenderer() {
        SkiaCtx.INSTANCE.setNotifRenderer(() -> {
            float sw = Platform.screen().guiWidth();
            float sh = Platform.screen().guiHeight();
            if (sw <= 0f || sh <= 0f) return;
            var ctx = new RenderContext(SkiaCtx.INSTANCE.getCanvas());
            NotificationsRenderer.render(ctx, sw, sh);
        });
    }

    private static void registerEventHandlers() {
        EventManager.register(ShutdownEvent.class, e -> MinecraftKeybindProfiles.shutdown());
        EventManager.register(InitializationEvent.class, e -> {
            HudManager.INSTANCE.setProfileReloadDispatcher(r -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && !mc.isSameThread()) mc.execute(r);
                else r.run();
            });
            HudManager.INSTANCE.initialize();
        });
        EventManager.register(
                HudEditorToggleEvent.class, e -> {
                    if (e.open) {
                        if (!(Platform.screen().current() instanceof HudEditorUIScreen)) {
                            Platform.screen().display(HudEditorUIScreen.open());
                        }
                    } else if (!e.screenAlreadyGone) {
                        if (Platform.screen().current() instanceof HudEditorUIScreen) {
                            Platform.screen().display(null, 0);
                        }
                    }
                });
        // resets the editing flag if another screen replaces the HUD editor without going through
        // HudManager.closeEditor() and null opens are ignored because commands close chat first
        EventManager.register(
                ScreenOpenEvent.class, e -> {
                    if (HudManager.INSTANCE.isEditorOpen() && e.getScreen() != null && !(e.getScreen() instanceof HudEditorUIScreen)) {
                        HudManager.INSTANCE.closeEditor();
                    }
                });
        EventManager.register(
                InitializationEvent.class, e -> {
                    ConfigManager.initialize();
                    KeybindConflicts.unbindMinecraftKeybinds();
                    cc.tidal.tidal.api.config.v1.CompatSnapshots.setDispatcher(r -> {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc != null && !mc.isSameThread()) mc.execute(r);
                        else r.run();
                    });
                    MinecraftKeybindProfiles.init();
                    ConfigRegistry.INSTANCE.loadFrom(ConfigManager.active(), ConfigSource.OC);
                    cc.tidal.tidal.internal.ui.hud.BuiltinHudRegistrar.register();
                    cc.tidal.tidal.api.hud.v1.HudManager.register(
                            new cc.tidal.tidal.modules.hud.PingDisplayHud(),
                            "tidal",
                            "hud"
                    );
                    cc.tidal.tidal.internal.compat.FirmamentHudCompat.register();
                    cc.tidal.tidal.internal.compat.ArmorHudCompat.register();
                    //? if wwaypoints_compat
                    cc.tidal.tidal.internal.compat.WWaypointsCompat.register();
                    cc.tidal.tidal.internal.ui.themes.ThemeRegistry.INSTANCE.loadFromConfig();
                });
        EventManager.register(WorldEvent.Load.class, e -> showFirstLaunchNotification());
        // after loading finishes so translation keys are available
        EventManager.register(ResourceFinishedLoading.class, e -> SearchCorpus.INSTANCE.init());
//        //#if MC < 1.13
//        // this is cringe but is better than the alternative of checking every frame in a mixin (that's how vanilla does it lol)
//        AtomicBoolean active = new AtomicBoolean(false);
//        EventManager.register(TickEvent.End.class, e -> {
//            boolean current = org.lwjgl.opengl.Display.isActive();
//            if (current != active.get()) {
//                active.set(current);
//                if (current) EventManager.INSTANCE.post(WindowFocusEvent.Gained.INSTANCE);
//                else EventManager.INSTANCE.post(WindowFocusEvent.Lost.INSTANCE);
//            }
//        });
//        //#endif
    }

    private static Notification firstLaunchToast;

    private static void showFirstLaunchNotification() {
        if (!TidalConfig.showFirstLaunchMessage) {
            return;
        }
        TidalConfig.markFirstLaunchShown();
        String keyName = TidalConfig.oneConfigKeybind.displayName();
        String title = Platform.i18n().translateString("tidal.notification.first_launch.title");
        String message = Platform.i18n().translateString("tidal.notification.first_launch.message", keyName);
        firstLaunchToast = Notifications.send(
                title,
                message,
                NotificationType.INFO,
                Notifications.icon("/assets/tidal/brand/tidal-icon.svg", 64),
                -1f,
                null,
                TidalUIScreen::openLastSession);
    }

    public static void dismissFirstLaunchToast() {
        if (firstLaunchToast != null) {
            Notifications.dismiss(firstLaunchToast);
            firstLaunchToast = null;
        }
    }

    //? neoforge {
    //static {
    //    INSTANCE.init();
    //}
    //? }
    @Override
    public void onInitializeClient() {
        init();
    }

    private void init() {
        if (initialized) {
            LOGGER.error("Attempted to initialize tidal twice! this will be ignored");
            return;
        }

        SkikoDataPath.redirect();

        // to enable RenderDoc set these JVM arguments
        // -Drenderdoc.enabled=true
        // (Windows) -Drenderdoc.path="C:\Program Files\RenderDoc\renderdoc.dll" (or wherever you installed RenderDoc)
        // (Linux) ensure librenderdoc.so is available in your LD_PRELOAD (TODO)
        //RenderDoc.init();

        if (Boolean.getBoolean("tidal.test")) {
            try {
                TestMod_Test.initialize();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        long t1 = System.nanoTime();
        ModInfo self = Platform.compatibility()
                .getMods()
                .stream()
                .filter(it -> "tidal".equals(it.getId()))
                .findFirst()
                .orElse(null);
        String v = self == null ? "LOCAL" : self.getVersion();
        LOGGER.info("Loading Tidal v{}", v);
        McFontService.INSTANCE.init();
        ThirdPartyModCategories.INSTANCE.init();
        // force class-load so the hello/disconnect handlers are armed before joining any server
        HypixelUtils.isHypixel();
        cc.tidal.tidal.internal.ui.sound.ExternalSounds.INSTANCE.ensureDownloaded();

        KeybindProviderRegistry.INSTANCE.register(MinecraftKeybindProvider.INSTANCE);
        registerKeybinds();
        new TidalConfig();
        new ThemeConfig();
        registerCommands();
        registerEventHandlers();
        installNotificationRenderer();
        MainMenuFpsSampler.init();
        //? fabric
        cc.tidal.tidal.internal.compat.ModMenuShimLoader.enable();
        cc.tidal.tidal.internal.compat.KaleidoCompat.enable();

        initialized = true;
        LOGGER.info("Tidal initialization took {}ms", (System.nanoTime() - t1) / 1_000_000.0);
    }

}
