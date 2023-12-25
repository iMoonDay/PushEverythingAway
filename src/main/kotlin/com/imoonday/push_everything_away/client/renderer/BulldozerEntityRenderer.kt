package com.imoonday.push_everything_away.client.renderer

import com.imoonday.push_everything_away.PushEverythingAway
import com.imoonday.push_everything_away.client.model.BulldozerEntityModel
import com.imoonday.push_everything_away.entities.BulldozerEntity
import com.imoonday.push_everything_away.init.ModEntities
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.RotationAxis

open class BulldozerEntityRenderer(ctx: EntityRendererFactory.Context) : EntityRenderer<BulldozerEntity>(ctx) {

    private val model: BulldozerEntityModel = BulldozerEntityModel(ctx.getPart(ModEntities.BULLDOZER_MODEL_LAYER))

    override fun render(
        entity: BulldozerEntity,
        yaw: Float,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
    ) {
        matrices.push()
        matrices.translate(0f, 1.5f, 0f)
        matrices.scale(-1.0f, -1.0f, 1.0f)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw(tickDelta)))
        model.render(
            matrices,
            vertexConsumers.getBuffer(model.getLayer(getTexture(entity))),
            light,
            OverlayTexture.DEFAULT_UV,
            1.0f,
            1.0f,
            1.0f,
            1.0f
        )
        matrices.pop()
    }

    override fun getTexture(entity: BulldozerEntity) = TEXTURE

    companion object {
        val TEXTURE = Identifier(PushEverythingAway.MOD_ID, "textures/entity/bulldozer.png")
    }
}
