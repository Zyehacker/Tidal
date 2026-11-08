package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaFonts
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.utils.PlayerUtils
import net.minecraft.client.gui.GuiGraphicsExtractor

class SpeedHUD : Module("SpeedHUD", "Shows current horizontal movement speed with configurable units and a lightweight HUD background.", Category.HUD) {
    private val background = register(BooleanSetting("background", "Show background", true))
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { if (!isInGame()) return; val x = hudX; val y = hudY; val text = "Speed: %.2f m/s".format(PlayerUtils.getSpeed()); graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); if (background.isEnabled()) AquaUI.hudText(graphics, mc.font, text, x, y, -1, true) else graphics.text(mc.font, AquaFonts.hud(text), x, y, -1, true); graphics.pose().popMatrix() }
    override fun getHudWidth() = mc.font.width(AquaFonts.hud("Speed: %.2f m/s".format(PlayerUtils.getSpeed()))) + 10
    override fun getHudHeight() = 17
}