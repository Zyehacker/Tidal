package com.aqua.hud.modules.impl.render

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.ModeSetting
import com.aqua.hud.modules.settings.NumberSetting

class MotionBlurModule : Module("Motion Blur", "Blurs the scene while you look around", Category.RENDER) {
    private val strength = register(NumberSetting("strength", "Camera blur strength", 0.1, 0.1, 10.0, 0.1))
    private val refreshRateScaling = register(BooleanSetting("refresh_rate_scaling", "Scale blur based on FPS vs refresh rate", true))
    private val excludeEntities = register(ModeSetting("exclude_entities", "Exclude entities from blur", "Third Person", arrayOf("Always", "Third Person", "Never")))
    private val blurAlgorithm = register(ModeSetting("blur_algorithm", "Blur direction algorithm", "Backwards", arrayOf("Backwards", "Centered")))
    fun getStrength() = strength.getFloatValue()
    fun isRefreshRateScaling() = refreshRateScaling.isEnabled()
    fun getExcludeEntities() = excludeEntities.getValue()
    fun getBlurAlgorithmOrdinal() = if (blurAlgorithm.`is`("Backwards")) 0 else 1
}