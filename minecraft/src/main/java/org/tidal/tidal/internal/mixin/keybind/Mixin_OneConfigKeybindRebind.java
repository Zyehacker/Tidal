package cc.tidal.tidal.internal.mixin.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
//? if >= 1.21.10
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
//? if >=1.21.10 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
//? if < 26.3
//import org.lwjgl.glfw.GLFW;
//? if >= 26.3
import org.lwjgl.sdl.SDLMouse;
import cc.tidal.tidal.api.platform.v1.Platform;
import cc.tidal.tidal.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import cc.tidal.tidal.internal.ui.keybind.TidalKeybindRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;

@Mixin(KeyBindsScreen.class)
public class Mixin_TidalKeybindRebind implements TidalKeybindRecorder {
    @Shadow public KeyMapping selectedKey;
    @Shadow private KeyBindsList keyBindsList;

    @Unique private final LinkedHashSet<Integer> tidal$keys = new LinkedHashSet<>();
    @Unique private final LinkedHashSet<Integer> tidal$mouse = new LinkedHashSet<>();
    @Unique private boolean tidal$recording = false;
    @Unique private KeyMapping tidal$target = null;

    //? if >=1.21.10 {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void tidal$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!tidal$isOurs()) return;
        if (event.key() == InputConstants.KEY_ESCAPE) {
            tidal$recordEscape();
            cir.setReturnValue(true);
            return;
        }
        tidal$recordKey(event.key());
        cir.setReturnValue(true);
    }
    //?} else {
    /*@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void tidal$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!tidal$isOurs()) return;
        if (keyCode == InputConstants.KEY_ESCAPE) {
            tidal$recordEscape();
            cir.setReturnValue(true);
            return;
        }
        tidal$recordKey(keyCode);
        cir.setReturnValue(true);
    }
    *///?}

    //? if >=1.21.10 {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tidal$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!tidal$isOurs()) return;
        tidal$recordMouse(event.button());
        cir.setReturnValue(true);
    }
    //?} else {
    /*@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tidal$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!tidal$isOurs()) return;
        tidal$recordMouse(button);
        cir.setReturnValue(true);
    }
    *///?}

    //? if >=26.1 {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void tidal$poll(CallbackInfo ci) {
        tidal$pollInputs();
    }
    //?} else {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void tidal$poll(CallbackInfo ci) {
        tidal$pollInputs();
    }
    *///?}

    @Unique
    private void tidal$pollInputs() {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge != null) bridge.setActiveRebind(tidal$target != null ? tidal$target : this.selectedKey);
        if (!tidal$recording) return;
        for (int k : tidal$keys) {
            //? if sdl_keycodes {
            if (InputConstants.isKeyDown(k)) return;
            //?} else {
            /*//~ if < 1.21.10 'Minecraft.getInstance().getWindow()' -> 'Platform.compatibility().windowHandle()'
            if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), k)) return;
            *///?}
        }
        //? if >= 26.3 {
        int buttons = SDLMouse.SDL_GetMouseState(null, null);
        for (int b : tidal$mouse) {
            if (b > 0 && b <= Integer.SIZE && (buttons & (1 << (b - 1))) != 0) return;
        }
        //?} else {
        /*long window = Platform.compatibility().windowHandle();
        for (int b : tidal$mouse) if (GLFW.glfwGetMouseButton(window, b) != InputConstants.RELEASE) return;
        *///?}
        tidal$commit();
    }

    @Override
    public boolean tidal$isOurs() {
        if (tidal$target != null) return true;
        KeyMapping sel = this.selectedKey;
        if (sel == null) return false;
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        return bridge != null && bridge.bindFor(sel) != null;
    }

    @Unique
    private boolean tidal$begin() {
        if (tidal$target == null) {
            KeyMapping sel = this.selectedKey;
            MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
            if (sel == null || bridge == null || bridge.bindFor(sel) == null) return false;
            tidal$target = sel;
        }
        tidal$recording = true;
        return true;
    }

    @Override
    public void tidal$recordKey(int keyCode) {
        //? if sdl_keycodes
        if (keyCode <= 0) return;
        if (!tidal$begin()) return;
        tidal$keys.add(keyCode);
        tidal$preview();
    }

    @Unique
    private void tidal$recordMouse(int button) {
        if (!tidal$begin()) return;
        tidal$mouse.add(button);
        tidal$preview();
    }

    @Override
    public void tidal$recordEscape() {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        KeyMapping target = tidal$target != null ? tidal$target : this.selectedKey;
        if (bridge != null && target != null) {
            bridge.clearPreview();
            bridge.menuUnbind(target);
        }
        tidal$reset();
        tidal$finishSelection();
    }

    @Unique
    private void tidal$preview() {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null || tidal$target == null) return;
        bridge.setPreview(tidal$target, tidal$toArray(tidal$keys), tidal$toArray(tidal$mouse));
        if (this.keyBindsList != null) this.keyBindsList.resetMappingAndUpdateButtons();
    }

    @Unique
    private void tidal$commit() {
        KeyMapping sel = tidal$target;
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (sel != null && bridge != null && (!tidal$keys.isEmpty() || !tidal$mouse.isEmpty())) {
            bridge.menuRebind(sel, tidal$toArray(tidal$keys), tidal$toArray(tidal$mouse), true);
        }
        if (bridge != null) bridge.clearPreview();
        tidal$reset();
        tidal$finishSelection();
    }

    @Unique
    private void tidal$finishSelection() {
        this.selectedKey = null;
        if (this.keyBindsList != null) this.keyBindsList.resetMappingAndUpdateButtons();
    }

    @Unique
    private void tidal$reset() {
        tidal$recording = false;
        tidal$keys.clear();
        tidal$mouse.clear();
        tidal$target = null;
    }

    @Unique
    private static int[] tidal$toArray(LinkedHashSet<Integer> set) {
        if (set.isEmpty()) return null;
        int[] out = new int[set.size()];
        int i = 0;
        for (int v : set) out[i++] = v;
        return out;
    }
}
