package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaFonts
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.ColorSetting
import com.aqua.hud.modules.settings.ModeSetting
import com.aqua.hud.render.Theme
import net.minecraft.client.gui.GuiGraphicsExtractor

class MemoryHUD : Module("Memory", "Shows RAM/memory usage", Category.HUD) {
    private val displayMode = register(ModeSetting("display", "Display format", "percent", arrayOf("percent", "mb", "both")))
    private val colorByUsage = register(BooleanSetting("color_by_usage", "Color by usage", true))
    private val background = register(BooleanSetting("background", "Show background", true))
    private val staticColor = register(ColorSetting("color", "Static color", 255, 255, 255))
    init { staticColor.visibleWhen { !colorByUsage.isEnabled() } }
    private fun values(): Triple<Int, Long, Long> { val runtime = Runtime.getRuntime(); val max = runtime.maxMemory(); val used = runtime.totalMemory() - runtime.freeMemory(); return Triple((used * 100 / max).toInt(), used / 1048576, max / 1048576) }
    private fun display(): String { val (percent, used, max) = values(); return when (displayMode.getValue()) { "mb" -> "$used/$max MB"; "both" -> "$percent% ($used/$max MB)"; else -> "RAM: $percent%" } }
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { val x = hudX; val y = hudY; val value = display(); val percent = values().first; val color = if (!colorByUsage.isEnabled()) staticColor.getRGB() or Int.MIN_VALUE else if (percent < 50) Theme.STATE_GOOD else if (percent < 90) Theme.STATE_WARNING else Theme.STATE_CRITICAL; graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); if (background.isEnabled()) AquaUI.hudText(graphics, mc.font, value, x, y, color, true) else graphics.text(mc.font, AquaFonts.hud(value), x, y, color, true); graphics.pose().popMatrix() }
    override fun getHudWidth() = mc.font.width(AquaFonts.hud(display())) + 10
    override fun getHudHeight() = 17
}