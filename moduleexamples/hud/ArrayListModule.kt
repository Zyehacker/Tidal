package com.aqua.hud.modules.impl.hud

import com.aqua.hud.core.ModuleManager
import com.aqua.hud.gui.AquaFonts
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.ColorSetting
import com.aqua.hud.modules.settings.ModeSetting
import net.minecraft.client.gui.GuiGraphicsExtractor

class ArrayListModule : Module("ArrayList", "List of active modules", Category.HUD) {
    private val sortMode = register(ModeSetting("sort_mode", "Sort order", "length", arrayOf("length", "alphabetical")))
    private val textColor = register(ColorSetting("text_color", "Module text color", 255, 255, 255))
    private val background = register(BooleanSetting("background", "Show background", true))
    private fun modules(): List<Module> {
        val manager = ModuleManager.getInstance() ?: return emptyList()
        return manager.enabledModules.filter { it !== this }
            .sortedWith(if (sortMode.`is`("length")) compareByDescending { mc.font.width(AquaFonts.moduleName(it.displayName)) } else compareBy { it.displayName })
    }
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { if (!isInGame()) return; val list = modules(); val x = hudX; val y = hudY; graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); list.forEachIndexed { index, module -> val name = AquaFonts.moduleName(module.displayName); if (background.isEnabled()) AquaUI.hudPanel(graphics, x - 5, y + index * 13 - 3, mc.font.width(name) + 10, 15); graphics.text(mc.font, name, x, y + index * 13, textColor.getRGB() or Int.MIN_VALUE, false) }; graphics.pose().popMatrix() }
    override fun getHudWidth() = modules().maxOfOrNull { mc.font.width(AquaFonts.moduleName(it.displayName)) }?.plus(10) ?: 10
    override fun getHudHeight() = modules().size * 13 + 2
}