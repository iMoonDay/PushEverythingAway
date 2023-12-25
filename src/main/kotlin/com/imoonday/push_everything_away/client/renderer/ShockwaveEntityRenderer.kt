package com.imoonday.push_everything_away.client.renderer

import com.imoonday.push_everything_away.PushEverythingAway
import com.imoonday.push_everything_away.entities.ShockwaveEntity
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RotationAxis
import org.joml.Matrix3f
import org.joml.Matrix4f

class ShockwaveEntityRenderer(context: EntityRendererFactory.Context) : EntityRenderer<ShockwaveEntity>(context) {

    override fun getBlockLight(dragonFireballEntity: ShockwaveEntity, blockPos: BlockPos) = 15

    override fun render(
        entity: ShockwaveEntity,
        f: Float,
        g: Float,
        matrixStack: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
        i: Int,
    ) {
        matrixStack.push()
        val power = entity.power
        matrixStack.scale(power * 2.0f, power * 2.0f, power * 2.0f)
        matrixStack.multiply(dispatcher.rotation)
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f))
        val entry = matrixStack.peek()
        val matrix4f = entry.positionMatrix
        val matrix3f = entry.normalMatrix
        val vertexConsumer = vertexConsumerProvider.getBuffer(LAYER)
        vertexConsumer.produceVertex(matrix4f, matrix3f, i, 0.0f, 0, 0, 1)
        vertexConsumer.produceVertex(matrix4f, matrix3f, i, 1.0f, 0, 1, 1)
        vertexConsumer.produceVertex(matrix4f, matrix3f, i, 1.0f, 1, 1, 0)
        vertexConsumer.produceVertex(matrix4f, matrix3f, i, 0.0f, 1, 0, 0)
        matrixStack.pop()
        super.render(entity, f, g, matrixStack, vertexConsumerProvider, i)
    }

    private fun VertexConsumer.produceVertex(
        positionMatrix: Matrix4f,
        normalMatrix: Matrix3f,
        light: Int,
        x: Float,
        y: Int,
        textureU: Int,
        textureV: Int,
    ) = vertex(positionMatrix, x - 0.5f, y.toFloat() - 0.25f, 0.0f).color(255, 255, 255, 255)
        .texture(textureU.toFloat(), textureV.toFloat()).overlay(OverlayTexture.DEFAULT_UV).light(light)
        .normal(normalMatrix, 0.0f, 1.0f, 0.0f).next()

    override fun getTexture(dragonFireballEntity: ShockwaveEntity) = TEXTURE

    companion object {
        private val TEXTURE = Identifier(PushEverythingAway.MOD_ID, "textures/entity/shockwave.png")
        private val LAYER: RenderLayer = RenderLayer.getEntityCutoutNoCull(TEXTURE)
    }
}