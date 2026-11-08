package com.aqua.hud.modules.impl.render

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting

open class NametagIconModule : Module("NametagIcon", "Aqua Nametag", "Show Aqua logo next to Aqua players", Category.PLAYER) {
    private val showAboveHead = register(LockedBooleanSetting("show_above_head", "Show icon above players' heads"))
    private val showInTabList = register(LockedBooleanSetting("show_in_tab_list", "Show icon in the TAB player list"))
    init { instance = this }
    override fun setEnabled(value: Boolean) { super.setEnabled(value) }
    override fun isLocked() = false
    private class LockedBooleanSetting(name: String, description: String) : BooleanSetting(name, description, true) { override fun setValue(nextValue: Boolean) = super.setValue(true) }
    companion object { private var instance: NametagIconModule? = null; @JvmStatic fun shouldShowAboveHead() = instance?.isEnabled() == true && instance!!.showAboveHead.isEnabled(); @JvmStatic fun shouldShowInTabList() = instance?.isEnabled() == true && instance!!.showInTabList.isEnabled() }
}