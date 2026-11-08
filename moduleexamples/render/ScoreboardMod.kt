package com.aqua.hud.modules.impl.render

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.ColorSetting
import com.aqua.hud.modules.settings.NumberSetting

class ScoreboardMod : Module("ScoreboardMod", "Customize the scoreboard appearance", Category.RENDER) {
    private val hideScoreboard = register(BooleanSetting("hide_scoreboard", "Hide scoreboard completely", false))
    private val hideNumbers = register(BooleanSetting("hide_numbers", "Hide score numbers", true))
    private val customBackground = register(BooleanSetting("custom_background", "Use custom background color", false))
    private val backgroundColor = register(ColorSetting("background_color", "Background color", 0, 0, 0))
    private val backgroundOpacity = register(NumberSetting("background_opacity", "Background opacity", 64.0, 0.0, 255.0, 1.0))
    init { hideNumbers.visibleWhen { !hideScoreboard.isEnabled() }; customBackground.visibleWhen { !hideScoreboard.isEnabled() }; backgroundColor.visibleWhen { customBackground.isEnabled() && !hideScoreboard.isEnabled() }; backgroundOpacity.visibleWhen { customBackground.isEnabled() && !hideScoreboard.isEnabled() } }
    fun shouldHideScoreboard() = hideScoreboard.isEnabled()
    fun shouldHideNumbers() = hideNumbers.isEnabled()
    fun hasCustomBackground() = customBackground.isEnabled()
    fun getBackgroundColor() = backgroundOpacity.getIntValue() shl 24 or (backgroundColor.getRGB() and 0xFFFFFF)
}