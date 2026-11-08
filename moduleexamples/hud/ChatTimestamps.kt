package com.aqua.hud.modules.impl.hud

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.ColorSetting
import com.aqua.hud.modules.settings.ModeSetting
import java.text.SimpleDateFormat
import java.util.Date

class ChatTimestamps : Module("ChatTimestamps", "Add timestamps to chat messages", Category.HUD) {
    private val format = register(ModeSetting("format", "Timestamp format", "HH:mm", arrayOf("HH:mm", "HH:mm:ss", "hh:mm a", "hh:mm:ss a", "[HH:mm]", "[hh:mm a]")))
    private val brackets = register(BooleanSetting("brackets", "Wrap timestamp in brackets", true))
    private val useCustomColor = register(BooleanSetting("custom_color", "Use custom timestamp color", true))
    private val timestampColor = register(ColorSetting("color", "Timestamp text color", 170, 170, 170))
    private val seconds = register(BooleanSetting("show_seconds", "Show seconds in timestamp", false))
    init { timestampColor.visibleWhen { useCustomColor.isEnabled() }; instance = this }
    fun getTimestamp(): String { var pattern = format.getValue(); if (seconds.isEnabled() && !pattern.contains(":ss")) pattern = pattern.replace(":mm", ":mm:ss"); var value = SimpleDateFormat(pattern).format(Date()); if (brackets.isEnabled() && !pattern.startsWith("[")) value = "[$value]"; return "$value " }
    fun getFormattedTimestamp() = getTimestamp()
    fun getTimestampRGB() = timestampColor.getRGB() or Int.MIN_VALUE
    fun useCustomColorRendering() = useCustomColor.isEnabled()
    override fun isHudVisible() = false
    companion object { private var instance: ChatTimestamps? = null; @JvmStatic fun getInstance() = instance }
}