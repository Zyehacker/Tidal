package com.aqua.hud.modules.impl.utility

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.NumberSetting
import com.aqua.hud.modules.settings.TextSetting

class AutoGG : Module("AutoGG", "Automatically send GG when a game ends", Category.UTILITY) {
    private val message = register(TextSetting("message", "Message to send", "gg"))
    private val delay = register(NumberSetting("delay", "Delay before sending (seconds)", 1.0, 0.5, 5.0, 0.5))
    private val triggers = arrayOf("has won", "winner", "winners", "victory", "game over", "game ended", "1st place", "you win", "you lost", "you died", "top survivors", "won the game")
    private var sendDelayTicks = -1
    private var cooldownTicks = 0
    override fun onEnable() { sendDelayTicks = -1; cooldownTicks = 0 }
    fun onChatReceived(text: String) { if (cooldownTicks <= 0 && sendDelayTicks < 0 && triggers.any { text.lowercase().contains(it) }) sendDelayTicks = (delay.getValue() * 20).toInt() }
    override fun onTick() { if (!isInGame()) return; if (cooldownTicks > 0) cooldownTicks--; if (sendDelayTicks >= 0 && --sendDelayTicks < 0) sendGG() }
    private fun sendGG() { val text = message.getValue(); if (text.isNotEmpty()) { mc.connection?.let { if (text.startsWith('/')) it.sendCommand(text.substring(1)) else it.sendChat(text); cooldownTicks = 200 } } }
}