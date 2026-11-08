package com.aqua.hud.modules.impl.utility

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.NumberSetting
import com.aqua.hud.gui.DisplaySpace
import com.aqua.hud.ui.foundation.PolyNotificationHost
import net.minecraft.client.gui.GuiGraphicsExtractor

class Notifications : Module("Notifications", "Shows popup notifications for module toggles", Category.UTILITY) {
    private val moduleToggle = register(BooleanSetting("module_toggle", "Show notification when modules are toggled", true))
    private val showEnabled = register(BooleanSetting("show_enabled", "Show when modules are enabled", true))
    private val showDisabled = register(BooleanSetting("show_disabled", "Show when modules are disabled", true))
    private val duration = register(NumberSetting("duration", "How long notifications stay on screen", 3.0, 1.0, 10.0, 0.5))

    fun onModuleToggle(moduleName: String, enabled: Boolean) {
        if (!isEnabled() || !moduleToggle.isEnabled()) return
        if (enabled && !showEnabled.isEnabled() || !enabled && !showDisabled.isEnabled()) return
        PolyNotificationHost.push(if (enabled) "Module Enabled" else "Module Disabled", moduleName, if (enabled) PolyNotificationHost.Type.INFO else PolyNotificationHost.Type.WARNING, duration.getValue().toLong() * 1000L)
    }

    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) {
        if (isInGame()) PolyNotificationHost.render(graphics, DisplaySpace.width().toFloat(), DisplaySpace.height().toFloat())
    }
}