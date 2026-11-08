package com.aqua.hud.modules.impl.utility

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.KeybindSetting
import com.aqua.hud.modules.settings.TextSetting
import com.mojang.blaze3d.platform.InputConstants

class AutoText : Module("AutoText", "Send predefined messages quickly with keybinds (Numpad 1-3 by default)", Category.UTILITY) {
    private val texts = arrayOf(register(TextSetting("message_1", "First quick message", "GG!")), register(TextSetting("message_2", "Second quick message", "/home")), register(TextSetting("message_3", "Third quick message", "Nice shot!")))
    private val keys = arrayOf(register(KeybindSetting("key_1", "Key for message 1", 321)), register(KeybindSetting("key_2", "Key for message 2", 322)), register(KeybindSetting("key_3", "Key for message 3", 323)))
    private var wasPressed = BooleanArray(3)
    override fun onEnable() { wasPressed = BooleanArray(3) }
    override fun onTick() { if (!isInGame() || mc.gui.screen() != null) return; keys.forEachIndexed { index, key -> val code = key.getKey(); val pressed = code != 0 && InputConstants.isKeyDown(mc.window, code); if (pressed && !wasPressed[index]) sendText(texts[index].getValue()); wasPressed[index] = pressed } }
    private fun sendText(text: String) { if (text.isNotEmpty()) mc.connection?.let { if (text.startsWith('/')) it.sendCommand(text.substring(1)) else it.sendChat(text) } }
}