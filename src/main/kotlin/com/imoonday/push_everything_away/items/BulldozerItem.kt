package com.imoonday.push_everything_away.items

import com.imoonday.push_everything_away.entities.BulldozerEntity
import net.minecraft.block.DispenserBlock
import net.minecraft.block.dispenser.DispenserBehavior
import net.minecraft.block.dispenser.ItemDispenserBehavior
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPointer
import net.minecraft.world.World
import net.minecraft.world.WorldEvents
import net.minecraft.world.event.GameEvent

class BulldozerItem(settings: Settings) : Item(settings) {
    init {
        DispenserBlock.registerBehavior(this, DISPENSER_BEHAVIOR)
    }

    override fun appendTooltip(
        stack: ItemStack,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext,
    ) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(
            Text.translatable("item.push_everything_away.bulldozer.tooltip").formatted(Formatting.RED, Formatting.BOLD)
        )
    }

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val blockPos = context.blockPos
        val hitPos = context.hitPos
        val world = context.world
        val itemStack = context.stack
        if (!world.isClient) {
            val entity = BulldozerEntity(world, hitPos)
            if (itemStack.hasCustomName()) {
                entity.customName = itemStack.getName()
            }
            entity.setYaw(context.playerYaw)
            entity.prevYaw = entity.yaw
            world.spawnEntity(entity)
            world.emitGameEvent(
                GameEvent.ENTITY_PLACE,
                blockPos,
                GameEvent.Emitter.of(context.player, world.getBlockState(blockPos.down()))
            )
        }
        itemStack.decrement(1)
        return ActionResult.success(world.isClient)
    }

    companion object {
        private val DISPENSER_BEHAVIOR: DispenserBehavior = object : ItemDispenserBehavior() {
            public override fun dispenseSilently(pointer: BlockPointer, stack: ItemStack): ItemStack {
                val world = pointer.world()
                val entity = BulldozerEntity(world, pointer.centerPos())
                if (stack.hasCustomName()) {
                    entity.customName = stack.getName()
                }
                world.spawnEntity(entity)
                stack.decrement(1)
                return stack
            }

            override fun playSound(pointer: BlockPointer) {
                pointer.world().syncWorldEvent(WorldEvents.DISPENSER_DISPENSES, pointer.pos(), 0)
            }
        }
    }
}
