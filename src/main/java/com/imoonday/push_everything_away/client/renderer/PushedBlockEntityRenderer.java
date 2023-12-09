package com.imoonday.push_everything_away.client.renderer;

import com.imoonday.push_everything_away.api.PushedBlockRenderEvents;
import com.imoonday.push_everything_away.entities.PushedBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class PushedBlockEntityRenderer extends EntityRenderer<PushedBlockEntity> {

    private final BlockRenderManager blockRenderManager;

    public PushedBlockEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
        this.blockRenderManager = context.getBlockRenderManager();
    }

    @Override
    public void render(PushedBlockEntity entity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if (PushedBlockRenderEvents.EVENT.invoker().render(entity, f, g, matrixStack, vertexConsumerProvider, i)) {
            super.render(entity, f, g, matrixStack, vertexConsumerProvider, i);
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        BlockState blockState = entity.getBlockState();
        BlockRenderType renderType = blockState.getRenderType();
        Block block = blockState.getBlock();
        World world = entity.getWorld();
        if (renderType == BlockRenderType.MODEL) {
            matrixStack.push();
            BlockPos blockPos = BlockPos.ofFloored(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
            matrixStack.translate(-0.5, 0.0, -0.5);
            VoxelShape shape = blockState.getOutlineShape(world, blockPos, ShapeContext.of(client.cameraEntity));
            double offsetY = -shape.getMin(Direction.Axis.Y);
            if (block instanceof WallTorchBlock || block instanceof WallRedstoneTorchBlock) {
                Vec3i vector = blockState.get(Properties.HORIZONTAL_FACING).getVector();
                matrixStack.translate(0.35 * vector.getX(), offsetY, 0.35 * vector.getZ());
            } else {
                matrixStack.translate(0, offsetY, 0);
            }
            BlockPos sourcePos = entity.getSourcePos();
            if (blockState.contains(Properties.DOUBLE_BLOCK_HALF)) {
                if (blockState.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
                    this.blockRenderManager.getModelRenderer().render(world, this.blockRenderManager.getModel(blockState), blockState, blockPos, matrixStack, vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(blockState)), false, Random.create(), blockState.getRenderingSeed(sourcePos), OverlayTexture.DEFAULT_UV);
                    BlockState offsetState = blockState.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                    matrixStack.translate(0, 1, 0);
                    this.blockRenderManager.getModelRenderer().render(world, this.blockRenderManager.getModel(offsetState), offsetState, blockPos.up(), matrixStack, vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(offsetState)), false, Random.create(), offsetState.getRenderingSeed(sourcePos.up()), OverlayTexture.DEFAULT_UV);
                } else {
                    matrixStack.translate(0, 1, 0);
                    this.blockRenderManager.getModelRenderer().render(world, this.blockRenderManager.getModel(blockState), blockState, blockPos.up(), matrixStack, vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(blockState)), false, Random.create(), blockState.getRenderingSeed(sourcePos), OverlayTexture.DEFAULT_UV);
                    BlockState offsetState = blockState.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
                    matrixStack.translate(0, -1, 0);
                    this.blockRenderManager.getModelRenderer().render(world, this.blockRenderManager.getModel(offsetState), offsetState, blockPos, matrixStack, vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(offsetState)), false, Random.create(), offsetState.getRenderingSeed(sourcePos.down()), OverlayTexture.DEFAULT_UV);
                }
            } else if (block instanceof BedBlock) {
                Direction direction = BedBlock.getOppositePartDirection(blockState);
                Vec3i vector = direction.getOpposite().getVector();
                matrixStack.translate(vector.getX() * 0.5, 0.0, vector.getZ() * 0.5);
                this.blockRenderManager.getModelRenderer().render(world, this.blockRenderManager.getModel(blockState), blockState, blockPos, matrixStack, vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(blockState)), false, Random.create(), blockState.getRenderingSeed(sourcePos), OverlayTexture.DEFAULT_UV);
                vector = direction.getVector();
                matrixStack.translate(vector.getX(), 0.0, vector.getZ());
                blockState = blockState.cycle(BedBlock.PART);
                this.blockRenderManager.getModelRenderer().render(world, this.blockRenderManager.getModel(blockState), blockState, blockPos, matrixStack, vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(blockState)), false, Random.create(), blockState.getRenderingSeed(sourcePos), OverlayTexture.DEFAULT_UV);
            } else {
                this.blockRenderManager.getModelRenderer().render(world, this.blockRenderManager.getModel(blockState), blockState, blockPos, matrixStack, vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(blockState)), false, Random.create(), blockState.getRenderingSeed(sourcePos), OverlayTexture.DEFAULT_UV);
            }
            matrixStack.pop();
        }
        if (entity.hasBlockEntityData()) {
            BlockPos blockPos = BlockPos.ofFloored(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
            BlockEntity blockEntity = BlockEntity.createFromNbt(blockPos, blockState, entity.getBlockEntityData());
            if (blockEntity != null) {
                BlockEntityRenderDispatcher blockEntityRenderDispatcher = client.getBlockEntityRenderDispatcher();
                if (blockEntityRenderDispatcher.get(blockEntity) != null) {
                    matrixStack.push();
                    boolean isBed = block instanceof BedBlock;
                    if (block instanceof BannerBlock) {
                        float h = -RotationPropertyHelper.toDegrees(blockState.get(BannerBlock.ROTATION));
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(h));
                        matrixStack.translate(-0.5, 0, -0.5);
                    } else if (block instanceof WallSkullBlock) {
                        Direction direction = blockState.get(Properties.HORIZONTAL_FACING);
                        Vec3i vector = direction.getVector();
                        matrixStack.translate(-0.5 + vector.getX() / 4.0, 0, -0.5 + vector.getZ() / 4.0);
                    } else if (blockState.contains(Properties.HORIZONTAL_FACING) && !(block instanceof WallSignBlock || block instanceof WallHangingSignBlock)) {
                        Direction direction = blockState.get(Properties.HORIZONTAL_FACING);
                        switch (direction) {
                            case WEST -> {
                                matrixStack.translate(isBed ? 0 : 0.5, 0.0, -0.5);
                                matrixStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(90));
                            }
                            case EAST -> {
                                matrixStack.translate(isBed ? 0 : -0.5, 0.0, 0.5);
                                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
                            }
                            case NORTH -> {
                                matrixStack.translate(0.5, 0.0, isBed ? 0 : 0.5);
                                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                            }
                            case SOUTH -> matrixStack.translate(-0.5, 0.0, isBed ? 0 : -0.5);
                        }
                        if (block instanceof WallBannerBlock) {
                            matrixStack.translate(0, -1, -0.5);
                        }
                    } else if (blockState.contains(Properties.FACING)) {
                        Direction direction = blockState.get(Properties.FACING);
                        double offsetX = -direction.getOffsetX() / 2.0;
                        double offsetY = direction.getOffsetY() != 0 ? 0.0 : 1.0;
                        double offsetZ = -direction.getOffsetZ() / 2.0;
                        matrixStack.translate(offsetX, offsetY, offsetZ);
                        if (direction.getAxis() != Direction.Axis.Y) {
                            Direction rotated = direction.rotateYClockwise();
                            matrixStack.translate(rotated.getOffsetX() / 2.0, 0.0, rotated.getOffsetZ() / 2.0);
                        } else {
                            matrixStack.translate(-0.5, direction == Direction.UP ? 0 : 1, direction == Direction.UP ? -0.5 : 0.5);
                        }
                        matrixStack.multiply(direction.getRotationQuaternion());
                    } else {
                        matrixStack.translate(-0.5, 0, -0.5);
                    }
                    if (client.cameraEntity != null) {
                        double minY = blockState.getOutlineShape(world, blockPos, ShapeContext.of(client.cameraEntity)).getMin(Direction.Axis.Y);
                        matrixStack.translate(0, -minY, 0);
                    }
                    blockEntity.setCachedState(blockState);
                    blockEntity.setWorld(world);
                    blockEntityRenderDispatcher.renderEntity(blockEntity, matrixStack, vertexConsumerProvider, i, OverlayTexture.DEFAULT_UV);
                    matrixStack.pop();
                }
            }
        }
        super.render(entity, f, g, matrixStack, vertexConsumerProvider, i);
    }

    @Override
    public Identifier getTexture(PushedBlockEntity fallingBlockEntity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
