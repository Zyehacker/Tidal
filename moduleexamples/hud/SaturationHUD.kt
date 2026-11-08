package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.ColorSetting
import net.minecraft.client.gui.GuiGraphicsExtractor

class SaturationHUD : Module("Saturation", "Shows hidden hunger saturation", Category.HUD) {
    private val showBar = register(BooleanSetting("show_bar", "Show progress bar", true))
    private val background = register(BooleanSetting("background", "Show background", true))
    private val saturationColor = register(ColorSetting("saturation_color", "Saturation color", 103, 232, 249))
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { if (!isInGame()) return; val player = mc.player ?: return; val x = hudX; val y = hudY; val saturation = player.foodData.saturationLevel; val food = player.foodData.foodLevel; val argb = saturationColor.getRGB() or Int.MIN_VALUE; graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); if (background.isEnabled()) AquaUI.hudPanel(graphics, x - 5, y - 4, 90, (if (showBar.isEnabled()) 18 else 12) + 6); graphics.text(mc.font, "Sat: %.1f/%d".format(saturation, food), x, y, argb, true); if (showBar.isEnabled()) drawProgressBar(graphics, x, y + 10, 80, 4, saturation / 20f, argb); graphics.pose().popMatrix() }
    private fun drawProgressBar(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, progress: Float, color: Int) { AquaUI.roundedRect(graphics, x, y, width, height, 2, -1441128412); val filled = (width * progress.coerceIn(0f, 1f)).toInt(); if (filled > 0) AquaUI.roundedRect(graphics, x, y, filled, height, 2, color); AquaUI.outline(graphics, x, y, width, height, 1154997472) }
    override fun getHudWidth() = 90
    override fun getHudHeight() = if (showBar.isEnabled()) 24 else 18
}