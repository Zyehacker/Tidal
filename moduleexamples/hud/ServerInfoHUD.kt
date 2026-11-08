package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaFonts
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.NumberSetting
import net.minecraft.client.gui.GuiGraphicsExtractor

class ServerInfoHUD : Module("ServerInfo", "Shows server information", Category.HUD) {
    private val background = register(BooleanSetting("background", "Show background", true))
    private val bgOpacity = register(NumberSetting("opacity", "Background opacity", 80.0, 0.0, 255.0, 5.0))
    init { bgOpacity.visibleWhen { background.isEnabled() } }
    private fun server(): Pair<String, String>? = if (mc.hasSingleplayerServer()) "Singleplayer" to "Local" else mc.currentServer?.let { it.name to it.ip }
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { if (!isInGame()) return; val (name, address) = server() ?: return; val x = hudX; val y = hudY; graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); if (background.isEnabled()) AquaUI.hudTwoLine(graphics, mc.font, name, address, x, y, -1, -3025448, bgOpacity.getIntValue()) else { graphics.text(mc.font, AquaFonts.hud(name), x, y, -1, true); graphics.text(mc.font, AquaFonts.hud(address), x, y + 10, -3025448, true) }; graphics.pose().popMatrix() }
    override fun getHudWidth() = server()?.let { maxOf(mc.font.width(AquaFonts.hud(it.first)), mc.font.width(AquaFonts.hud(it.second))) + 10 } ?: 80
    override fun getHudHeight() = 28
}