package cc.tidal.tidal.test

import net.minecraft.client.Minecraft
import cc.tidal.tidal.api.config.v1.annotations.Slider
import cc.tidal.tidal.api.hud.v1.TextHud

class PingDisplayHud_Test : TextHud(
    "ping-display-hud",
    "Ping Display",
    Category.INFO,
    "Ping: ",
    "ms",
) {
    @Slider(title = "Warning latency", min = 1f, max = 500f, step = 5f)
    var warningPing: Int = 50

    @Slider(title = "Critical latency", min = 5f, max = 1000f, step = 5f)
    var criticalPing: Int = 100

    @Slider(title = "Background opacity", min = 0f, max = 255f, step = 5f)
    var backgroundOpacity: Int = 128

    private var pingText = "0"

    init {
        showBackground = true
        showShadow = true
    }

    private fun ping(): Int {
        val client = Minecraft.getInstance()
        val player = client.player ?: return 0
        return client.connection?.getPlayerInfo(player.uuid)?.latency ?: 0
    }

    override fun update(): Boolean {
        val currentPing = ping()
        val oldText = pingText
        val oldColor = textColor
        val oldBackground = bgColor

        pingText = currentPing.toString()
        textColor = when {
            currentPing < warningPing -> 0xFF55FF55.toInt()
            currentPing < criticalPing -> 0xFFFFFF55.toInt()
            else -> 0xFFFF5555.toInt()
        }
        bgColor = (backgroundOpacity.coerceIn(0, 255) shl 24) or 0x0010151B

        val changed = oldText != pingText || oldColor != textColor || oldBackground != bgColor
        super.update()
        return changed
    }

    override fun updateFrequency(): Long = 250_000_000L

    override fun defaultPosition(): Pair<Float, Float> = 10f to 10f

    override fun showByDefault(): Boolean = true

    override fun deletable(): Boolean = false

    override fun getText(): String = pingText

    override fun setup() {
        super.setup()
        if (isReal) {
            updateWhenChanged("warningPing")
            updateWhenChanged("criticalPing")
            updateWhenChanged("backgroundOpacity")
        }
        update()
    }
}