package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaFonts
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.ColorSetting
import com.aqua.hud.modules.settings.ModeSetting
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.text.SimpleDateFormat
import java.util.Date

class ClockHUD : Module("Clock", "Shows the current real-world time", Category.HUD) {
    private val format = register(ModeSetting("format", "Time format", "24h", arrayOf("24h", "12h", "24h_seconds", "12h_seconds")))
    private val showDate = register(BooleanSetting("show_date", "Show date", false))
    private val background = register(BooleanSetting("background", "Show background", true))
    private val color = register(ColorSetting("color", "Text color", 255, 255, 255))
    private val formats = mapOf("24h" to SimpleDateFormat("HH:mm"), "12h" to SimpleDateFormat("hh:mm a"), "24h_seconds" to SimpleDateFormat("HH:mm:ss"), "12h_seconds" to SimpleDateFormat("hh:mm:ss a"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy")
    private fun text(now: Date): String { val time = formats[format.getValue()]!!.format(now); return if (showDate.isEnabled()) "${dateFormat.format(now)} $time" else time }
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { val x = hudX; val y = hudY; val value = text(Date()); graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); val argb = color.getRGB() or Int.MIN_VALUE; if (background.isEnabled()) AquaUI.hudText(graphics, mc.font, value, x, y, argb, true) else graphics.text(mc.font, AquaFonts.hud(value), x, y, argb, true); graphics.pose().popMatrix() }
    override fun getHudWidth() = mc.font.width(AquaFonts.hud(text(Date()))) + 10
    override fun getHudHeight() = 17
}