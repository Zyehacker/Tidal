package org.tidal.compose.node

import org.tidal.compose.render.RenderContext

class CanvasNode : PolyNode() {
    var onDraw: (RenderContext.(x: Float, y: Float, width: Float, height: Float) -> Unit)? = null

    override fun render(ctx: RenderContext) {
        onDraw?.invoke(ctx, x, y, width, height)
    }
}
