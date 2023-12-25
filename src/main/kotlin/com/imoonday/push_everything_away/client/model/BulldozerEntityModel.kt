package com.imoonday.push_everything_away.client.model

import com.imoonday.push_everything_away.entities.BulldozerEntity
import net.minecraft.client.model.*
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.entity.model.EntityModel
import net.minecraft.client.util.math.MatrixStack

class BulldozerEntityModel(root: ModelPart) : EntityModel<BulldozerEntity>() {

    private val body: ModelPart = root.getChild("body")
    private val head: ModelPart = root.getChild("head")

    override fun setAngles(
        entity: BulldozerEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
    ) {
    }

    override fun render(
        matrices: MatrixStack,
        vertexConsumer: VertexConsumer,
        light: Int,
        overlay: Int,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float,
    ) {
        body.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha)
        head.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha)
    }

    companion object {
        val texturedModelData: TexturedModelData
            get() {
                val modelData = ModelData()
                val modelPartData = modelData.root
                val body = modelPartData.addChild(
                    "body",
                    ModelPartBuilder.create().uv(0, 68).cuboid(-9.0f, -8.0f, -9.0f, 18.0f, 7.0f, 1.0f, Dilation(0.0f))
                        .uv(0, 0).cuboid(-9.0f, -1.0f, -9.0f, 18.0f, 1.0f, 18.0f, Dilation(0.0f))
                        .uv(0, 4).cuboid(-0.5f, -6.0f, 5.5f, 1.0f, 5.0f, 1.0f, Dilation(0.0f))
                        .uv(58, 63).cuboid(-9.0f, -8.0f, 8.0f, 18.0f, 7.0f, 1.0f, Dilation(0.0f))
                        .uv(60, 3).cuboid(-9.0f, -8.0f, -8.0f, 1.0f, 7.0f, 16.0f, Dilation(0.0f))
                        .uv(62, 26).cuboid(8.0f, -8.0f, -8.0f, 1.0f, 7.0f, 16.0f, Dilation(0.0f)),
                    ModelTransform.pivot(0.0f, 24.0f, 0.0f)
                )
                body.addChild(
                    "steering_wheel_r1",
                    ModelPartBuilder.create().uv(0, 0).cuboid(-1.5f, -0.5f, -1.5f, 3.0f, 1.0f, 3.0f, Dilation(0.0f)),
                    ModelTransform.of(0.0f, -6.0208f, 5.7929f, 0.7854f, 0.0f, 0.0f)
                )
                body.addChild(
                    "right_up_r1",
                    ModelPartBuilder.create().uv(20, 24)
                        .cuboid(4.709f, -12.8433f, -10.2358f, 1.0f, 5.0f, 18.0f, Dilation(0.0f)),
                    ModelTransform.of(0.0002f, -3.562f, 1.2358f, 0.0f, 0.0f, 0.5236f)
                )
                body.addChild(
                    "right_middle_r1",
                    ModelPartBuilder.create().uv(40, 19).cuboid(-3.8f, -0.5f, -9.0f, 1.0f, 4.0f, 18.0f, Dilation(0.0f)),
                    ModelTransform.of(13.4548f, -4.495f, 0.0f, 0.0f, 0.0f, 0.7854f)
                )
                body.addChild(
                    "right_down_r1",
                    ModelPartBuilder.create().uv(0, 42).cuboid(-0.5f, -6.5f, -9.0f, 1.0f, 3.0f, 18.0f, Dilation(0.0f)),
                    ModelTransform.of(5.7189f, 0.317f, 0.0f, 0.0f, 0.0f, 1.0472f)
                )
                body.addChild(
                    "left_up_r1",
                    ModelPartBuilder.create().uv(0, 19).cuboid(-0.5f, -1.5f, -9.0f, 1.0f, 5.0f, 18.0f, Dilation(0.0f)),
                    ModelTransform.of(-10.183f, -10.7811f, 0.0f, 0.0f, 0.0f, -0.5236f)
                )
                body.addChild(
                    "left_middle_r1",
                    ModelPartBuilder.create().uv(40, 41).cuboid(-3.8f, 1.5f, -9.0f, 1.0f, 4.0f, 18.0f, Dilation(0.0f)),
                    ModelTransform.of(-10.2021f, -10.5761f, 0.0f, 0.0f, 0.0f, -0.7854f)
                )
                body.addChild(
                    "left_down_r1",
                    ModelPartBuilder.create().uv(20, 47).cuboid(-0.5f, -0.5f, -9.0f, 1.0f, 3.0f, 18.0f, Dilation(0.0f)),
                    ModelTransform.of(-10.9151f, -2.683f, 0.0f, 0.0f, 0.0f, -1.0472f)
                )
                body.addChild(
                    "back_down_r1",
                    ModelPartBuilder.create().uv(20, 19).cuboid(-9.0f, 0.5f, -9.5f, 18.0f, 3.0f, 1.0f, Dilation(0.0f)),
                    ModelTransform.of(0.0f, -10.9772f, -7.2811f, 1.0472f, 0.0f, 0.0f)
                )
                body.addChild(
                    "back_middle_r1",
                    ModelPartBuilder.create().uv(38, 71).cuboid(-9.0f, -1.5f, -6.5f, 18.0f, 5.0f, 1.0f, Dilation(0.0f)),
                    ModelTransform.of(0.0f, -11.0711f, -6.8787f, 0.7854f, 0.0f, 0.0f)
                )
                body.addChild(
                    "back_up_r1",
                    ModelPartBuilder.create().uv(60, 49).cuboid(-9.0f, -3.5f, -0.5f, 18.0f, 7.0f, 1.0f, Dilation(0.0f)),
                    ModelTransform.of(0.0f, -10.7811f, -10.183f, 0.5236f, 0.0f, 0.0f)
                )
                val head = modelPartData.addChild(
                    "head",
                    ModelPartBuilder.create().uv(4, 7).cuboid(-5.0f, -4.5f, 9.0f, 2.0f, 2.0f, 1.0f, Dilation(0.0f))
                        .uv(4, 4).cuboid(3.0f, -4.5f, 9.0f, 2.0f, 2.0f, 1.0f, Dilation(0.0f))
                        .uv(0, 76).cuboid(-8.0f, -5.5f, 10.0f, 16.0f, 4.0f, 1.0f, Dilation(0.0f)),
                    ModelTransform.pivot(0.0f, 23.5f, 0.0f)
                )
                head.addChild(
                    "plate_up_r1",
                    ModelPartBuilder.create().uv(76, 71)
                        .cuboid(-8.0f, -8.5141f, 6.4014f, 16.0f, 2.0f, 1.0f, Dilation(0.0f)),
                    ModelTransform.of(0.0f, -3.0593f, 1.1992f, -0.5236f, 0.0f, 0.0f)
                )
                head.addChild(
                    "plate_down_r1",
                    ModelPartBuilder.create().uv(54, 0)
                        .cuboid(-8.0f, 5.7501f, 6.8447f, 16.0f, 2.0f, 1.0f, Dilation(0.0f)),
                    ModelTransform.of(0.0f, -3.0574f, 1.1972f, 0.5236f, 0.0f, 0.0f)
                )
                return TexturedModelData.of(modelData, 128, 128)
            }
    }
}