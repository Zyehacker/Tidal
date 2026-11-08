package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaFonts
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.ColorSetting
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.util.Locale

class BiomeHUD : Module("Biome", "Shows the current biome", Category.HUD) {
    private val showLabel = register(BooleanSetting("show_label", "Show 'Biome:' label", true))
    private val formatName = register(BooleanSetting("format_name", "Format biome name", true))
    private val background = register(BooleanSetting("background", "Show background", true))
    private val color = register(ColorSetting("color", "Text color", 100, 200, 100))
    private fun biome(): String {
        val player = mc.player ?: return "unknown"
        val level = mc.level ?: return "unknown"
        val key = level.getBiome(player.blockPosition()).unwrapKey()
        val raw = key?.map { it.identifier().path }?.orElse("unknown") ?: "unknown"
        return if (formatName.isEnabled()) raw.split('_').filter(String::isNotEmpty).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() }.lowercase(Locale.ROOT).replaceFirstChar { c -> c.uppercase() } } else raw
    }
    private fun display() = if (showLabel.isEnabled()) "Biome: ${biome()}" else biome()
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { if (!isInGame()) return; val x = hudX; val y = hudY; val text = display(); val argb = color.getRGB() or Int.MIN_VALUE; graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); if (background.isEnabled()) AquaUI.hudText(graphics, mc.font, text, x, y, argb, true) else graphics.text(mc.font, AquaFonts.hud(text), x, y, argb, true); graphics.pose().popMatrix() }
    override fun getHudWidth() = if (mc.level == null) 80 else mc.font.width(AquaFonts.hud(display())) + 10
    override fun getHudHeight() = 17
}