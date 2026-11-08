package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaFonts
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.NumberSetting
import com.aqua.hud.render.Theme
import net.minecraft.client.gui.GuiGraphicsExtractor

class PingDisplay : Module("PingDisplay", "Shows your server latency in milliseconds from the current player-list connection and colors it by configurable thresholds.", Category.HUD) {
    private val background = register(BooleanSetting("background", "Show background", true))
    private val bgOpacity = register(NumberSetting("opacity", "Background opacity", 80.0, 0.0, 255.0, 5.0))
    private val warningPing = register(NumberSetting("warning_ping", "Warning latency", 50.0, 1.0, 500.0, 5.0))
    private val criticalPing = register(NumberSetting("critical_ping", "Critical latency", 100.0, 5.0, 1000.0, 5.0))
    init { bgOpacity.visibleWhen { background.isEnabled() } }
    private fun ping(): Int = mc.player?.uuid?.let { uuid ->
        mc.connection?.getPlayerInfo(uuid)?.latency
    } ?: 0
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { if (!isInGame()) return; val x = hudX; val y = hudY; val ping = ping(); val text = "Ping: ${ping}ms"; val color = if (ping < warningPing.getIntValue()) Theme.STATE_GOOD else if (ping < criticalPing.getIntValue()) Theme.STATE_WARNING else Theme.STATE_CRITICAL; graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); if (background.isEnabled()) AquaUI.hudText(graphics, mc.font, text, x, y, color, true, bgOpacity.getIntValue()) else graphics.text(mc.font, AquaFonts.hud(text), x, y, color, true); graphics.pose().popMatrix() }
    override fun getHudWidth() = mc.font.width(AquaFonts.hud("Ping: ${ping()}ms")) + 10
    override fun getHudHeight() = 17
}