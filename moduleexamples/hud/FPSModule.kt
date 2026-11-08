package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaFonts
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.ColorSetting
import com.aqua.hud.modules.settings.ModeSetting
import com.aqua.hud.modules.settings.NumberSetting
import com.aqua.hud.render.Theme
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.util.ArrayDeque

class FPSModule : Module("FPS", "Shows current FPS with rolling average and minimum readings, updating about every 50 ms with configurable warning colors.", Category.HUD) {
    private val showAverage = register(BooleanSetting("show_average", "Show average FPS", true))
    private val showMin = register(BooleanSetting("show_min", "Show minimum FPS", true))
    private val colorMode = register(ModeSetting("color_mode", "Color mode", "dynamic", arrayOf("dynamic", "static")))
    private val staticColor = register(ColorSetting("color", "Static color", 255, 255, 255))
    private val warningThreshold = register(NumberSetting("warning_fps", "FPS warning threshold", 60.0, 15.0, 240.0, 5.0))
    private val criticalThreshold = register(NumberSetting("critical_fps", "FPS critical threshold", 30.0, 5.0, 120.0, 5.0))
    private val background = register(BooleanSetting("background", "Show background", true))
    private val bgOpacity = register(NumberSetting("opacity", "Background opacity", 80.0, 0.0, 255.0, 5.0))
    private val history = ArrayDeque<Int>(); private var minFps = Int.MAX_VALUE; private var average = 0; private var lastUpdate = 0L
    init { staticColor.visibleWhen { colorMode.`is`("static") }; bgOpacity.visibleWhen { background.isEnabled() } }
    override fun onEnable() { history.clear(); minFps = Int.MAX_VALUE }
    override fun onTick() { if (System.currentTimeMillis() - lastUpdate <= 50) return; val fps = mc.fps; history.addLast(fps); if (history.size > 100) history.removeFirst(); if (fps > 0) minFps = minOf(minFps, fps); average = if (history.isEmpty()) 0 else history.average().toInt(); lastUpdate = System.currentTimeMillis() }
    private fun displayColor(fps: Int) = if (colorMode.`is`("static")) staticColor.getRGB() else if (fps >= warningThreshold.getIntValue()) Theme.STATE_GOOD else if (fps >= criticalThreshold.getIntValue()) Theme.STATE_WARNING else Theme.STATE_CRITICAL
    private fun displayText(fps: Int) = buildString { append("FPS: $fps"); if (showAverage.isEnabled()) append(" (Avg: $average)"); if (showMin.isEnabled() && minFps != Int.MAX_VALUE) append(" (Min: $minFps)") }
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { if (!isInGame()) return; val x = hudX; val y = hudY; val value = displayText(mc.fps); val color = displayColor(mc.fps); graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); if (background.isEnabled()) AquaUI.hudPanel(graphics, x - 5, y - 4, mc.font.width(AquaFonts.hud(value)) + 10, 17, bgOpacity.getIntValue()); graphics.text(mc.font, AquaFonts.hud(value), x, y, color, true); graphics.pose().popMatrix() }
    override fun getHudWidth() = mc.font.width(AquaFonts.hud(displayText(mc.fps))) + 10
    override fun getHudHeight() = 17
}