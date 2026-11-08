package com.aqua.hud.modules.impl.render

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module
import net.minecraft.client.CloudStatus
import net.minecraft.client.GraphicsPreset
import net.minecraft.client.Minecraft
import net.minecraft.server.level.ParticleStatus
import com.aqua.hud.ui.foundation.PolyNotificationHost

class FPSBoost : Module("FPSBoost", "Badlion-style low-impact graphics preset", Category.RENDER) {
    private var original: Snapshot? = null
    override fun onEnable() { if (mc.options == null) return; original = Snapshot.capture(mc); applyBoost(mc); toast("FPS Boost", "Low-impact settings enabled") }
    override fun onDisable() { original?.restore(mc); if (original != null) mc.options.save(); original = null; toast("FPS Boost", "Original settings restored") }
    override fun onTick() { if (!isEnabled() || mc.options == null) return; if (original == null) { original = Snapshot.capture(mc); applyBoost(mc); toast("FPS Boost", "Low-impact settings enabled") } else if (changed(mc)) applyBoost(mc) }
    private fun applyBoost(client: Minecraft) { client.options.renderDistance().set(6); client.options.simulationDistance().set(5); client.options.cloudStatus().set(CloudStatus.OFF); client.options.cloudRange().set(32); client.options.weatherRadius().set(5); client.options.particles().set(ParticleStatus.MINIMAL); client.options.entityShadows().set(false); client.options.ambientOcclusion().set(false); client.options.biomeBlendRadius().set(0); client.options.entityDistanceScaling().set(.5); client.options.mipmapLevels().set(0); client.options.framerateLimit().set(260); client.options.enableVsync().set(false); client.options.save() }
    private fun changed(c: Minecraft) = c.options.renderDistance().get() != 6 || c.options.simulationDistance().get() != 5 || c.options.cloudStatus().get() != CloudStatus.OFF || c.options.cloudRange().get() != 32 || c.options.weatherRadius().get() != 5 || c.options.particles().get() != ParticleStatus.MINIMAL || c.options.entityShadows().get() || c.options.ambientOcclusion().get() || c.options.biomeBlendRadius().get() != 0 || c.options.entityDistanceScaling().get() != .5 || c.options.mipmapLevels().get() != 0 || c.options.framerateLimit().get() != 260 || c.options.enableVsync().get()
    private fun toast(title: String, message: String) { PolyNotificationHost.push(title, message, PolyNotificationHost.Type.INFO) }
    private data class Snapshot(val renderDistance: Int, val simulationDistance: Int, val entityDistanceScaling: Double, val framerateLimit: Int, val graphicsPreset: GraphicsPreset, val cloudStatus: CloudStatus, val cloudRange: Int, val weatherRadius: Int, val ambientOcclusion: Boolean, val mipmapLevels: Int, val biomeBlendRadius: Int, val enableVsync: Boolean, val entityShadows: Boolean, val guiScale: Int, val particles: ParticleStatus) {
        fun restore(c: Minecraft) { c.options.renderDistance().set(renderDistance); c.options.simulationDistance().set(simulationDistance); c.options.entityDistanceScaling().set(entityDistanceScaling); c.options.framerateLimit().set(framerateLimit); c.options.graphicsPreset().set(graphicsPreset); c.options.cloudStatus().set(cloudStatus); c.options.cloudRange().set(cloudRange); c.options.weatherRadius().set(weatherRadius); c.options.ambientOcclusion().set(ambientOcclusion); c.options.mipmapLevels().set(mipmapLevels); c.options.biomeBlendRadius().set(biomeBlendRadius); c.options.enableVsync().set(enableVsync); c.options.entityShadows().set(entityShadows); c.options.guiScale().set(guiScale); c.options.particles().set(particles) }
        companion object { fun capture(c: Minecraft) = Snapshot(c.options.renderDistance().get(), c.options.simulationDistance().get(), c.options.entityDistanceScaling().get(), c.options.framerateLimit().get(), c.options.graphicsPreset().get(), c.options.cloudStatus().get(), c.options.cloudRange().get(), c.options.weatherRadius().get(), c.options.ambientOcclusion().get(), c.options.mipmapLevels().get(), c.options.biomeBlendRadius().get(), c.options.enableVsync().get(), c.options.entityShadows().get(), c.options.guiScale().get(), c.options.particles().get()) }
    }
}