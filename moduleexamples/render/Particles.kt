package com.aqua.hud.modules.impl.render

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.ColorSetting
import com.aqua.hud.modules.settings.NumberSetting

class Particles : Module("Particles", "Control particle effects and weather", Category.RENDER) {
    private val multiplier = register(NumberSetting("multiplier", "Particle spawn multiplier (1 = normal)", 1.0, 0.0, 5.0, 0.5)); private val weather = register(BooleanSetting("weather", "Show rain/snow weather effects", true)); private val criticals = register(BooleanSetting("criticals", "Show critical hit particles", true)); private val enchanted = register(BooleanSetting("enchanted", "Show enchanted hit particles", true)); private val explosion = register(BooleanSetting("explosion", "Show explosion particles", true)); private val potion = register(BooleanSetting("potion", "Show potion/effect particles", true)); private val firework = register(BooleanSetting("firework", "Show firework particles", true)); private val totem = register(BooleanSetting("totem", "Show totem of undying particles", true)); private val smoke = register(BooleanSetting("smoke", "Show smoke particles", true)); private val flame = register(BooleanSetting("flame", "Show flame particles", true)); private val waterSplash = register(BooleanSetting("water_splash", "Show water splash particles", true)); private val useCustomColor = register(BooleanSetting("custom_crit_color", "Use custom color for crit particles", false)); private val critColor = register(ColorSetting("crit_color", "Custom crit particle color", 255, 215, 0))
    init { critColor.visibleWhen { useCustomColor.isEnabled() }; instance = this }
    fun getMultiplier() = multiplier.getFloatValue(); fun shouldShowWeather() = weather.isEnabled(); fun shouldShowCriticals() = criticals.isEnabled(); fun shouldShowEnchanted() = enchanted.isEnabled(); fun shouldShowExplosion() = explosion.isEnabled(); fun shouldShowPotion() = potion.isEnabled(); fun shouldShowFirework() = firework.isEnabled(); fun shouldShowTotem() = totem.isEnabled(); fun shouldShowSmoke() = smoke.isEnabled(); fun shouldShowFlame() = flame.isEnabled(); fun shouldShowWaterSplash() = waterSplash.isEnabled(); fun useCustomCritColor() = useCustomColor.isEnabled(); fun getCritColor() = critColor.getRGB() or Int.MIN_VALUE
    companion object { private var instance: Particles? = null; @JvmStatic fun getInstance() = instance }
}