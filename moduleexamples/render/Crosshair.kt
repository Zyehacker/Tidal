package com.aqua.hud.modules.impl.render

import com.aqua.hud.gui.DisplaySpace
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.ColorSetting
import com.aqua.hud.modules.settings.ModeSetting
import com.aqua.hud.modules.settings.NumberSetting
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class Crosshair : Module("Crosshair", "Custom crosshair replacement", Category.RENDER) {
    private val style = register(ModeSetting("style", "Crosshair style", "cross", arrayOf("cross", "dot", "circle", "square")))
    private val size = register(NumberSetting("size", "Crosshair size", 6.0, 1.0, 30.0, 1.0))
    private val thickness = register(NumberSetting("thickness", "Line thickness", 1.0, 0.5, 10.0, 0.5))
    private val gap = register(NumberSetting("gap", "Center gap", 2.0, 0.0, 15.0, 1.0))
    private val color = register(ColorSetting("color", "Crosshair color", 255, 255, 255))

    init {
        gap.visibleWhen { style.`is`("cross") }
        thickness.visibleWhen { !style.`is`("circle") }
    }

    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) {
        if (!isInGame() || !mc.options.cameraType.isFirstPerson) return
        val centerX = DisplaySpace.width() / 2; val centerY = DisplaySpace.height() / 2
        val argb = color.getRGB() or Int.MIN_VALUE; val radius = size.getIntValue(); val line = thickness.getFloatValue(); val centerGap = gap.getIntValue()
        when (style.getValue()) {
            "cross" -> { val half = max(1, ceil(line / 2).toInt()); val offset = if (line < 1f) 0 else half - 1; graphics.fill(centerX - offset, centerY - radius - centerGap, centerX + half, centerY - centerGap, argb); graphics.fill(centerX - offset, centerY + centerGap + 1, centerX + half, centerY + radius + centerGap + 1, argb); graphics.fill(centerX - radius - centerGap, centerY - offset, centerX - centerGap, centerY + half, argb); graphics.fill(centerX + centerGap + 1, centerY - offset, centerX + radius + centerGap + 1, centerY + half, argb) }
            "dot" -> { val dot = max(1, line.toInt()); val half = dot / 2; graphics.fill(centerX - half, centerY - half, centerX + dot - half, centerY + dot - half, argb) }
            "circle" -> drawCircle(graphics, centerX, centerY, radius, argb)
            "square" -> { val thick = max(1, line.toInt()); graphics.fill(centerX - radius, centerY - radius, centerX + radius, centerY - radius + thick, argb); graphics.fill(centerX - radius, centerY + radius - thick, centerX + radius, centerY + radius, argb); graphics.fill(centerX - radius, centerY - radius, centerX - radius + thick, centerY + radius, argb); graphics.fill(centerX + radius - thick, centerY - radius, centerX + radius, centerY + radius, argb) }
        }
    }

    private fun drawCircle(graphics: GuiGraphicsExtractor, centerX: Int, centerY: Int, radius: Int, color: Int) {
        val segments = max(16, radius * 4)
        repeat(segments) { index -> val angle = index * Math.PI * 2 / segments; graphics.fill(centerX + (cos(angle) * radius).toInt(), centerY + (sin(angle) * radius).toInt(), centerX + (cos(angle) * radius).toInt() + 1, centerY + (sin(angle) * radius).toInt() + 1, color) }
    }
}