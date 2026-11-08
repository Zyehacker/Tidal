package com.aqua.hud.modules.impl.utility

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.NumberSetting

class ZoomModule : Module("Zoom", "Zoom the camera", Category.UTILITY) {
    private val zoomLevel = register(NumberSetting("zoom_level", "Zoom multiplier", 4.0, 1.5, 10.0, 0.5))
    private val holdToZoom = register(BooleanSetting("hold_to_zoom", "Hold key to zoom (vs toggle)", true))
    private val smoothZoom = register(BooleanSetting("smooth_zoom", "Smooth zoom transition", false))
    private var savedFov = -1
    private var animatedFov = -1.0
    init { setKeyHeld(holdToZoom.isEnabled()) }
    override fun isHotkeyOnly() = true
    override fun setEnabled(value: Boolean) { if (!value || isInGame()) super.setEnabled(value) }
    private fun updateHoldMode() { setKeyHeld(holdToZoom.isEnabled()) }
    override fun onEnable() { updateHoldMode(); savedFov = mc.options.fov().get(); val target = savedFov / zoomLevel.getValue(); if (smoothZoom.isEnabled()) animatedFov = savedFov.toDouble() else setFov(target) }
    override fun onDisable() { if (savedFov > 0) setFov(savedFov.toDouble()); savedFov = -1; animatedFov = -1.0 }
    override fun onTick() { if (isKeyHeld() != holdToZoom.isEnabled()) updateHoldMode(); if (savedFov > 0) { val target = savedFov / zoomLevel.getValue(); if (smoothZoom.isEnabled()) { val diff = target - animatedFov; animatedFov = if (kotlin.math.abs(diff) > 0.5) animatedFov + diff * 0.3 else target; setFov(animatedFov) } else setFov(target) } }
    private fun setFov(value: Double) { runCatching { mc.options.fov().set(value.coerceIn(30.0, 110.0).toInt()) } }
}