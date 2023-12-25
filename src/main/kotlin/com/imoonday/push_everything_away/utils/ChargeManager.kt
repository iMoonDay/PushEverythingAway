package com.imoonday.push_everything_away.utils

import com.imoonday.push_everything_away.client.PushEverythingAwayClient
import com.imoonday.push_everything_away.config.Config
import com.imoonday.push_everything_away.enchantments.SlamEnchantment
import com.imoonday.push_everything_away.network.NetworkHandler
import com.imoonday.push_everything_away.utils.Pushable.Companion.getAllPos
import com.imoonday.push_everything_away.utils.Pushable.Companion.tryPushBlock
import com.imoonday.push_everything_away.utils.PushableObject.Companion.getMaxAttribute
import net.minecraft.client.MinecraftClient
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World

object ChargeManager {
    @JvmField
    var chargingTimes = 0
    private var chargingSlot = -1

    @JvmField
    var cooling = false

    @JvmStatic
    fun MinecraftClient.checkChargingStatus() {
        val player = player ?: return
        if (cooling) {
            if (--chargingTimes <= 0) {
                chargingTimes = 0
                cooling = false
            }
            return
        }
        val stack = player.mainHandStack
        val canCharge = EnchantmentHelper.get(stack).keys.stream()
            .anyMatch { enchantment: Enchantment? -> enchantment is SlamEnchantment }
        if (canCharge) {
            if (PushEverythingAwayClient.CHARGE.isPressed || options.useKey.isPressed && player.isSneaking) {
                if (chargingSlot == -1) {
                    chargingSlot = player.inventory.selectedSlot
                }
                if (chargingSlot != player.inventory.selectedSlot) {
                    chargingTimes = 0
                    chargingSlot = player.inventory.selectedSlot
                } else {
                    chargingTimes++
                }
            } else if (options.useKey.isPressed && !player.isSneaking) {
                chargingTimes = 0
                chargingSlot = -1
            } else if (chargingTimes > 0) {
                var pos: BlockPos? = null
                val crosshairTarget = crosshairTarget
                if (crosshairTarget is BlockHitResult && crosshairTarget.getType() == HitResult.Type.BLOCK) {
                    pos = crosshairTarget.blockPos
                } else if (interactionManager != null) {
                    val raycast = this.player!!.raycast(
                        interactionManager!!.getReachDistance().toDouble(),
                        tickDelta,
                        false
                    )
                    pos =
                        if (raycast is BlockHitResult && raycast.getType() == HitResult.Type.BLOCK) raycast.blockPos else BlockPos.ofFloored(
                            raycast.pos
                        )
                }
                if (pos != null) {
                    NetworkHandler.stopCharging(stack, pos, Hand.MAIN_HAND, chargingTimes)
                }
                chargingSlot = -1
            }
        } else if (chargingTimes != 0 || chargingSlot != -1) {
            chargingTimes = 0
            chargingSlot = -1
        }
    }

    @JvmStatic
    fun PlayerEntity.onStoppedCharging(
        stack: ItemStack,
        world: World,
        pos: BlockPos,
        hand: Hand,
        chargingTicks: Int,
    ): Int {
        var cooldown = 0
        if (!world.isClient) {
            if (this !is CooledPushable) {
                return 0
            }
            val pushCooldown: Int = `pushEverythingAway$getPushCooldown`()
            if (pushCooldown > 0) {
                sendMessage(
                    Text.translatable("text.push_everything_away.cooling", pushCooldown)
                        .formatted(Formatting.GREEN, Formatting.BOLD), true
                )
                return 0
            }
            val speed: Float = stack.getMaxAttribute { obj: PushableObject, level: Int -> obj.getPushSpeed(level) }
                .toFloat()
            val actualTicks = chargingTicks * speed
            val chargeTime = Config.instance.enchantmentSettings.chargeTime
            val progress = (actualTicks / chargeTime).coerceIn(0.0f, 1.0f)
            val strength: Float =
                stack.getMaxAttribute { obj: PushableObject, level: Int -> obj.getPushStrength(level) }
                    .toFloat() * progress
            if (strength <= 0) {
                return 0
            }
            val range: Int = (stack.getMaxAttribute { obj: PushableObject, level: Int -> obj.getPushRange(level) }
                .toInt() * progress).toInt()
            if (range < 0) {
                return 0
            }
            val posList: List<BlockPos> = pos.getAllPos(range)
            posList.sortedWith(compareByDescending<Vec3i> { it.y }.thenBy { it.getSquaredDistance(pos) })
            for (blockPos in posList) {
                tryPushBlock(
                    world = world,
                    hand = hand,
                    pos = blockPos,
                    pushStrength = strength,
                    alwaysUpward = true,
                    breakBlock = false
                )
            }
            cooldown = actualTicks.coerceIn(chargeTime / 2.0f, chargeTime.toFloat()).toInt()
            `pushEverythingAway$setPushCooldown`(cooldown)
            stack.damage<PlayerEntity>(
                ((range + 1) * strength).toInt(),
                this
            ) { it.sendToolBreakStatus(hand) }
            world.playSound(null, blockPos, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS)
            swingHand(Hand.MAIN_HAND, true)
        }
        return cooldown
    }

    @JvmStatic
    fun onStartCooling(cooldown: Int) {
        cooling = true
        chargingTimes = cooldown
    }
}
