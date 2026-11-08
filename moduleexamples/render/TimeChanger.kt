package com.aqua.hud.modules.impl.render

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.ModeSetting
import com.aqua.hud.modules.settings.NumberSetting

class TimeChanger : Module("TimeChanger", "Changes the visual time of day", Category.RENDER) {
    private val mode = register(ModeSetting("mode", "Time preset", "custom", arrayOf("day", "night", "sunset", "custom")))
    private val customTime = register(NumberSetting("custom_time", "Custom time value", 6000.0, 0.0, 24000.0, 100.0))
    init { customTime.visibleWhen { mode.`is`("custom") } }
    fun getCustomTime() = when { mode.`is`("day") -> 6000L; mode.`is`("night") -> 18000L; mode.`is`("sunset") -> 12000L; else -> customTime.getValue().toLong() }
}