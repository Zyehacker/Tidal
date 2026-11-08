package cc.tidal.tidal.internal.mixin.events;

// TODO
//import com.llamalad7.mixinextras.sugar.Local;
//import net.minecraft.client.Minecraft;
//import cc.tidal.tidal.api.event.v1.EventManager;
//import cc.tidal.tidal.api.event.v1.events.HudEvent;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(KeyBind.class)
//public abstract class Mixin_TabOpenedEvent {
//    @Unique
//    private static boolean tidal$tab$open = false;
//
//    @Inject(method = "setKeyBindState", at = @At("TAIL"))
//    private static void onTabOpened(
//            //#if MC < 1.13
//            int keyCode,
//            //#else
//            //$$ @org.spongepowered.asm.mixin.injection.Coerce Object key,
//            //#endif
//            boolean pressed, CallbackInfo ci, @Local KeyBinding bind) {
//        if (pressed == tidal$tab$open) return;
//        if (bind == Minecraft.getMinecraft().gameSettings.keyBindPlayerList) {
//            if (pressed) {
//                EventManager.INSTANCE.post(HudEvent.Tab.OPENED);
//                tidal$tab$open = true;
//            } else {
//                EventManager.INSTANCE.post(HudEvent.Tab.CLOSED);
//                tidal$tab$open = false;
//            }
//        }
//    }
//}
