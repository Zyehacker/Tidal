package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaFonts
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.NumberSetting
import com.aqua.hud.utils.PlayerUtils
import net.minecraft.client.gui.GuiGraphicsExtractor

class DirectionHUD : Module("DirectionHUD", "Shows facing direction and yaw", Category.HUD) {
    private val background = register(BooleanSetting("background", "Show background", true))
    private val bgOpacity = register(NumberSetting("opacity", "Background opacity", 80.0, 0.0, 255.0, 5.0))
    init { bgOpacity.visibleWhen { background.isEnabled() } }
    private fun yawText() = "%.1f°".format(((mc.player?.yRot ?: 0f) % 360 + 360) % 360)
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { if (!isInGame()) return; val x = hudX; val y = hudY; val direction = PlayerUtils.getFacingDirection(); val yaw = yawText(); graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); if (background.isEnabled()) AquaUI.hudTwoLine(graphics, mc.font, direction, yaw, x, y, -1, -3025448, bgOpacity.getIntValue()) else { graphics.text(mc.font, AquaFonts.hud(direction), x, y, -1, true); graphics.text(mc.font, AquaFonts.hud(yaw), x, y + 10, -3025448, true) }; graphics.pose().popMatrix() }
    override fun getHudWidth() = maxOf(mc.font.width(AquaFonts.hud(PlayerUtils.getFacingDirection())), mc.font.width(AquaFonts.hud(yawText()))) + 10
    override fun getHudHeight() = 28
}