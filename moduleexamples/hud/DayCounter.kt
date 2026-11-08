package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaFonts
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.ColorSetting
import net.minecraft.client.gui.GuiGraphicsExtractor

class DayCounter : Module("DayCounter", "Shows in-game day count", Category.HUD) {
    private val showLabel = register(BooleanSetting("show_label", "Show 'Day' label", true))
    private val showTime = register(BooleanSetting("show_time", "Show in-game time", false))
    private val background = register(BooleanSetting("background", "Show background", true))
    private val color = register(ColorSetting("color", "Text color", 103, 232, 249))
    private fun display(): String { val time = mc.level?.overworldClockTime ?: return "Day 1"; val day = time / 24000 + 1; val prefix = if (showLabel.isEnabled()) "Day " else ""; if (!showTime.isEnabled()) return prefix + day; val hours = ((time % 24000 / 1000 + 6) % 24).toInt(); val minutes = (time % 1000 * 60 / 1000).toInt(); return "$prefix$day (%02d:%02d)".format(hours, minutes) }
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { val x = hudX; val y = hudY; val value = display(); graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); val argb = color.getRGB() or Int.MIN_VALUE; if (background.isEnabled()) AquaUI.hudText(graphics, mc.font, value, x, y, argb, true) else graphics.text(mc.font, AquaFonts.hud(value), x, y, argb, true); graphics.pose().popMatrix() }
    override fun getHudWidth() = mc.font.width(AquaFonts.hud(display())) + 10
    override fun getHudHeight() = 17
}