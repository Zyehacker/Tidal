package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaFonts
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import net.minecraft.client.gui.GuiGraphicsExtractor

class PackDisplay : Module("PackDisplay", "Shows active resource packs", Category.HUD) {
    private val background = register(BooleanSetting("background", "Show background", true))
    private fun packs() = mc.resourcePackRepository.selectedPacks.map { it.title.string }.filter { it != "Default" && it != "Fabric Mods" }
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { if (!isInGame()) return; val names = packs(); if (names.isEmpty()) return; val x = hudX; val y = hudY; val width = names.maxOf { mc.font.width(AquaFonts.hud(it)) }; graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); if (background.isEnabled()) AquaUI.hudPanel(graphics, x - 5, y - 4, width + 10, names.size * 12 + 6); names.forEachIndexed { index, name -> graphics.text(mc.font, AquaFonts.hud(name), x, y + index * 12, -1, true) }; graphics.pose().popMatrix() }
    override fun getHudWidth() = packs().maxOfOrNull { mc.font.width(AquaFonts.hud(it)) }?.plus(10) ?: 10
    override fun getHudHeight() = packs().size * 12 + 6
}