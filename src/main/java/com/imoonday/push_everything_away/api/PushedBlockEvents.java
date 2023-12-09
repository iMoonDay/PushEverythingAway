package com.imoonday.push_everything_away.api;

import com.imoonday.push_everything_away.entities.PushedBlockEntity;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public interface PushedBlockEvents {

    Event<CanPlace> CAN_PLACE = EventFactory.createArrayBacked(CanPlace.class,
            events -> (entity, pos) -> {
                for (CanPlace event : events) {
                    boolean result = event.canPlace(entity, pos);
                    if (!result) {
                        return false;
                    }
                }
                return true;
            });

    Event<CanTryPlace> CAN_TRY_PLACE = EventFactory.createArrayBacked(CanTryPlace.class,
            events -> entity -> {
                for (CanTryPlace event : events) {
                    boolean result = event.canTryPlace(entity);
                    if (result) {
                        return true;
                    }
                }
                return false;
            });

    Event<OnEntityCollision> ON_ENTITY_COLLISION = EventFactory.createArrayBacked(OnEntityCollision.class,
            events -> (entity, other) -> {
                for (OnEntityCollision event : events) {
                    event.onEntityCollision(entity, other);
                }
            });

    Event<BeforePlaceBlock> BEFORE_PLACE_BLOCK = EventFactory.createArrayBacked(BeforePlaceBlock.class,
            events -> (entity, pos) -> {
                for (BeforePlaceBlock event : events) {
                    boolean result = event.beforePlaceBlock(entity, pos);
                    if (!result) {
                        return false;
                    }
                }
                return true;
            });

    Event<AfterPlaceBlock> AFTER_PLACE_BLOCK = EventFactory.createArrayBacked(AfterPlaceBlock.class,
            events -> (entity, pos) -> {
                for (AfterPlaceBlock event : events) {
                    event.afterPlaceBlock(entity, pos);
                }
            });

    Event<AfterPlaceBlockEntity> AFTER_PLACE_BLOCK_ENTITY = EventFactory.createArrayBacked(AfterPlaceBlockEntity.class,
            events -> (entity, blockEntity, blockPos) -> {
                for (AfterPlaceBlockEntity event : events) {
                    event.afterPlaceBlockEntity(entity, blockEntity, blockPos);
                }
            });

    Event<OnCalculatingBoundingBox> BOUNDING_BOX = EventFactory.createArrayBacked(OnCalculatingBoundingBox.class,
            events -> (entity, box) -> {
                for (OnCalculatingBoundingBox event : events) {
                    Box result = event.onCalculatingBoundingBox(entity, box);
                    if (result != null) {
                        return result;
                    }
                }
                return box;
            });

    Event<BeforeTick> BEFORE_TICK = EventFactory.createArrayBacked(BeforeTick.class,
            events -> entity -> {
                for (BeforeTick event : events) {
                    event.beforeTick(entity);
                }
            });

    Event<AfterTick> AFTER_TICK = EventFactory.createArrayBacked(AfterTick.class,
            events -> entity -> {
                for (AfterTick event : events) {
                    event.afterTick(entity);
                }
            });

    @FunctionalInterface
    interface CanPlace {
        boolean canPlace(PushedBlockEntity entity, BlockPos blockPos);
    }

    @FunctionalInterface
    interface CanTryPlace {
        boolean canTryPlace(PushedBlockEntity entity);
    }

    @FunctionalInterface
    interface OnEntityCollision {
        void onEntityCollision(PushedBlockEntity entity, Entity other);
    }

    @FunctionalInterface
    interface BeforePlaceBlock {
        boolean beforePlaceBlock(PushedBlockEntity entity, BlockPos blockPos);
    }

    @FunctionalInterface
    interface AfterPlaceBlock {
        void afterPlaceBlock(PushedBlockEntity entity, BlockPos blockPos);
    }

    @FunctionalInterface
    interface AfterPlaceBlockEntity {
        void afterPlaceBlockEntity(PushedBlockEntity entity, BlockEntity blockEntity, BlockPos blockPos);
    }

    @FunctionalInterface
    interface OnCalculatingBoundingBox {
        Box onCalculatingBoundingBox(PushedBlockEntity entity, Box boundingBox);
    }

    @FunctionalInterface
    interface BeforeTick {
        void beforeTick(PushedBlockEntity entity);
    }

    @FunctionalInterface
    interface AfterTick {
        void afterTick(PushedBlockEntity entity);
    }
}
