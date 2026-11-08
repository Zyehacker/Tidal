package com.aqua.hud.modules.impl.render

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.render.Theme

class PingOverlay : Module("PingOverlay", "Show numeric ping on the player list", Category.RENDER) {
    private val colorCoded = register(BooleanSetting("color_coded", "Color code by ping quality", true))
    private val showMs = register(BooleanSetting("show_ms", "Show 'ms' suffix", true))
    fun formatPing(latency: Int) = when { latency <= 0 -> "?"; showMs.isEnabled() -> "${latency}ms"; else -> latency.toString() }
    fun getPingColor(latency: Int) = when { !colorCoded.isEnabled() -> -1; latency <= 0 -> -5592406; latency < 75 -> Theme.STATE_GOOD; latency < 300 -> Theme.STATE_WARNING; else -> Theme.STATE_CRITICAL }
}