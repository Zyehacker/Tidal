package com.aqua.hud.modules.impl.hud

import com.aqua.hud.gui.AquaFonts
import com.aqua.hud.gui.AquaUI
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.lwjgl.glfw.GLFW
import java.util.ArrayDeque

class CPSCounter : Module("CPSCounter", "Measures recent left and right mouse clicks as clicks per second over a rolling interval for combat feedback.", Category.HUD) {
    private val background = register(BooleanSetting("background", "Show background", true)); private val leftClicks = ArrayDeque<Long>(); private val rightClicks = ArrayDeque<Long>(); private var leftPressed = false; private var rightPressed = false
    override fun onTick() { if (!isInGame()) return; val now = System.currentTimeMillis(); while (leftClicks.firstOrNull()?.let { now - it > 1000 } == true) leftClicks.removeFirst(); while (rightClicks.firstOrNull()?.let { now - it > 1000 } == true) rightClicks.removeFirst(); val handle = mc.window.handle(); val left = GLFW.glfwGetMouseButton(handle, 0) == 1; val right = GLFW.glfwGetMouseButton(handle, 1) == 1; if (left && !leftPressed) leftClicks.addLast(now); if (right && !rightPressed) rightClicks.addLast(now); leftPressed = left; rightPressed = right }
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) { if (!isInGame()) return; val x = hudX; val y = hudY; val text = "CPS: ${leftClicks.size} | ${rightClicks.size}"; graphics.pose().pushMatrix(); graphics.pose().translate(x.toFloat(), y.toFloat()); graphics.pose().scale(hudScale, hudScale); graphics.pose().translate(-x.toFloat(), -y.toFloat()); if (background.isEnabled()) AquaUI.hudText(graphics, mc.font, text, x, y, -1, true) else graphics.text(mc.font, AquaFonts.hud(text), x, y, -1, true); graphics.pose().popMatrix() }
    override fun getHudWidth() = mc.font.width(AquaFonts.hud("CPS: ${leftClicks.size} | ${rightClicks.size}")) + 10
    override fun getHudHeight() = 17
}