package com.imoonday.push_everything_away.client.renderer

import com.imoonday.push_everything_away.api.PushedBlockRenderEvents
import com.imoonday.push_everything_away.entities.PushedBlockEntity
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.enums.DoubleBlockHalf
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.RenderLayers
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.BlockRenderManager
import net.minecraft.client.render.entity.EntityRenderer
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction.*
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.random.Random

class PushedBlockEntityRenderer(context: EntityRendererFactory.Context) : EntityRenderer<PushedBlockEntity>(context) {

    private val blockRenderManager: BlockRenderManager = context.blockRenderManager

    init {
        shadowRadius = 0.0f
    }

    override fun render(
        entity: PushedBlockEntity,
        f: Float,
        g: Float,
        matrixStack: MatrixStack,
        vertexConsumerProvider: VertexConsumerProvider,
        i: Int,
    ) {
        if (PushedBlockRenderEvents.EVENT.invoker().render(entity, f, g, matrixStack, vertexConsumerProvider, i)) {
            super.render(entity, f, g, matrixStack, vertexConsumerProvider, i)
            return
        }
        val client = MinecraftClient.getInstance() ?: return
        var blockState = entity.blockState
        val renderType = blockState.renderType
        val block = blockState.block
        val world = entity.world
        if (renderType == BlockRenderType.MODEL) {
            matrixStack.push()
            val blockPos = BlockPos.ofFloored(entity.x, entity.boundingBox.maxY, entity.z)
            matrixStack.translate(-0.5, 0.0, -0.5)
            val shape = blockState.getOutlineShape(world, blockPos, ShapeContext.of(client.cameraEntity))
            val offsetY = -shape.getMin(Axis.Y)
            if (block is WallTorchBlock || block is WallRedstoneTorchBlock) {
                val vector = blockState.get(Properties.HORIZONTAL_FACING).vector
                matrixStack.translate(0.35 * vector.x, offsetY, 0.35 * vector.z)
            } else {
                matrixStack.translate(0.0, offsetY, 0.0)
            }
            if (block is VineBlock) {
                val properties = VineBlock.FACING_PROPERTIES.filter { blockState.get(it.value) }
                if (properties.size == 1) {
                    val vector = properties.keys.first().opposite.vector
                    matrixStack.translate(vector.x * 0.5, vector.y * 0.5, vector.z * 0.5)
                }
            } else if (block is CocoaBlock) {
                val direction = blockState.get(Properties.HORIZONTAL_FACING)
                val age = blockState.get(Properties.AGE_2)
                val vector = direction.opposite.vector
                val multiplier = 0.31 - age * 0.06
                matrixStack.translate(vector.x * multiplier, 0.0, vector.z * multiplier)
            } else if (block is LadderBlock) {
                val direction = blockState.get(Properties.HORIZONTAL_FACING)
                val vector = direction.vector
                matrixStack.translate(vector.x * 0.42, 0.0, vector.z * 0.42)
            }
            val sourcePos = entity.sourcePos
            if (blockState.contains(Properties.DOUBLE_BLOCK_HALF)) {
                if (blockState.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
                    blockRenderManager.modelRenderer.render(
                        world,
                        blockRenderManager.getModel(blockState),
                        blockState,
                        blockPos,
                        matrixStack,
                        vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(blockState)),
                        false,
                        Random.create(),
                        blockState.getRenderingSeed(sourcePos),
                        OverlayTexture.DEFAULT_UV
                    )
                    val offsetState = blockState.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
                    matrixStack.translate(0f, 1f, 0f)
                    blockRenderManager.modelRenderer.render(
                        world,
                        blockRenderManager.getModel(offsetState),
                        offsetState,
                        blockPos.up(),
                        matrixStack,
                        vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(offsetState)),
                        false,
                        Random.create(),
                        offsetState.getRenderingSeed(sourcePos.up()),
                        OverlayTexture.DEFAULT_UV
                    )
                } else {
                    matrixStack.translate(0f, 1f, 0f)
                    blockRenderManager.modelRenderer.render(
                        world,
                        blockRenderManager.getModel(blockState),
                        blockState,
                        blockPos.up(),
                        matrixStack,
                        vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(blockState)),
                        false,
                        Random.create(),
                        blockState.getRenderingSeed(sourcePos),
                        OverlayTexture.DEFAULT_UV
                    )
                    val offsetState = blockState.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                    matrixStack.translate(0f, -1f, 0f)
                    blockRenderManager.modelRenderer.render(
                        world,
                        blockRenderManager.getModel(offsetState),
                        offsetState,
                        blockPos,
                        matrixStack,
                        vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(offsetState)),
                        false,
                        Random.create(),
                        offsetState.getRenderingSeed(sourcePos.down()),
                        OverlayTexture.DEFAULT_UV
                    )
                }
            } else if (block is BedBlock) {
                val direction = BedBlock.getOppositePartDirection(blockState)
                var vector = direction.opposite.vector
                matrixStack.translate(vector.x * 0.5, 0.0, vector.z * 0.5)
                blockRenderManager.modelRenderer.render(
                    world,
                    blockRenderManager.getModel(blockState),
                    blockState,
                    blockPos,
                    matrixStack,
                    vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(blockState)),
                    false,
                    Random.create(),
                    blockState.getRenderingSeed(sourcePos),
                    OverlayTexture.DEFAULT_UV
                )
                vector = direction.vector
                matrixStack.translate(vector.x.toDouble(), 0.0, vector.z.toDouble())
                blockState = blockState.cycle(BedBlock.PART)
                blockRenderManager.modelRenderer.render(
                    world,
                    blockRenderManager.getModel(blockState),
                    blockState,
                    blockPos,
                    matrixStack,
                    vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(blockState)),
                    false,
                    Random.create(),
                    blockState.getRenderingSeed(sourcePos),
                    OverlayTexture.DEFAULT_UV
                )
            } else {
                blockRenderManager.modelRenderer.render(
                    world,
                    blockRenderManager.getModel(blockState),
                    blockState,
                    blockPos,
                    matrixStack,
                    vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(blockState)),
                    false,
                    Random.create(),
                    blockState.getRenderingSeed(sourcePos),
                    OverlayTexture.DEFAULT_UV
                )
            }
            matrixStack.pop()
        }
        if (entity.hasBlockEntityData()) {
            val blockPos = BlockPos.ofFloored(entity.x, entity.boundingBox.maxY, entity.z)
            val blockEntity = BlockEntity.createFromNbt(blockPos, blockState, entity.blockEntityData)
            if (blockEntity != null) {
                val blockEntityRenderDispatcher = client.blockEntityRenderDispatcher
                if (blockEntityRenderDispatcher.get(blockEntity) != null) {
                    matrixStack.push()
                    val isBed = block is BedBlock
                    if (block is WallSkullBlock) {
                        val direction = blockState.get(Properties.HORIZONTAL_FACING)
                        val vector = direction.vector
                        matrixStack.translate(-0.5 + vector.x / 4.0, 0.0, -0.5 + vector.z / 4.0)
                    } else if (blockState.contains(Properties.HORIZONTAL_FACING) && !(block is WallSignBlock || block is WallHangingSignBlock || block is WallBannerBlock || block is ChestBlock)) {
                        val direction = blockState.get(Properties.HORIZONTAL_FACING)!!
                        when (direction) {
                            WEST -> {
                                matrixStack.translate(if (isBed) 0.0 else 0.5, 0.0, -0.5)
                                matrixStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(90f))
                            }

                            EAST -> {
                                matrixStack.translate(if (isBed) 0.0 else -0.5, 0.0, 0.5)
                                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f))
                            }

                            NORTH -> {
                                matrixStack.translate(0.5, 0.0, if (isBed) 0.0 else 0.5)
                                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f))
                            }

                            SOUTH -> matrixStack.translate(-0.5, 0.0, if (isBed) 0.0 else -0.5)

                            else -> {}
                        }
                    } else if (blockState.contains(Properties.FACING)) {
                        val direction = blockState.get(Properties.FACING)
                        val offsetX = -direction.offsetX / 2.0
                        val offsetY = if (direction.offsetY != 0) 0.0 else 1.0
                        val offsetZ = -direction.offsetZ / 2.0
                        matrixStack.translate(offsetX, offsetY, offsetZ)
                        if (direction.axis !== Axis.Y) {
                            val rotated = direction.rotateYClockwise()
                            matrixStack.translate(rotated.offsetX / 2.0, 0.0, rotated.offsetZ / 2.0)
                        } else {
                            matrixStack.translate(
                                -0.5,
                                (if (direction == UP) 0.0 else 1.0),
                                if (direction == UP) -0.5 else 0.5
                            )
                        }
                        matrixStack.multiply(direction.rotationQuaternion)
                    } else {
                        matrixStack.translate(-0.5, 0.0, -0.5)
                    }
                    if (client.cameraEntity != null) {
                        val minY =
                            blockState.getOutlineShape(world, blockPos, ShapeContext.of(client.cameraEntity)).getMin(
                                Axis.Y
                            )
                        matrixStack.translate(0.0, -minY, 0.0)
                    }
                    blockEntity.cachedState = blockState
                    blockEntity.world = world
                    blockEntityRenderDispatcher.renderEntity(
                        blockEntity,
                        matrixStack,
                        vertexConsumerProvider,
                        i,
                        OverlayTexture.DEFAULT_UV
                    )
                    matrixStack.pop()
                }
            }
        }
        super.render(entity, f, g, matrixStack, vertexConsumerProvider, i)
    }

    override fun getTexture(fallingBlockEntity: PushedBlockEntity): Identifier = SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE
}
