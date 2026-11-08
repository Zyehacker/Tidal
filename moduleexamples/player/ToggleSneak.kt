package com.aqua.hud.modules.impl.player

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.mojang.blaze3d.platform.InputConstants

class ToggleSneak : Module("ToggleSneak", "Press sneak once to toggle sneaking on/off - great for building!", Category.PLAYER) {
    private var sneakToggled = false
    private var wasKeyPressed = false
    override fun onEnable() { sneakToggled = false; wasKeyPressed = false }
    override fun onTick() {
        if (!isInGame()) return
        val pressed = InputConstants.isKeyDown(mc.window, mc.options.keyShift.defaultKey.value)
        if (pressed && !wasKeyPressed) sneakToggled = !sneakToggled
        wasKeyPressed = pressed
        if (sneakToggled) mc.options.keyShift.isDown = true
    }
    override fun onDisable() { sneakToggled = false; mc.options.keyShift.isDown = false }
    fun isSneakToggled() = sneakToggled
}