package com.imoonday.push_everything_away.utils;

import com.imoonday.push_everything_away.api.PushBlockEvents;
import com.imoonday.push_everything_away.entities.PushedBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.joml.Vector3f;

public interface Pushable {
    static ActionResult tryPushBlock(PlayerEntity player, World world, Hand hand, BlockPos pos, float pushStrength) {
        if (player == null) {
            return ActionResult.PASS;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient || !(stack.getItem() instanceof Pushable pushable)) {
            return ActionResult.PASS;
        }
        if (!pushable.canPushAway(world, pos)) {
            return ActionResult.FAIL;
        }
        if (!PushBlockEvents.CAN_PUSH_AWAY.invoker().canPushAway(player, world, hand, pos, pushStrength)) {
            return ActionResult.FAIL;
        }
        PushedBlockEntity entity = PushedBlockEntity.createPushedBlockEntity(world, pos);

        Vec3d rotationVector = player.getRotationVector().multiply(pushStrength);
        entity.setVelocity(rotationVector);

        PushBlockEvents.BEFORE_SPAWN.invoker().beforeSpawn(entity, player, world, hand, pos, pushStrength);
        world.spawnEntity(entity);
        return ActionResult.CONSUME;
    }

    static Box createBox(BlockPos blockPos, Vec3d rotationVector, int r) {
        Direction direction = Direction.getFacing(rotationVector.x, rotationVector.y, rotationVector.z);
        BlockPos minPos, maxPos;
        switch (direction) {
            case UP -> {
                minPos = blockPos.add(-r, 0, -r);
                maxPos = blockPos.add(r, 2 * r, r);
            }
            case DOWN -> {
                minPos = blockPos.add(-r, -2 * r, -r);
                maxPos = blockPos.add(r, 0, r);
            }
            case NORTH -> {
                minPos = blockPos.add(-r, -r, -2 * r);
                maxPos = blockPos.add(r, r, 0);
            }
            case SOUTH -> {
                minPos = blockPos.add(-r, -r, 0);
                maxPos = blockPos.add(r, r, 2 * r);
            }
            case WEST -> {
                minPos = blockPos.add(-2 * r, -r, -r);
                maxPos = blockPos.add(0, r, r);
            }
            case EAST -> {
                minPos = blockPos.add(0, -r, -r);
                maxPos = blockPos.add(2 * r, r, r);
            }
            default -> {
                minPos = blockPos;
                maxPos = blockPos;
            }
        }

        return new Box(minPos, maxPos);
    }

    static Box createBox(Vec3d centerPos, Vec3d rotationVector, double r) {
        Direction direction = Direction.getFacing(rotationVector.x, rotationVector.y, rotationVector.z);
        Vec3d minPos, maxPos;
        switch (direction) {
            case UP -> {
                minPos = centerPos.add(-r, 0, -r);
                maxPos = centerPos.add(r, 2 * r, r);
            }
            case DOWN -> {
                minPos = centerPos.add(-r, -2 * r, -r);
                maxPos = centerPos.add(r, 0, r);
            }
            case NORTH -> {
                minPos = centerPos.add(-r, -r, -2 * r);
                maxPos = centerPos.add(r, r, 0);
            }
            case SOUTH -> {
                minPos = centerPos.add(-r, -r, 0);
                maxPos = centerPos.add(r, r, 2 * r);
            }
            case WEST -> {
                minPos = centerPos.add(-2 * r, -r, -r);
                maxPos = centerPos.add(0, r, r);
            }
            case EAST -> {
                minPos = centerPos.add(0, -r, -r);
                maxPos = centerPos.add(2 * r, r, r);
            }
            default -> {
                minPos = centerPos;
                maxPos = centerPos;
            }
        }
        Vector3f directionVector = direction.getOpposite().getUnitVector().div(2);
        return new Box(minPos, maxPos).offset(directionVector.x, directionVector.y, directionVector.z);
    }

    default int getPushStrength(int usedTime) {
        return (int) MathHelper.clamp(usedTime / getPushStrengthInterval() + 1.0, 1, getMaxPushStrength());
    }

    default double getPushStrengthInterval() {
        return 30.0;
    }

    default int getPushRange(int usedTime) {
        return MathHelper.clamp(usedTime / getPushRangeInterval(), 0, getMaxRange());
    }

    default int getPushRangeInterval() {
        return 20;
    }

    default boolean canPushAway(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return (!state.contains(Properties.EXTENDED) || !state.get(Properties.EXTENDED)) && !state.isOf(Blocks.PISTON_HEAD) && !state.isOf(Blocks.MOVING_PISTON) && !state.isOf(Blocks.BUBBLE_COLUMN) && state.getHardness(world, pos) >= 0 && !state.isAir() && !(state.getBlock() instanceof FluidBlock);
    }

    default double getDistance(int usedTime) {
        return getBaseDistance() + MathHelper.clamp((double) usedTime / getPushRangeInterval(), 0.0, getMaxRange());
    }

    double getBaseDistance();

    int getMaxPushStrength();

    int getMaxRange();
}
