package com.imoonday.push_everything_away.utils

import com.imoonday.push_everything_away.api.PushBlockEvents
import com.imoonday.push_everything_away.enchantments.SlamEnchantment
import com.imoonday.push_everything_away.entities.PushedBlockEntity
import net.minecraft.block.Blocks
import net.minecraft.block.FluidBlock
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.ToolItem
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.function.BiFunction

interface Pushable {

    val pushStrengthInterval: Double
        get() = 30.0
    val pushRangeInterval: Int
        get() = 20
    val baseDistance: Double
    val maxPushStrength: Int
    val maxRange: Int
    fun getPushStrength(usedTime: Int) =
        (usedTime / pushStrengthInterval + 1.0).coerceIn(1.0, maxPushStrength.toDouble()).toInt()

    fun getPushRange(usedTime: Int) = (usedTime / pushRangeInterval).coerceIn(0, maxRange)
    fun canPushAway(world: World, pos: BlockPos) = DEFAULT_PREDICATE.apply(world, pos)
    fun getDistance(usedTime: Int) =
        baseDistance + (usedTime.toDouble() / pushRangeInterval).coerceIn(0.0, maxRange.toDouble())

    companion object {

        @JvmField
        val DEFAULT_PREDICATE = BiFunction { world: World, pos: BlockPos ->
            val state = world.getBlockState(pos)
            (!state.contains(Properties.EXTENDED) || !state.get(Properties.EXTENDED)) && !state.isOf(Blocks.PISTON_HEAD) && !state.isOf(
                Blocks.MOVING_PISTON
            ) && !state.isOf(Blocks.BUBBLE_COLUMN) && state.getHardness(
                world,
                pos
            ) >= 0 && !state.isAir && state.block !is FluidBlock
        }

        @JvmStatic
        fun PlayerEntity.tryPushBlock(
            world: World,
            hand: Hand,
            pos: BlockPos,
            pushStrength: Float,
            alwaysUpward: Boolean,
            breakBlock: Boolean,
        ): ActionResult {
            val stack = getStackInHand(hand)
            if (world.isClient) {
                return ActionResult.PASS
            }
            val predicate = stack.getPushPredicate(breakBlock)
            if (!predicate.apply(world, pos)) {
                return ActionResult.FAIL
            }
            if (!PushBlockEvents.CAN_PUSH_AWAY.invoker().canPushAway(this, world, hand, pos, pushStrength)) {
                return ActionResult.FAIL
            }
            var rotationVector = rotationVector.multiply(pushStrength.toDouble())
            if (alwaysUpward && rotationVector.y < 0) {
                rotationVector = Vec3d(rotationVector.x, -rotationVector.y, rotationVector.z)
            }
            val entity = PushedBlockEntity.createEntity(world, pos) { rotationVector }
            PushBlockEvents.BEFORE_SPAWN.invoker().beforeSpawn(entity, this, world, hand, pos, pushStrength)
            world.spawnEntity(entity)
            return ActionResult.CONSUME
        }

        @JvmStatic
        fun ItemStack.getPushPredicate(breakBlock: Boolean): BiFunction<World, BlockPos, Boolean> {
            val item = item
            when {
                item is Pushable -> {
                    return BiFunction<World, BlockPos, Boolean> { world: World, pos: BlockPos ->
                        item.canPushAway(
                            world = world,
                            pos = pos
                        )
                    }
                }

                !breakBlock && EnchantmentHelper.get(this).keys.stream()
                    .anyMatch { it is SlamEnchantment }
                -> {
                    return if (item is ToolItem) {
                        val materials: List<HammerMaterial> =
                            HammerMaterial.fromToolMaterial(item.material)
                        if (materials.isNotEmpty()) BiFunction<World, BlockPos, Boolean>(materials[0]::canPushAway) else DEFAULT_PREDICATE
                    } else DEFAULT_PREDICATE
                }

                else -> return BiFunction { _: World, _: BlockPos -> false }
            }
        }

        @JvmStatic
        fun BlockPos.createBox(rotationVector: Vec3d, r: Int): Box {
            val direction = Direction.getFacing(rotationVector.x, rotationVector.y, rotationVector.z)
            val minPos: BlockPos
            val maxPos: BlockPos
            when (direction) {
                Direction.UP -> {
                    minPos = add(-r, 0, -r)
                    maxPos = add(r, 2 * r, r)
                }

                Direction.DOWN -> {
                    minPos = add(-r, -2 * r, -r)
                    maxPos = add(r, 0, r)
                }

                Direction.NORTH -> {
                    minPos = add(-r, -r, -2 * r)
                    maxPos = add(r, r, 0)
                }

                Direction.SOUTH -> {
                    minPos = add(-r, -r, 0)
                    maxPos = add(r, r, 2 * r)
                }

                Direction.WEST -> {
                    minPos = add(-2 * r, -r, -r)
                    maxPos = add(0, r, r)
                }

                Direction.EAST -> {
                    minPos = add(0, -r, -r)
                    maxPos = add(2 * r, r, r)
                }

                else -> {
                    minPos = this
                    maxPos = this
                }
            }
            return Box(minPos, maxPos)
        }

        @JvmStatic
        fun Vec3d.createBox(rotationVector: Vec3d, r: Double): Box? {
            val direction = Direction.getFacing(rotationVector.x, rotationVector.y, rotationVector.z)
            val minPos: Vec3d
            val maxPos: Vec3d
            when (direction) {
                Direction.UP -> {
                    minPos = add(-r, 0.0, -r)
                    maxPos = add(r, 2 * r, r)
                }

                Direction.DOWN -> {
                    minPos = add(-r, -2 * r, -r)
                    maxPos = add(r, 0.0, r)
                }

                Direction.NORTH -> {
                    minPos = add(-r, -r, -2 * r)
                    maxPos = add(r, r, 0.0)
                }

                Direction.SOUTH -> {
                    minPos = add(-r, -r, 0.0)
                    maxPos = add(r, r, 2 * r)
                }

                Direction.WEST -> {
                    minPos = add(-2 * r, -r, -r)
                    maxPos = add(0.0, r, r)
                }

                Direction.EAST -> {
                    minPos = add(0.0, -r, -r)
                    maxPos = add(2 * r, r, r)
                }

                else -> {
                    minPos = this
                    maxPos = this
                }
            }
            val directionVector = direction.opposite.unitVector.div(2f)
            return Box(minPos, maxPos).offset(
                directionVector.x.toDouble(),
                directionVector.y.toDouble(),
                directionVector.z.toDouble()
            )
        }

        @JvmStatic
        fun BlockPos.getAllPos(range: Int): List<BlockPos> {
            val list: MutableList<BlockPos> = ArrayList()
            for (x in -range..range) {
                for (y in -range..range) {
                    for (z in -range..range) {
                        list.add(add(x, y, z))
                    }
                }
            }
            return list
        }
    }
}
