package cc.tidal.tidal.internal.ui.hud

import net.minecraft.client.gui.GuiGraphicsExtractor
import cc.tidal.tidal.api.hud.v1.HudManager
import cc.tidal.tidal.internal.ui.compose.SkiaCtx
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

object CompatOverlayRenderer {
    private val LOG = LoggerFactory.getLogger("Tidal/CompatOverlayRenderer")
    private val hooks = CopyOnWriteArrayList<(GuiGraphicsExtractor) -> Unit>()

    @JvmStatic
    fun register(hook: (GuiGraphicsExtractor) -> Unit) {
        hooks.add(hook)
    }

    @JvmStatic
    fun hasHooks(): Boolean = hooks.isNotEmpty()

    @JvmStatic
    fun oneConfigScreenOpen(): Boolean = HudManager.isEditing || SkiaCtx.suppressInGameHudRender

    @JvmStatic
    fun render(ctx: GuiGraphicsExtractor) {
        if (hooks.isEmpty()) return
        for (hook in hooks) {
            try {
                hook(ctx)
            } catch (t: Throwable) {
                LOG.debug("compat overlay hook failed", t)
            }
        }
    }
}
