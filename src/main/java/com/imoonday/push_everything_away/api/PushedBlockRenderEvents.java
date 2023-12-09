package com.imoonday.push_everything_away.api;

import com.imoonday.push_everything_away.entities.PushedBlockEntity;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public interface PushedBlockRenderEvents {

    Event<PushedBlockRenderEvents> EVENT = EventFactory.createArrayBacked(PushedBlockRenderEvents.class,
            entityRenderers -> (entity, yaw, tickDelta, matrices, vertexConsumers, light) -> {
                boolean rendered = false;
                for (PushedBlockRenderEvents renderer : entityRenderers) {
                    if (renderer.render(entity, yaw, tickDelta, matrices, vertexConsumers, light)) {
                        rendered = true;
                    }
                }
                return rendered;
            });

    boolean render(PushedBlockEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);
}
