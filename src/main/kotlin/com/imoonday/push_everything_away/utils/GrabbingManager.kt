package com.imoonday.push_everything_away.utils

import com.imoonday.push_everything_away.entities.PushedBlockEntity
import com.imoonday.push_everything_away.network.NetworkHandler
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult

object GrabbingManager {
    @JvmField
    var grabbing = false

    @JvmField
    var usingForGrabbing = false

    @JvmStatic
    fun MinecraftClient.checkGrabbingStatus() {
        if (grabbing) {
            val crosshair = crosshairTarget
            if (options.attackKey.isPressed && (crosshair !is BlockHitResult || crosshair.type == HitResult.Type.MISS)) {
                NetworkHandler.pushGrabbingEntity(1.0f)
                if (options.useKey.isPressed) {
                    usingForGrabbing = true
                }
            } else if (options.useKey.isPressed) {
                if (!usingForGrabbing) {
                    NetworkHandler.pushGrabbingEntity(0.0f)
                    usingForGrabbing = true
                }
            }
            if (!options.useKey.isPressed && usingForGrabbing) {
                usingForGrabbing = false
            }
        }
    }

    @JvmStatic
    fun PlayerEntity.pushGrabbingEntity(pushStrength: Float) {
        if (this is Grabbable) {
            val grabbingEntity: PushedBlockEntity? = `pushEverythingAway$getGrabbingEntity`()
            grabbingEntity?.let {
                it.grabbingOwner = null
                it.velocity = rotationVector.multiply(pushStrength.toDouble())
            }
            `pushEverythingAway$setGrabbingEntity`(null)
        }
    }
}
