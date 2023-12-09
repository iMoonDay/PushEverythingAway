package com.imoonday.push_everything_away.api;

import com.imoonday.push_everything_away.entities.PushedBlockEntity;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface PushBlockEvents {

    Event<CanPushAway> CAN_PUSH_AWAY = EventFactory.createArrayBacked(CanPushAway.class,
            events -> (player, world, hand, pos, pushStrength) -> {
                for (CanPushAway event : events) {
                    boolean result = event.canPushAway(player, world, hand, pos, pushStrength);
                    if (!result) {
                        return false;
                    }
                }
                return true;
            });

    Event<BeforeSpawn> BEFORE_SPAWN = EventFactory.createArrayBacked(BeforeSpawn.class,
            events -> (entity, player, world, hand, pos, pushStrength) -> {
                for (BeforeSpawn event : events) {
                    event.beforeSpawn(entity, player, world, hand, pos, pushStrength);
                }
            });

    @FunctionalInterface
    interface CanPushAway {
        boolean canPushAway(PlayerEntity player, World world, Hand hand, BlockPos pos, float pushStrength);
    }

    @FunctionalInterface
    interface BeforeSpawn {
        void beforeSpawn(PushedBlockEntity entity, PlayerEntity player, World world, Hand hand, BlockPos pos, float pushStrength);
    }
}
