package cc.tidal.tidal.modules.hud

import net.minecraft.client.Minecraft
import cc.tidal.tidal.api.config.v1.annotations.Slider
import cc.tidal.tidal.api.hud.v1.TextHud

/**
 * Displays the local player's current server latency.
 *
 * The HUD deliberately uses TextHud so positioning, scaling, background styling, editor
 * integration, and persistence remain owned by Tidal.
 */
class PingDisplayHud : TextHud(
    "tidal-ping-display",
    "Ping Display",
    Category.INFO,
    "Ping: ",
    "ms",
) {
    @Slider(
        title = "Warning latency",
        description = "Latency at which the HUD changes from good to warning.",
        min = 1f,
        max = 500f,
        step = 5f,
    )
    var warningPing: Int = 50

    @Slider(
        title = "Critical latency",
        description = "Latency at which the HUD changes from warning to critical.",
        min = 5f,
        max = 1000f,
        step = 5f,
    )
    var criticalPing: Int = 100

    @Slider(
        title = "Background opacity",
        description = "Opacity of the HUD background.",
        min = 0f,
        max = 255f,
        step = 5f,
    )
    var backgroundOpacity: Int = 80

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
        val currentPing = ping().coerceAtLeast(0)
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

        super.update()
        return oldText != pingText || oldColor != textColor || oldBackground != bgColor
    }

    override fun updateFrequency(): Long = 250_000_000L

    override fun defaultPosition(): Pair<Float, Float> = 10f to 10f

    override fun showByDefault(): Boolean = true

    override fun deletable(): Boolean = true

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
