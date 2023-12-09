package com.imoonday.push_everything_away.entities;

import com.imoonday.push_everything_away.PushEverythingAway;
import com.imoonday.push_everything_away.api.PushedBlockEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PushedBlockEntity extends Entity {

    private static final Logger LOGGER = LogUtils.getLogger();
    protected static final TrackedData<BlockPos> BLOCK_POS = DataTracker.registerData(PushedBlockEntity.class, TrackedDataHandlerRegistry.BLOCK_POS);
    protected static final TrackedData<NbtCompound> DATA = DataTracker.registerData(PushedBlockEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);
    private BlockState block = Blocks.GRASS_BLOCK.getDefaultState();
    private int failedTimes = 0;

    public PushedBlockEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public PushedBlockEntity(World world, Vec3d pos) {
        super(PushEverythingAway.PUSHED_BLOCK, world);
        this.setPosition(pos.x, pos.y, pos.z);
        this.setVelocity(Vec3d.ZERO);
        this.prevX = pos.x;
        this.prevY = pos.y;
        this.prevZ = pos.z;
    }

    public static PushedBlockEntity createPushedBlockEntity(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Vec3d spawnPos = Vec3d.ofBottomCenter(pos);
        VoxelShape shape = state.getOutlineShape(world, pos);
        if (!shape.isEmpty()) {
            Box boundingBox = shape.getBoundingBox();
            Vec3d center = boundingBox.getCenter();
            spawnPos = new Vec3d(center.x + pos.getX(), boundingBox.minY + pos.getY(), center.z + pos.getZ());
        }
        PushedBlockEntity pushedBlockEntity = new PushedBlockEntity(world, spawnPos);
        BlockPos offset;
        Direction directionOffset;
        BlockState offsetState;
        pushedBlockEntity.setBlockState(state);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            pushedBlockEntity.setBlockEntityData(blockEntity.createNbtWithIdentifyingData());
            world.removeBlockEntity(pos);
        }
        if (state.getBlock() instanceof BedBlock) {
            DoubleBlockProperties.Type type = BedBlock.getBedPart(state);
            pushedBlockEntity.setPosition(pushedBlockEntity.getPos().offset(state.get(Properties.HORIZONTAL_FACING), type == DoubleBlockProperties.Type.FIRST ? -0.5 : 0.5));
            directionOffset = BedBlock.getOppositePartDirection(state);
            switch (type) {
                case FIRST -> {
                    world.removeBlock(pos, false);
                    offset = pos.offset(directionOffset);
                    offsetState = world.getBlockState(offset);
                    if (offsetState.getBlock() instanceof BedBlock && BedBlock.getBedPart(offsetState) == DoubleBlockProperties.Type.SECOND) {
                        world.removeBlock(offset, false);
                    }
                }
                case SECOND -> {
                    offset = pos.offset(directionOffset);
                    offsetState = world.getBlockState(offset);
                    if (offsetState.getBlock() instanceof BedBlock && BedBlock.getBedPart(offsetState) == DoubleBlockProperties.Type.FIRST) {
                        world.removeBlock(offset, false);
                    }
                    world.removeBlock(pos, false);
                }
                default -> world.removeBlock(pos, false);
            }
        } else if (state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.get(Properties.DOUBLE_BLOCK_HALF);
            if (half == DoubleBlockHalf.UPPER) {
                pushedBlockEntity.setPosition(pushedBlockEntity.getPos().add(0, -1, 0));
                directionOffset = Direction.DOWN;
                offset = pos.offset(directionOffset);
                offsetState = world.getBlockState(offset);
                if (offsetState.isOf(state.getBlock()) && offsetState.contains(Properties.DOUBLE_BLOCK_HALF) && offsetState.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
                    world.removeBlock(offset, false);
                }
                world.removeBlock(pos, false);
            } else {
                directionOffset = Direction.UP;
                offset = pos.offset(directionOffset);
                offsetState = world.getBlockState(offset);
                if (offsetState.isOf(state.getBlock()) && offsetState.contains(Properties.DOUBLE_BLOCK_HALF) && offsetState.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
                    world.removeBlock(pos, false);
                    world.removeBlock(offset, false);
                } else {
                    world.removeBlock(pos, false);
                }
            }
        } else {
            world.removeBlock(pos, false);
        }
        return pushedBlockEntity;
    }

    public void setBlockState(BlockState state) {
        this.block = state;
    }

    public BlockState getBlockState() {
        return block;
    }

    @Override
    public Text getName() {
        return this.getBlockState().getBlock().getName();
    }

    public NbtCompound getBlockEntityData() {
        return this.dataTracker.get(DATA);
    }

    public void setBlockEntityData(NbtCompound data) {
        this.dataTracker.set(DATA, data);
    }

    public BlockPos getSourcePos() {
        return this.dataTracker.get(BLOCK_POS);
    }

    public void setSourcePos(BlockPos pos) {
        this.dataTracker.set(BLOCK_POS, pos);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(BLOCK_POS, BlockPos.ORIGIN);
        this.dataTracker.startTracking(DATA, new NbtCompound());
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.setBlockState(NbtHelper.toBlockState(this.getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt.getCompound("BlockState")));
        if (nbt.contains("TileEntityData", NbtElement.COMPOUND_TYPE)) {
            this.setBlockEntityData(nbt.getCompound("TileEntityData"));
        }
        if (nbt.contains("sourcePos", NbtElement.INT_ARRAY_TYPE) && nbt.getIntArray("sourcePos").length == 3) {
            int[] sourcePos = nbt.getIntArray("sourcePos");
            this.setSourcePos(new BlockPos(sourcePos[0], sourcePos[1], sourcePos[2]));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.put("BlockState", NbtHelper.fromBlockState(this.getBlockState()));
        if (this.hasBlockEntityData()) {
            nbt.put("TileEntityData", this.getBlockEntityData());
        }
        nbt.putIntArray("sourcePos", new int[]{this.getSourcePos().getX(), this.getSourcePos().getY(), this.getSourcePos().getZ()});
    }

    public boolean hasBlockEntityData() {
        NbtCompound data = this.getBlockEntityData();
        return data != null && !data.isEmpty();
    }

    @Override
    public void tick() {
        PushedBlockEvents.BEFORE_TICK.invoker().beforeTick(this);
        if (this.isRemoved()) {
            return;
        }
        World world = this.getWorld();

        if (this.getBlockState().isAir()) {
            this.discard();
            return;
        }

        if (this.getBlockState().isOf(Blocks.TNT) && world.isReceivingRedstonePower(this.getBlockPos())) {
            this.discard();
            TntEntity tnt = new TntEntity(world, this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), null);
            tnt.setFuse(0);
            world.spawnEntity(tnt);
            return;
        }

        Box boundingBox = this.getBoundingBox();
        List<Entity> entities = world.getOtherEntities(this, boundingBox, entity -> !entity.isRemoved() && !entity.isSpectator() && boundingBox.intersects(entity.getBoundingBox()));
        if (!entities.isEmpty()) {
            entities.sort(Comparator.<Entity>comparingDouble(entity -> {
                Box box = boundingBox.intersection(entity.getBoundingBox());
                return box.getLengthX() * box.getLengthY() * box.getLengthZ();
            }).reversed());
            Entity entity = entities.get(0);
            Box box = boundingBox.intersection(entity.getBoundingBox());
            double multiplier = (box.getLengthX() * box.getLengthY() * box.getLengthZ()) / (boundingBox.getLengthX() * boundingBox.getLengthY() * boundingBox.getLengthZ()) / 2;
            Vec3d offsetVelocity = entity.getPos().subtract(this.getPos()).normalize().multiply(-multiplier);
            if (offsetVelocity.x == 0 && offsetVelocity.z == 0) {
                offsetVelocity = offsetVelocity.addRandom(random, 0.2f);
            }
            double slowdownMultiplier = Math.max(1 - 0.02 * entities.size(), 0);
            this.setVelocity(this.getVelocity().add(offsetVelocity.x, offsetVelocity.y, offsetVelocity.z).multiply(slowdownMultiplier, 0.95, slowdownMultiplier));
            for (int i = 1; i < entities.size(); i++) {
                Entity other = entities.get(i);
                PushedBlockEvents.ON_ENTITY_COLLISION.invoker().onEntityCollision(this, other);
                if (other instanceof PushedBlockEntity) {
                    continue;
                }
                Box otherBoundingBox = other.getBoundingBox();
                box = otherBoundingBox.intersection(boundingBox);
                multiplier = (box.getLengthX() * box.getLengthY() * box.getLengthZ()) / (otherBoundingBox.getLengthX() * otherBoundingBox.getLengthY() * otherBoundingBox.getLengthZ()) / 4;
                offsetVelocity = this.getPos().subtract(other.getPos()).normalize().multiply(-multiplier);
                other.addVelocity(offsetVelocity.x, offsetVelocity.y, offsetVelocity.z);
                if (boundingBox.contains(other.getEyePos())) {
                    other.damage(this.getDamageSources().inWall(), 1.0f);
                }
            }
        }

        if (world.isBlockSpaceEmpty(this, boundingBox)) {
            if (!this.hasNoGravity()) {
                this.setVelocity(this.getVelocity().add(0.0, -0.04, 0.0));
            }
        } else {
            this.setPosition(this.getPos().add(this.random.nextFloat() - 0.5f, this.random.nextFloat() / 2, this.random.nextFloat() - 0.5f));
        }

        if (this.updateWaterState()) {
            this.setVelocity(this.getVelocity().multiply(0.98));
        }

        Vec3d velocity = this.getVelocity();
        this.move(MovementType.SELF, this.getVelocity());
        if (horizontalCollision || verticalCollision) {
            Vec3d velocity1 = this.getVelocity();
            this.setVelocity(new Vec3d(velocity1.x == 0 ? -velocity.x * 0.95 : velocity1.x, velocity1.y == 0 ? -velocity.y * 0.75 : velocity1.y, velocity1.z == 0 ? -velocity.z * 0.95 : velocity1.z));
        }

        velocity = this.getVelocity();
        double d = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        this.setYaw((float) MathHelper.lerp(0.5, this.getYaw(), -Math.toDegrees(MathHelper.atan2(velocity.x, velocity.z))));
        this.setPitch((float) MathHelper.lerp(0.5, this.getPitch(), -Math.toDegrees(MathHelper.atan2(velocity.y, d))));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();

        if (!world.isClient) {
            BlockPos blockPos = BlockPos.ofFloored(this.getPos());
            BlockState blockState = world.getBlockState(blockPos);
            if (!blockState.isAir() && blockState.getHardness(world, blockPos) == 0) {
                world.breakBlock(blockPos, true, this);
            }
            if (this.getBlockY() < world.getBottomY() - 1) {
                if (!tryPlace(blockPos.withY(world.getBottomY()), world, blockState)) {
                    this.discard();
                    return;
                }
            }
            if ((this.getVelocity().lengthSquared() < 0.001 && (this.isOnGround() || this.isInsideWall()) || PushedBlockEvents.CAN_TRY_PLACE.invoker().canTryPlace(this))) {
                if (tryPlace(blockPos, world, blockState)) return;
            }
        }
        this.setVelocity(this.getVelocity().multiply(0.98));
        PushedBlockEvents.AFTER_TICK.invoker().afterTick(this);
    }

    protected boolean tryPlace(BlockPos blockPos, World world, BlockState blockState) {
        this.setVelocity(this.getVelocity().multiply(0.7, -0.5, 0.7));
        List<BlockPos> blocks = getNearbyBlocks(blockPos, 1 + MathHelper.clamp(failedTimes / 1000, 0, 4));
        blocks.addAll(new ArrayList<>(blocks));
        for (int i = 0; i < blocks.size(); i++) {
            BlockPos pos = blocks.get(i);
            BlockHitResult blockHitResult;
            boolean isConcretePowderBlock = this.getBlockState().getBlock() instanceof ConcretePowderBlock;
            boolean isInWater = isConcretePowderBlock && world.getFluidState(pos).isIn(FluidTags.WATER);
            if (isConcretePowderBlock && (blockHitResult = world.raycast(new RaycastContext(new Vec3d(this.prevX, this.prevY, this.prevZ), this.getPos(), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.SOURCE_ONLY, this))).getType() != HitResult.Type.MISS && world.getFluidState(blockHitResult.getBlockPos()).isIn(FluidTags.WATER)) {
                pos = blockHitResult.getBlockPos();
                isInWater = true;
            }
            boolean canReplace = blockState.canReplace(new AutomaticItemPlacementContext(world, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
            boolean canFallThrough = i < blocks.size() / 2 && FallingBlock.canFallThrough(this.getWorld().getBlockState(pos.down())) && (!isConcretePowderBlock || !isInWater);
            boolean canPlaceAt = this.getBlockState().canPlaceAt(world, pos) && !canFallThrough;
            if (!canPlaceAt && !canFallThrough) {
                if (this.getBlockState().contains(Properties.DOUBLE_BLOCK_HALF) && this.getBlockState().get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
                    this.setBlockState(this.getBlockState().with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
                    canPlaceAt = this.getBlockState().canPlaceAt(world, pos);
                }
            }
            if (canReplace && canPlaceAt) {
                boolean canPlace = PushedBlockEvents.CAN_PLACE.invoker().canPlace(this, pos);
                if (canPlace && tryPlaceAt(world, pos)) {
                    BlockSoundGroup blockSoundGroup = this.getBlockState().getSoundGroup();
                    world.playSound(null, pos, blockSoundGroup.getPlaceSound(), SoundCategory.BLOCKS, (blockSoundGroup.getVolume() + 1.0f) / 2.0f, blockSoundGroup.getPitch() * 0.8f);
                    return true;
                }
            }
            failedTimes++;
        }
        if (failedTimes > 5000) {
            this.setPosition(this.getPos().add(this.random.nextFloat() - 0.5f, 0, this.random.nextFloat() - 0.5f));
            failedTimes -= 1000;
        }
        return false;
    }

    @NotNull
    private static List<BlockPos> getNearbyBlocks(BlockPos center, int range) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int x = -range; x <= range; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -range; z <= range; z++) {
                    blocks.add(center.add(x, y, z));
                }
            }
        }
        blocks.sort(Comparator.<BlockPos>comparingDouble(pos -> pos.getSquaredDistance(center)).thenComparing(Vec3i::getY));
        return blocks;
    }

    protected boolean tryPlaceAt(World world, BlockPos blockPos) {
        BlockState offsetState = null;
        if (this.getBlockState().contains(Properties.DOUBLE_BLOCK_HALF)) {
            if (this.getBlockState().get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
                offsetState = world.getBlockState(blockPos.down());
            } else {
                offsetState = world.getBlockState(blockPos.up());
            }

        } else if (this.getBlockState().getBlock() instanceof BedBlock) {
            Direction oppositePartDirection = BedBlock.getOppositePartDirection(this.getBlockState());
            offsetState = world.getBlockState(blockPos.offset(oppositePartDirection));
        }
        if (offsetState != null && (!offsetState.isAir() && offsetState.getFluidState().isEmpty() || !offsetState.isReplaceable())) {
            return false;
        }
        BlockState state = world.getBlockState(blockPos);
        if (!state.isAir() && state.getFluidState().isEmpty() || !state.isReplaceable()) {
            return false;
        }
        if (this.getBlockState().contains(Properties.WATERLOGGED)) {
            this.setBlockState(world.getFluidState(blockPos).getFluid() == Fluids.WATER ? this.getBlockState().with(Properties.WATERLOGGED, true) : this.getBlockState().with(Properties.WATERLOGGED, false));
        }
        if (this.getBlockState().contains(Properties.CHEST_TYPE)) {
            this.setBlockState(this.getBlockState().with(Properties.CHEST_TYPE, ChestType.SINGLE));
        }
        if (!PushedBlockEvents.BEFORE_PLACE_BLOCK.invoker().beforePlaceBlock(this, blockPos)) {
            return false;
        }
        if (world.setBlockState(blockPos, this.getBlockState(), Block.NOTIFY_ALL)) {
            if (this.getBlockState().contains(Properties.DOUBLE_BLOCK_HALF)) {
                world.setBlockState(blockPos.add(0, this.getBlockState().get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? -1 : 1, 0), this.getBlockState().cycle(Properties.DOUBLE_BLOCK_HALF), Block.NOTIFY_ALL);
            } else if (this.getBlockState().getBlock() instanceof BedBlock) {
                Direction oppositePartDirection = BedBlock.getOppositePartDirection(this.getBlockState());
                world.setBlockState(blockPos.offset(oppositePartDirection), this.getBlockState().cycle(BedBlock.PART), Block.NOTIFY_ALL);
            }
            PushedBlockEvents.AFTER_PLACE_BLOCK.invoker().afterPlaceBlock(this, blockPos);
            BlockEntity blockEntity;
            ((ServerWorld) world).getChunkManager().threadedAnvilChunkStorage.sendToOtherNearbyPlayers(this, new BlockUpdateS2CPacket(blockPos, world.getBlockState(blockPos)));
            this.discard();
            if (this.hasBlockEntityData() && this.getBlockState().hasBlockEntity() && (blockEntity = world.getBlockEntity(blockPos)) != null) {
                NbtCompound nbtCompound = blockEntity.createNbt();
                for (String string : this.getBlockEntityData().getKeys()) {
                    nbtCompound.put(string, this.getBlockEntityData().get(string).copy());
                }
                try {
                    blockEntity.readNbt(nbtCompound);
                } catch (Exception exception) {
                    LOGGER.error("Failed to load getBlockState() entity from falling getBlockState()", exception);
                }
                blockEntity.markDirty();
                PushedBlockEvents.AFTER_PLACE_BLOCK_ENTITY.invoker().afterPlaceBlockEntity(this, blockEntity, blockPos);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        Entity entity = source.getAttacker();
        if (entity == null) {
            entity = source.getSource();
        }
        if (entity != null) {
            this.setVelocity(entity.getRotationVector());
        }
        return super.damage(source, amount);
    }

    @Override
    public boolean collidesWith(Entity other) {
        return super.collidesWith(other);
    }

    @Override
    public void calculateDimensions() {
        double d = this.getX();
        double e = this.getY();
        double f = this.getZ();
        super.calculateDimensions();
        this.setPosition(d, e, f);
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, Block.getRawIdFromState(this.getBlockState()));
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        this.block = Block.getStateFromRawId(packet.getEntityData());
        this.intersectionChecked = true;
        double d = packet.getX();
        double e = packet.getY();
        double f = packet.getZ();
        this.setPosition(d, e, f);
        this.setSourcePos(this.getBlockPos());
    }

    @Override
    protected Box calculateBoundingBox() {
        Box box = super.calculateBoundingBox();
        if (this.block != null) {
            Box blockBoundingBox = calculateBlockBoundingBox(this.getWorld(), this.getBlockPos(), this.block, this.getPos(), null);
            if (blockBoundingBox != null) {
                box = blockBoundingBox;
            }
        }
        return PushedBlockEvents.BOUNDING_BOX.invoker().onCalculatingBoundingBox(this, box);
    }

    public static Box calculateBlockBoundingBox(World world, BlockPos blockPos, BlockState blockState, Vec3d pos, @Nullable Entity entity) {
        VoxelShape shape = blockState.getOutlineShape(world, blockPos, entity != null ? ShapeContext.of(entity) : ShapeContext.absent());
        if (!shape.isEmpty()) {
            Box boundingBox = shape.getBoundingBox();
            double widthX = boundingBox.getLengthX() / 2.0;
            double height = boundingBox.getLengthY();
            double widthZ = boundingBox.getLengthZ() / 2.0;
            double x = pos.x;
            double y = pos.y;
            double z = pos.z;
            Block block = blockState.getBlock();
            if (blockState.contains(Properties.DOUBLE_BLOCK_HALF)) {
                height *= 2;
            } else if (block instanceof BedBlock) {
                Direction direction = BedBlock.getOppositePartDirection(blockState);
                double offsetX = direction.getOffsetX() / 2.0;
                double offsetZ = direction.getOffsetZ() / 2.0;
                if (offsetX > 0) {
                    widthX += offsetX;
                } else {
                    widthX -= offsetX;
                }
                if (offsetZ > 0) {
                    widthZ += offsetZ;
                } else {
                    widthZ -= offsetZ;
                }
            }
            if (block instanceof DoorBlock || block instanceof WallBannerBlock || block instanceof TrapdoorBlock && blockState.get(Properties.OPEN)) {
                Vec3i vector = blockState.get(Properties.HORIZONTAL_FACING).getVector();
                double offset = 0.41;
                x -= vector.getX() * offset;
                z -= vector.getZ() * offset;
            }
            return new Box(x - widthX, y, z - widthZ, x + widthX, y + height, z + widthZ);
        }
        return null;
    }
}
