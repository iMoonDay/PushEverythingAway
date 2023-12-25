package com.imoonday.push_everything_away.items

import com.imoonday.push_everything_away.config.ClientConfig
import com.imoonday.push_everything_away.utils.HammerMaterial
import com.imoonday.push_everything_away.utils.Pushable
import com.imoonday.push_everything_away.utils.Pushable.Companion.createBox
import com.imoonday.push_everything_away.utils.Pushable.Companion.getAllPos
import com.imoonday.push_everything_away.utils.Pushable.Companion.tryPushBlock
import me.x150.renderer.render.Renderer3d
import me.x150.renderer.util.RendererUtils
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.yarn.constants.MiningLevels
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.SwordItem
import net.minecraft.registry.tag.BlockTags
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.Rarity
import net.minecraft.util.TypedActionResult
import net.minecraft.util.UseAction
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.awt.Color

class GroundHammerItem(private val hammerMaterial: HammerMaterial, settings: Settings) : SwordItem(
    hammerMaterial,
    9,
    if (hammerMaterial.isSpecial) 1.0f else -3.5f,
    settings.rarity(if (hammerMaterial.isSpecial) Rarity.EPIC else Rarity.COMMON)
), Pushable {

    val isSpecial: Boolean
        get() = hammerMaterial.isSpecial
    override val baseDistance: Double
        get() = hammerMaterial.baseDistance
    override val maxPushStrength: Int
        get() = hammerMaterial.maxPushStrength
    override val maxRange: Int
        get() = hammerMaterial.maxRange
    override val pushRangeInterval: Int
        get() = hammerMaterial.pushRangeInterval
    override val pushStrengthInterval: Double
        get() = hammerMaterial.pushStrengthInterval

    override fun canMine(state: BlockState, world: World, pos: BlockPos, miner: PlayerEntity) = true
    override fun getMiningSpeedMultiplier(stack: ItemStack, state: BlockState) =
        if (this.isSuitableFor(state)) hammerMaterial.getMiningSpeedMultiplier() else 1.0f

    override fun isSuitableFor(state: BlockState): Boolean {
        val i = material.miningLevel
        if (i < MiningLevels.DIAMOND && state.isIn(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return false
        }
        return if (i < MiningLevels.IRON && state.isIn(BlockTags.NEEDS_IRON_TOOL)) false
        else i >= MiningLevels.STONE || !state.isIn(BlockTags.NEEDS_STONE_TOOL)
    }

    override fun getMaxUseTime(stack: ItemStack) = 72000
    override fun getUseAction(stack: ItemStack) = UseAction.BOW
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
        super.onStoppedUsing(stack, world, user, remainingUseTicks)
        if (!world.isClient && user is PlayerEntity) {
            val usedTime = getMaxUseTime(stack) - remainingUseTicks
            val raycast = user.raycast(getDistance(usedTime), 0.0f, false)
            val center =
                if (raycast is BlockHitResult && raycast.getType() == HitResult.Type.BLOCK) raycast.blockPos else BlockPos.ofFloored(
                    raycast.pos
                )
            val range = getPushRange(usedTime)
            val pushStrength = getPushStrength(usedTime)
            val vector = user.getRotationVector()
            val posList: MutableList<BlockPos> = ArrayList()
            if (isSpecial) {
                posList.addAll(center.getAllPos(range))
            } else {
                val box = center.createBox(vector, range)
                BlockPos.stream(box).forEach { posList.add(BlockPos(it)) }
            }
            posList.sortWith(compareByDescending<BlockPos> { it.y }.thenBy { it.getSquaredDistance(center) })
            for (pos in posList) {
                user.tryPushBlock(
                    world = world,
                    hand = user.getActiveHand(),
                    pos = pos,
                    pushStrength = pushStrength.toFloat(),
                    alwaysUpward = false,
                    breakBlock = false
                )
            }
            if (!isSpecial) {
                user.itemCooldownManager[this] = 10 * (range + 1)
                stack.damage((range + 1) * pushStrength, user) { it.sendToolBreakStatus(user.getActiveHand()) }
            }
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS)
        }
    }

    override fun hasGlint(stack: ItemStack) = isSpecial || super.hasGlint(stack)
    override fun canPushAway(world: World, pos: BlockPos) = hammerMaterial.canPushAway(world, pos)
    override fun getDistance(usedTime: Int) = hammerMaterial.getDistance(usedTime)
    override fun getPushRange(usedTime: Int) = hammerMaterial.getPushRange(usedTime)
    override fun getPushStrength(usedTime: Int) = hammerMaterial.getPushStrength(usedTime)

    companion object {
        @JvmStatic
        fun renderSelectionBoxes(context: WorldRenderContext) {
            val client = MinecraftClient.getInstance() ?: return
            val player = client.player ?: return
            if (player.isSpectator()) {
                return
            }
            var item = player.mainHandStack.item
            if (item !is GroundHammerItem) {
                item = player.offHandStack.item
            }
            if (item !is GroundHammerItem) {
                return
            }
            var box: Box? = null
            var color = Color.GREEN
            val boxes: MutableSet<Box> = HashSet()
            val special = item.isSpecial
            if (!player.isUsingItem) {
                val hitResult = client.crosshairTarget
                if (hitResult is BlockHitResult && hitResult.getType() == HitResult.Type.BLOCK) {
                    val world = client.world
                    if (world != null) {
                        val pos = hitResult.blockPos
                        box = world.getBlockState(pos).getOutlineShape(world, pos, ShapeContext.of(player))
                            .getBoundingBox().offset(pos)
                    }
                }
            } else {
                val activeItem = player.activeItem
                val stackInHand = player.getStackInHand(player.activeHand)
                if (activeItem != stackInHand) {
                    return
                }
                val usedTime = player.getItemUseTime()
                val expandRange = item.getPushRange(usedTime)
                val raycast = player.raycast(item.getDistance(usedTime), 0.0f, false)
                val blockPos =
                    if (raycast is BlockHitResult && raycast.getType() == HitResult.Type.BLOCK) raycast.blockPos else BlockPos.ofFloored(
                        raycast.pos
                    )
                val vector = player.rotationVector
                var selectionRange =
                    (usedTime.toDouble() / item.pushRangeInterval).coerceIn(0.0, (item.maxRange + 1).toDouble())
                if (selectionRange < 1) selectionRange /= 2.0 else selectionRange -= 0.5
                val selectionPos =
                    if (raycast is BlockHitResult && raycast.getType() == HitResult.Type.BLOCK) raycast.blockPos.toCenterPos() else raycast.pos
                box = if (special) Box.from(selectionPos).expand(selectionRange) else selectionPos.createBox(
                    rotationVector = vector,
                    r = selectionRange
                )
                val pushStrength = item.getPushStrength(usedTime)
                color = RendererUtils.lerp(Color.GREEN, Color.RED, pushStrength.toDouble() / item.maxPushStrength)
                if (ClientConfig.instance.displaySelectionOutlines) {
                    val stream = if (special) blockPos.getAllPos(expandRange).stream() else BlockPos.stream(
                        blockPos.createBox(
                            rotationVector = vector,
                            r = expandRange
                        )
                    )
                    stream.map {
                        client.world!!.getBlockState(it).getOutlineShape(client.world, it, ShapeContext.of(player))
                            .offset(it.x.toDouble(), it.y.toDouble(), it.z.toDouble())
                    }
                        .filter { !it.isEmpty }
                        .forEach { boxes.addAll(it.getBoundingBoxes()) }
                }
            }
            for (box1 in boxes) {
                Renderer3d.renderEdged(
                    context.matrixStack(),
                    RendererUtils.modify(Color.WHITE, -1, -1, -1, 75),
                    Color.WHITE,
                    Vec3d(box1.minX, box1.minY, box1.minZ),
                    Vec3d(box1.lengthX, box1.lengthY, box1.lengthZ)
                )
            }
            if (box != null) {
                box = box.expand(0.02)
                Renderer3d.renderEdged(
                    context.matrixStack(),
                    RendererUtils.modify(color!!, -1, -1, -1, 50),
                    color,
                    Vec3d(box.minX, box.minY, box.minZ),
                    Vec3d(box.lengthX, box.lengthY, box.lengthZ)
                )
            }
        }
    }
}
