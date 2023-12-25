package com.imoonday.push_everything_away.items

import com.imoonday.push_everything_away.entities.PushedBlockEntity
import com.imoonday.push_everything_away.utils.Grabbable
import com.imoonday.push_everything_away.utils.GrabbingManager
import com.imoonday.push_everything_away.utils.GrabbingManager.pushGrabbingEntity
import com.imoonday.push_everything_away.utils.Pushable
import net.minecraft.item.Item
import net.minecraft.item.ItemUsageContext
import net.minecraft.util.ActionResult

class GravityStaffItem(settings: Settings?) : Item(settings) {

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val pos = context.blockPos
        if (!Pushable.DEFAULT_PREDICATE.apply(world, pos)) {
            return ActionResult.FAIL
        }
        val player = context.player
        if (player !is Grabbable) return ActionResult.FAIL
        if (world.isClient) {
            if (GrabbingManager.grabbing) {
                return ActionResult.PASS
            }
        } else if (player.`pushEverythingAway$getGrabbingEntity`() != null) {
            player.pushGrabbingEntity(0.0f)
            return ActionResult.CONSUME
        }
        if (!world.isClient) {
            val entity = PushedBlockEntity.createEntity(world, pos)
            entity.grabbingOwner = player
            player.`pushEverythingAway$getGrabbingEntity`()?.let { it.grabbingOwner = null }
            player.`pushEverythingAway$setGrabbingEntity`(entity)
            world.spawnEntity(entity)
            context.stack.damage(1, player) { it.sendToolBreakStatus(context.hand) }
        } else {
            GrabbingManager.usingForGrabbing = true
        }
        return ActionResult.success(world.isClient)
    }
}
