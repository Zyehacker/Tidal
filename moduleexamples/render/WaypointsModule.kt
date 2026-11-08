package com.aqua.hud.modules.impl.render

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import com.aqua.hud.modules.settings.BooleanSetting
import com.aqua.hud.modules.settings.NumberSetting
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.nio.file.Files
import java.util.Locale
import java.util.UUID
import kotlin.math.sqrt

class WaypointsModule : Module("Waypoints", "Create persistent, colored navigation markers for every dimension", Category.RENDER) {
    private val showDistance = register(BooleanSetting("show_distance", "Show the distance to each waypoint", true))
    private val showCoordinates = register(BooleanSetting("show_coordinates", "Show waypoint coordinates in the navigation marker", false))
    private val maxVisible = register(NumberSetting("max_visible", "Maximum number of navigation markers", 5.0, 1.0, 12.0, 1.0))
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configPath = FabricLoader.getInstance().configDir.resolve("aqua-hud").resolve("waypoints.json")
    private val waypoints = mutableListOf<Waypoint>()
    init { setKeyBinding(78); load() }
    fun getWaypoints(): List<Waypoint> = waypoints.toList()
    fun upsert(waypoint: Waypoint) { val index = waypoints.indexOfFirst { it.id == waypoint.id }; if (index >= 0) { if (waypoint.world == null) waypoint.world = waypoints[index].world; waypoints[index] = waypoint } else waypoints += waypoint; save() }
    fun remove(id: String) { waypoints.removeIf { it.id == id }; save() }
    fun setWaypointEnabled(id: String, enabled: Boolean) { waypoints.firstOrNull { it.id == id }?.let { it.enabled = enabled; save() } }
    fun create(name: String, x: Double, y: Double, z: Double, dimension: String, color: Int) = Waypoint(UUID.randomUUID().toString(), cleanName(name), x, y, z, normalizeDimension(dimension), currentWorld(), color or Int.MIN_VALUE, true)
    fun currentDimension() = mc.level?.dimension()?.identifier()?.path ?: "overworld"
    fun currentWorld() = mc.currentServer?.let { "server:${it.ip.lowercase(Locale.ROOT)}" } ?: mc.singleplayerServer?.let { "singleplayer:${it.worldData.levelName.lowercase(Locale.ROOT)}" } ?: "unknown"
    private fun load() { if (!Files.exists(configPath)) return; runCatching { gson.fromJson<List<Waypoint>>(Files.readString(configPath), object : TypeToken<List<Waypoint>>() {}.type)?.filterTo(waypoints) { it.id != null && it.name != null } } }
    fun save() { runCatching { Files.createDirectories(configPath.parent); Files.writeString(configPath, gson.toJson(waypoints)) }.onFailure { sendMessage("Could not save waypoints.") } }
    fun getVisibleWorldWaypoints(): List<Waypoint> { val player = mc.player ?: return emptyList(); val dimension = currentDimension(); return waypoints.filter { it.enabled && (it.world == null || it.world == currentWorld()) && normalizeDimension(it.dimension) == dimension }.sortedBy { distanceSquared(it, player.x, player.y, player.z) }.take(maxVisible.getIntValue()) }
    fun getWorldLabel(waypoint: Waypoint): String = buildString { append(waypoint.name); if (showDistance.isEnabled()) append("  ${distanceToPlayer(waypoint).toInt()}m"); if (showCoordinates.isEnabled()) append("  [%.0f, %.0f, %.0f]".format(waypoint.x, waypoint.y, waypoint.z)) }
    fun distanceToPlayer(waypoint: Waypoint) = mc.player?.let { sqrt(distanceSquared(waypoint, it.x, it.y, it.z)) } ?: 0.0
    override fun onRender(graphics: GuiGraphicsExtractor, tickDelta: Float) {}
    override fun isHudVisible() = false
    class Waypoint(var id: String, var name: String, var x: Double, var y: Double, var z: Double, var dimension: String, var world: String?, var color: Int, var enabled: Boolean) {
        constructor(id: String, name: String, x: Double, y: Double, z: Double, dimension: String, color: Int, enabled: Boolean) : this(id, name, x, y, z, dimension, null, color, enabled)
    }
    companion object {
        private fun distanceSquared(waypoint: Waypoint, x: Double, y: Double, z: Double) = (waypoint.x - x) * (waypoint.x - x) + (waypoint.y - y) * (waypoint.y - y) + (waypoint.z - z) * (waypoint.z - z)
        @JvmStatic fun cleanName(name: String?) = name.orEmpty().trim().replace(Regex("\\s+"), " ").take(40)
        @JvmStatic fun normalizeDimension(dimension: String?) = dimension.orEmpty().trim().lowercase(Locale.ROOT).substringAfterLast(':').let { if (it == "dimension") "overworld" else it }
    }
}