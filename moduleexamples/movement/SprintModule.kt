package com.aqua.hud.modules.impl.movement

import com.aqua.hud.modules.Category
import com.aqua.hud.modules.Module

class SprintModule : Module("Sprint", "Auto sprint when moving forward", Category.MOVEMENT) {
    override fun onTick() { if (isInGame() && mc.player!!.input.hasForwardImpulse() && !mc.player!!.isCrouching) mc.player!!.isSprinting = true }
}