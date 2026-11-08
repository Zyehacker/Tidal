package com.aqua.hud.modules.impl.render

import com.aqua.hud.appearance.PlayerAppearanceService
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting

open class AquaSkinsModule : Module("AquaSkins", "Aqua Skins & Capes", "Show Aqua skins and capes on Aqua players", Category.PLAYER) {
    private val enableSkin = register(LockedBooleanSetting("enable_skin", "Enable Aqua custom skins"))
    private val enableCape = register(LockedBooleanSetting("enable_cape", "Enable Aqua custom capes"))

    init {
        instance = this
    }

    override fun setEnabled(value: Boolean) { super.setEnabled(value) }
    override fun isLocked() = false
    override fun onEnable() { PlayerAppearanceService.getInstance().setEnabled(true) }
    override fun onDisable() { PlayerAppearanceService.getInstance().setEnabled(false) }

    private class LockedBooleanSetting(name: String, description: String) : BooleanSetting(name, description, true) {
        override fun setValue(nextValue: Boolean) = super.setValue(true)
    }

    companion object {
        private var instance: AquaSkinsModule? = null
        @JvmStatic fun isActive() = instance?.isEnabled() == true
        @JvmStatic fun isSkinEnabled() = instance?.let { it.isEnabled() && it.enableSkin.isEnabled() } == true
        @JvmStatic fun isCapeEnabled() = instance?.let { it.isEnabled() && it.enableCape.isEnabled() } == true
    }
}