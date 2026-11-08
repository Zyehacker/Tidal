package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaGlass
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.ColorSetting
import com.aqua.hud.modules.settings.NumberSetting
import com.aqua.hud.render.Theme
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.lwjgl.glfw.GLFW

class Keystrokes : Module("Keystrokes", "Shows movement, mouse, and jump inputs in real time with adjustable key size, spacing, colors, and optional input groups.", Category.HUD) {
    private val keySize = register(NumberSetting("key_size", "Size of each key", 22.0, 16.0, 40.0, 2.0)); private val spacing = register(NumberSetting("spacing", "Gap between keys", 2.0, 0.0, 10.0, 1.0)); private val showMouse = register(BooleanSetting("show_mouse", "Show mouse buttons", true)); private val showSpace = register(BooleanSetting("show_space", "Show spacebar", true)); private val background = register(BooleanSetting("background", "Show key backgrounds", true)); private val backgroundOpacity = register(NumberSetting("background_opacity", "Key background opacity", 255.0, 0.0, 255.0, 5.0)); private val pressedColor = register(ColorSetting("pressed_color", "Pressed key color", Theme.PRIMARY)); private val normalColor = register(ColorSetting("normal_color", "Normal key color", Theme.SURFACE))
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { if (!isInGame()) return; val x = hudX; val y = hudY; val size = keySize.getIntValue(); val gap = spacing.getIntValue(); graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); drawKey(graphics, x + size + gap, y, size, "W", mc.options.keyUp); drawKey(graphics, x, y + size + gap, size, "A", mc.options.keyLeft); drawKey(graphics, x + size + gap, y + size + gap, size, "S", mc.options.keyDown); drawKey(graphics, x + (size + gap) * 2, y + size + gap, size, "D", mc.options.keyRight); var currentY = y + (size + gap) * 2; if (showMouse.isEnabled()) { val mouseWidth = (size * 3 + gap * 2 - gap) / 2; val handle = mc.window.handle(); drawKeyRect(graphics, x, currentY, mouseWidth, size, "LMB", GLFW.glfwGetMouseButton(handle, 0) == 1); drawKeyRect(graphics, x + mouseWidth + gap, currentY, mouseWidth, size, "RMB", GLFW.glfwGetMouseButton(handle, 1) == 1); currentY += size + gap }; if (showSpace.isEnabled()) drawKeyRect(graphics, x, currentY, size * 3 + gap * 2, size / 2, "—", mc.options.keyJump.isDown); graphics.pose().popMatrix() }
    private fun drawKey(graphics: GuiGraphicsExtractor, x: Int, y: Int, size: Int, text: String, key: KeyMapping) = drawKeyRect(graphics, x, y, size, size, text, key.isDown)
    private fun drawKeyRect(graphics: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, text: String, pressed: Boolean) { val color = if (pressed) pressedColor.getRGB() else normalColor.getRGB(); if (background.isEnabled()) if (pressed) AquaGlass.glassButtonHover(graphics, x, y, width, height, 4) else AquaGlass.glassButton(graphics, x, y, width, height, 4); AquaUI.outline(graphics, x, y, width, height, if (pressed) Theme.lighten(color, 0.2f) else Theme.BORDER); graphics.text(mc.font, text, x + (width - mc.font.width(text)) / 2, y + (height - 8) / 2, if (pressed) Theme.TEXT else Theme.TEXT_SECONDARY, true) }
    override fun getHudWidth() = keySize.getIntValue() * 3 + spacing.getIntValue() * 2
    override fun getHudHeight() = (keySize.getIntValue() + spacing.getIntValue()) * 2 + (if (showMouse.isEnabled()) keySize.getIntValue() + spacing.getIntValue() else 0) + (if (showSpace.isEnabled()) keySize.getIntValue() / 2 else 0)
}