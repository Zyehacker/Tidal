package com.aqua.hud.modules.impl.render

import com.aqua.hud.badge.BadgeService
import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting

open class BadgesModule : Module("Badges", "Show player badges above heads and in the TAB list", Category.PLAYER) {
    private val showAboveHead = register(LockedBooleanSetting("show_above_head", "Show badge chips above players' heads"))
    private val showInTabList = register(LockedBooleanSetting("show_in_tab_list", "Show badge indicators in the TAB player list"))
    private val showOwn = register(LockedBooleanSetting("show_own", "Show your own badges above your head"))
    private val seeOwnNametag = register(BooleanSetting("see_own_nametag", "Preview your full nametag in third-person view", false))

    init {
        instance = this
    }

    override fun setEnabled(value: Boolean) { super.setEnabled(value) }
    override fun isLocked() = false
    fun isShowAboveHead() = showAboveHead.isEnabled()
    fun isShowOwn() = showOwn.isEnabled()
    override fun onEnable() { BadgeService.getInstance().refreshNow() }

    private class LockedBooleanSetting(name: String, description: String) : BooleanSetting(name, description, true) {
        override fun setValue(nextValue: Boolean) = super.setValue(true)
    }

    companion object {
        private var instance: BadgesModule? = null
        @JvmStatic fun shouldShowAboveHead() = instance?.isEnabled() == true && instance!!.showAboveHead.isEnabled()
        @JvmStatic fun shouldShowInTabList() = instance?.isEnabled() == true && instance!!.showInTabList.isEnabled()
        @JvmStatic fun shouldShowOwnNametag() = instance?.seeOwnNametag?.isEnabled() == true
    }
}