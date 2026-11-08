package com.aqua.hud.modules.impl.render

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.NumberSetting

class FullbrightModule : Module("Fullbright", "See in the dark like it's daytime", Category.RENDER) {
    private val strength = register(NumberSetting("strength", "Brightness level (10 = maximum)", 10.0, 1.0, 10.0, 1.0))
    init { setKeyBinding(66) }
    fun getStrength() = strength.getFloatValue()
}