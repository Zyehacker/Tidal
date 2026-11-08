package cc.tidal.tidal.internal.ui.hud

import com.mojang.blaze3d.pipeline.RenderTarget

object GuiTargetRedirect {
    @Volatile
    @JvmField
    var target: RenderTarget? = null
}
