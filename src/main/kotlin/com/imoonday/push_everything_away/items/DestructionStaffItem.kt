package com.imoonday.push_everything_away.items

import com.imoonday.push_everything_away.entities.ShockwaveEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.UseAction
import net.minecraft.world.World

class DestructionStaffItem(settings: Settings) : Item(settings) {

    override fun getMaxUseTime(stack: ItemStack?) = 72000

    override fun getUseAction(stack: ItemStack?) = UseAction.BOW

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        val canUse = !stack.isDamageable || stack.damage < stack.maxDamage
        if (user.abilities.creativeMode || canUse) {
            user.setCurrentHand(hand)
            return TypedActionResult.consume(stack)
        }
        return TypedActionResult.fail(stack)
    }

    override fun onStoppedUsing(stack: ItemStack, world: World, user: LivingEntity, remainingUseTicks: Int) {
        val usedTime = this.getMaxUseTime(stack) - remainingUseTicks
        if (usedTime >= 10 && !world.isClient && stack.damage < stack.maxDamage) {
            val power = (usedTime / 20.0).coerceAtMost(3.0)
            val entity =
                ShockwaveEntity(
                    world = world,
                    pos = user.eyePos,
                    velocity = user.rotationVector.multiply(power),
                    power = power.toFloat(),
                    owner = user
                )
            world.spawnEntity(entity)
            stack.damage(1, user) { it.sendToolBreakStatus(user.activeHand) }
        }
    }
}