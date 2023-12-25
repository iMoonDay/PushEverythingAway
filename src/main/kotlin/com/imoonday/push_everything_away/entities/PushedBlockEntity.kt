package com.imoonday.push_everything_away.entities

import com.imoonday.push_everything_away.api.PushedBlockEvents
import com.imoonday.push_everything_away.init.ModEntities
import com.imoonday.push_everything_away.items.GravityStaffItem
import com.imoonday.push_everything_away.utils.Grabbable
import com.imoonday.push_everything_away.utils.GrabbingManager
import com.mojang.logging.LogUtils
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.enums.ChestType
import net.minecraft.block.enums.DoubleBlockHalf
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.MovementType
import net.minecraft.entity.TntEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.fluid.Fluids
import net.minecraft.item.AutomaticItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtHelper
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.FluidTags
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.max

open class PushedBlockEntity(type: EntityType<*>, world: World) : Entity(type, world) {

    var blockState: BlockState = Blocks.AIR.defaultState
    private var failedTimes = 0
    var blockEntityData: NbtCompound?
        get() = dataTracker.get(DATA)
        set(data) = dataTracker.set(DATA, data)
    var sourcePos: BlockPos
        get() = dataTracker.get(BLOCK_POS)
        set(pos) = dataTracker.set(BLOCK_POS, pos)
    var grabbingOwner: PlayerEntity?
        get() = dataTracker.get(GRABBING_OWNER_UUID).map(world::getPlayerByUuid)
            .orElse(null)
        set(grabbingOwner) = dataTracker.set(
            GRABBING_OWNER_UUID, Optional.ofNullable(
                grabbingOwner?.uuid
            )
        )

    constructor(
        world: World,
        pos: Vec3d,
        blockState: BlockState,
        velocity: Vec3d = Vec3d.ZERO,
    ) : this(ModEntities.PUSHED_BLOCK, world) {
        this.blockState = blockState
        this.setPosition(pos.x, pos.y, pos.z)
        this.velocity = velocity
        prevX = pos.x
        prevY = pos.y
        prevZ = pos.z
    }

    override fun getName(): Text = blockState.block.name
    override fun initDataTracker() {
        dataTracker.startTracking(BLOCK_POS, BlockPos.ORIGIN)
        dataTracker.startTracking(DATA, NbtCompound())
        dataTracker.startTracking(GRABBING_OWNER_UUID, Optional.empty())
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        blockState = NbtHelper.toBlockState(
            world.createCommandRegistryWrapper(RegistryKeys.BLOCK),
            nbt.getCompound("BlockState")
        )
        if (nbt.contains("TileEntityData", NbtElement.COMPOUND_TYPE.toInt())) {
            blockEntityData = nbt.getCompound("TileEntityData")
        }
        if (nbt.contains("sourcePos", NbtElement.INT_ARRAY_TYPE.toInt()) && nbt.getIntArray("sourcePos").size == 3) {
            val sourcePos = nbt.getIntArray("sourcePos")
            this.sourcePos = BlockPos(sourcePos[0], sourcePos[1], sourcePos[2])
        }
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        nbt.put("BlockState", NbtHelper.fromBlockState(blockState))
        if (hasBlockEntityData()) {
            nbt.put("TileEntityData", blockEntityData)
        }
        nbt.putIntArray("sourcePos", intArrayOf(sourcePos.x, sourcePos.y, sourcePos.z))
    }

    fun hasBlockEntityData(): Boolean {
        val data = blockEntityData
        return data != null && !data.isEmpty
    }

    override fun interact(player: PlayerEntity, hand: Hand): ActionResult {
        val isClient = world.isClient
        if (isClient && GrabbingManager.usingForGrabbing) {
            return ActionResult.PASS
        }
        val grabbingOwner = grabbingOwner
        if (grabbingOwner == null) {
            val stack = player.getStackInHand(hand)
            if (stack.item is GravityStaffItem) {
                this.grabbingOwner = player
                if (player is Grabbable) {
                    player.`pushEverythingAway$setGrabbingEntity`(this)
                }
                if (isClient) {
                    GrabbingManager.usingForGrabbing = true
                }
                return ActionResult.SUCCESS
            }
        }
        return super.interact(player, hand)
    }

    override fun tick() {
        PushedBlockEvents.BEFORE_TICK.invoker().beforeTick(this)
        if (this.isRemoved) {
            return
        }
        val world = world
        if (blockState.isAir) {
            discard()
            return
        }
        if (blockState.isOf(Blocks.TNT) && world.isReceivingRedstonePower(blockPos)) {
            discard()
            val tnt = TntEntity(world, pos.getX(), pos.getY(), pos.getZ(), null)
            tnt.fuse = 0
            world.spawnEntity(tnt)
            return
        }

        val grabbingOwner = grabbingOwner
        if (grabbingOwner != null && grabbingOwner is Grabbable) {
            if (!world.isClient && grabbingOwner.`pushEverythingAway$getGrabbingEntity`() !== this) {
                grabbingOwner.`pushEverythingAway$setGrabbingEntity`(null)
            } else if (grabbingOwner.mainHandStack.item is GravityStaffItem || grabbingOwner.offHandStack.item is GravityStaffItem) {
                val grabbingPos =
                    grabbingOwner.eyePos.add(grabbingOwner.rotationVector.multiply((grabbingOwner.`pushEverythingAway$getGrabbingDistance`())))
                        .add(0.0, -boundingBox.lengthY / 2.0, 0.0)
                move(MovementType.SELF, grabbingPos.subtract(pos))
                if (pos != grabbingPos && world.isBlockSpaceEmpty(
                        this,
                        this.boundingBox.offset(grabbingPos.subtract(pos))
                    )
                ) {
                    this.setPosition(grabbingPos)
                }
                return
            } else {
                this.grabbingOwner = null
                grabbingOwner.`pushEverythingAway$setGrabbingEntity`(null)
            }
        } else {
            this.grabbingOwner = null
        }

        val boundingBox = boundingBox
        val entities = world.getOtherEntities(
            this,
            boundingBox
        ) { !it.isRemoved && !it.isSpectator && boundingBox.intersects(it.boundingBox) }
        if (entities.isNotEmpty()) {
            entities.sortWith(compareByDescending {
                boundingBox.intersection(it.boundingBox).volume()
            })
            val entity = entities[0]
            var box = boundingBox.intersection(entity.boundingBox)
            var multiplier =
                box.volume() / boundingBox.volume() / 2
            var offsetVelocity = entity.pos.subtract(pos).normalize().multiply(-multiplier)
            if (offsetVelocity.x == 0.0 && offsetVelocity.z == 0.0) {
                offsetVelocity = rotationVector.multiply(0.2)
            }
            val slowdownMultiplier = max(1 - 0.02 * entities.size, 0.0)
            velocity = velocity.add(offsetVelocity.x, offsetVelocity.y, offsetVelocity.z)
                .multiply(slowdownMultiplier, 0.95, slowdownMultiplier)
            for (other in entities) {
                PushedBlockEvents.ON_ENTITY_COLLISION.invoker().onEntityCollision(this, other)
                if (other is PushedBlockEntity || !other.isPushable) {
                    continue
                }
                val otherBoundingBox = other.boundingBox
                box = otherBoundingBox.intersection(boundingBox)
                multiplier = box.volume() / otherBoundingBox.volume() / 4
                offsetVelocity = pos.subtract(other.pos).normalize().multiply(-multiplier)
                other.addVelocity(offsetVelocity.x, offsetVelocity.y, offsetVelocity.z)
                if (boundingBox.contains(other.eyePos)) {
                    other.damage(this.damageSources.inWall(), 1.0f)
                }
            }
        }
        if (world.isBlockSpaceEmpty(this, boundingBox)) {
            if (!hasNoGravity()) {
                velocity = velocity.add(0.0, -0.04, 0.0)
            }
        } else {
            this.setPosition(
                pos.add(
                    rotationVector.x / 2,
                    rotationVector.y.absoluteValue / 2,
                    rotationVector.z / 2
                )
            )
        }
        if (updateWaterState()) {
            velocity = velocity.multiply(0.98)
        }
        val velocity = velocity
        move(MovementType.SELF, this.velocity)
        if (horizontalCollision || verticalCollision) {
            val velocity1 = this.velocity
            this.velocity = Vec3d(
                if (velocity1.x == 0.0) -velocity.x * 0.95 else velocity1.x,
                if (velocity1.y == 0.0) -velocity.y * 0.75 else velocity1.y,
                if (velocity1.z == 0.0) -velocity.z * 0.95 else velocity1.z
            )
        }
        ProjectileUtil.setRotationFromVelocity(this, 0.5f)
        if (!world.isClient) {
            val blockPos = BlockPos.ofFloored(pos)
            val blockState = world.getBlockState(blockPos)
            if (!blockState.isAir && blockState.getHardness(world, blockPos) == 0f) {
                world.breakBlock(blockPos, true, this)
            }
            if (this.blockY < world.bottomY - 1) {
                if (!tryPlace(blockPos.withY(world.bottomY), world, blockState)) {
                    discard()
                    return
                }
            }
            if ((this.velocity.lengthSquared() < 0.001 && (this.isOnGround || isInsideWall()) || PushedBlockEvents.CAN_TRY_PLACE.invoker()
                    .canTryPlace(this)) && tryPlace(blockPos, world, blockState)
            ) return
        }
        this.velocity = this.velocity.multiply(0.98)
        PushedBlockEvents.AFTER_TICK.invoker().afterTick(this)
    }

    private fun Box.volume() = lengthX * lengthY * lengthZ

    private fun tryPlace(blockPos: BlockPos, world: World, blockState: BlockState): Boolean {
        velocity = velocity.multiply(0.7, -0.5, 0.7)
        val blocks = blockPos.getNearbyBlocks(1 + (failedTimes / 1000).coerceIn(0, 4))
        blocks.addAll(ArrayList(blocks))
        for (i in blocks.indices) {
            var pos = blocks[i]
            val isConcretePowderBlock = this.blockState.block is ConcretePowderBlock
            var isInWater = isConcretePowderBlock && world.getFluidState(pos).isIn(FluidTags.WATER)
            if (isConcretePowderBlock) {
                val blockHitResult = world.raycast(
                    RaycastContext(
                        Vec3d(prevX, prevY, prevZ),
                        this.pos,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.SOURCE_ONLY,
                        this
                    )
                )
                if (blockHitResult.type != HitResult.Type.MISS && world.getFluidState(blockHitResult.blockPos)
                        .isIn(FluidTags.WATER)
                ) {
                    pos = blockHitResult.blockPos
                    isInWater = true
                }
            }
            val canReplace = blockState.canReplace(
                AutomaticItemPlacementContext(
                    world,
                    pos,
                    Direction.DOWN,
                    ItemStack.EMPTY,
                    Direction.UP
                )
            )
            val canFallThrough =
                i < blocks.size / 2 && FallingBlock.canFallThrough(this.world.getBlockState(pos.down())) && (!isConcretePowderBlock || !isInWater)
            var canPlaceAt = this.blockState.canPlaceAt(world, pos) && !canFallThrough
            if (!canPlaceAt && !canFallThrough) {
                if (this.blockState.contains(Properties.DOUBLE_BLOCK_HALF) && this.blockState.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
                    this.blockState = this.blockState.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
                    canPlaceAt = this.blockState.canPlaceAt(world, pos)
                }
            }
            if (canReplace && canPlaceAt) {
                val canPlace = PushedBlockEvents.CAN_PLACE.invoker().canPlace(this, pos)
                if (canPlace && tryPlaceAt(world, pos)) {
                    val blockSoundGroup = this.blockState.soundGroup
                    world.playSound(
                        null,
                        pos,
                        blockSoundGroup.placeSound,
                        SoundCategory.BLOCKS,
                        (blockSoundGroup.getVolume() + 1.0f) / 2.0f,
                        blockSoundGroup.getPitch() * 0.8f
                    )
                    return true
                }
            }
            failedTimes++
        }
        if (failedTimes > 5000) {
            this.setPosition(
                pos.add(
                    (random.nextFloat() - 0.5f).toDouble(),
                    0.0,
                    (random.nextFloat() - 0.5f).toDouble()
                )
            )
            failedTimes -= 1000
        }
        return false
    }

    private fun tryPlaceAt(world: World, blockPos: BlockPos): Boolean {
        var offsetState: BlockState? = null
        if (blockState.contains(Properties.DOUBLE_BLOCK_HALF)) {
            offsetState = if (blockState.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
                world.getBlockState(blockPos.down())
            } else {
                world.getBlockState(blockPos.up())
            }
        } else if (blockState.block is BedBlock) {
            val oppositePartDirection = BedBlock.getOppositePartDirection(blockState)
            offsetState = world.getBlockState(blockPos.offset(oppositePartDirection))
        }
        if (offsetState != null && (!offsetState.isAir && offsetState.fluidState.isEmpty || !offsetState.isReplaceable)) {
            return false
        }
        val state = world.getBlockState(blockPos)
        if (!state.isAir && state.fluidState.isEmpty || !state.isReplaceable) {
            return false
        }
        if (blockState.contains(Properties.WATERLOGGED)) {
            blockState = blockState.with(
                Properties.WATERLOGGED,
                world.getFluidState(blockPos).fluid === Fluids.WATER
            )
        }
        if (blockState.contains(Properties.CHEST_TYPE)) {
            blockState = blockState.with(Properties.CHEST_TYPE, ChestType.SINGLE)
        }
        if (!PushedBlockEvents.BEFORE_PLACE_BLOCK.invoker().beforePlaceBlock(this, blockPos)) {
            return false
        }
        if (world.setBlockState(blockPos, blockState, Block.NOTIFY_ALL)) {
            if (blockState.contains(Properties.DOUBLE_BLOCK_HALF)) {
                world.setBlockState(
                    blockPos.add(
                        0,
                        if (blockState.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) -1 else 1,
                        0
                    ), blockState.cycle(
                        Properties.DOUBLE_BLOCK_HALF
                    ), Block.NOTIFY_ALL
                )
            } else if (blockState.block is BedBlock) {
                val oppositePartDirection = BedBlock.getOppositePartDirection(blockState)
                world.setBlockState(
                    blockPos.offset(oppositePartDirection),
                    blockState.cycle(BedBlock.PART),
                    Block.NOTIFY_ALL
                )
            }
            PushedBlockEvents.AFTER_PLACE_BLOCK.invoker().afterPlaceBlock(this, blockPos)
            val blockEntity: BlockEntity? = world.getBlockEntity(blockPos)
            (world as ServerWorld).chunkManager.threadedAnvilChunkStorage.sendToOtherNearbyPlayers(
                this,
                BlockUpdateS2CPacket(blockPos, world.getBlockState(blockPos))
            )
            discard()
            if (hasBlockEntityData() && blockState.hasBlockEntity() && blockEntity != null
            ) {
                val nbtCompound = blockEntity.createNbt()
                for (string in blockEntityData!!.keys) {
                    nbtCompound.put(string, blockEntityData!![string]!!.copy())
                }
                try {
                    blockEntity.readNbt(nbtCompound)
                } catch (exception: Exception) {
                    LOGGER.error("Failed to load getBlockState() entity from falling getBlockState()", exception)
                }
                blockEntity.markDirty()
                PushedBlockEvents.AFTER_PLACE_BLOCK_ENTITY.invoker().afterPlaceBlockEntity(this, blockEntity, blockPos)
            }
            return true
        }
        return false
    }

    override fun canHit() = true
    override fun damage(source: DamageSource, amount: Float): Boolean {
        source.attacker
            ?.let { velocity = it.rotationVector }
            ?: source.source
                ?.let { velocity = it.rotationVector }
        return super.damage(source, amount)
    }

    override fun calculateDimensions() {
        val d = this.x
        val e = this.y
        val f = this.z
        super.calculateDimensions()
        this.setPosition(d, e, f)
    }

    override fun createSpawnPacket(): Packet<ClientPlayPacketListener> =
        EntitySpawnS2CPacket(this, Block.getRawIdFromState(blockState))

    override fun onSpawnPacket(packet: EntitySpawnS2CPacket) {
        super.onSpawnPacket(packet)
        blockState = Block.getStateFromRawId(packet.entityData)
        intersectionChecked = true
        val d = packet.x
        val e = packet.y
        val f = packet.z
        this.setPosition(d, e, f)
        sourcePos = blockPos
        velocity = Vec3d(packet.velocityX, packet.velocityY, packet.velocityZ)
    }

    override fun calculateBoundingBox(): Box = PushedBlockEvents.BOUNDING_BOX.invoker().onCalculatingBoundingBox(
        this,
        calculateBlockBoundingBox(world, blockPos, blockState, pos, null) ?: super.calculateBoundingBox()
    )

    override fun updateTrackedPositionAndAngles(
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
        interpolationSteps: Int,
    ) = if (grabbingOwner != null) {
        val newPos = Vec3d(x, y, z)
        val oldPos = pos
        this.setPosition(
            when {
                oldPos.distanceTo(newPos) < 3.0 -> oldPos
                velocity.lengthSquared() == 0.0 -> newPos
                else -> oldPos.lerp(
                    newPos,
                    0.5
                )
            }
        )
        this.setRotation(yaw, pitch)
    } else super.updateTrackedPositionAndAngles(x, y, z, yaw, pitch, interpolationSteps)

    companion object {
        private val LOGGER = LogUtils.getLogger()
        protected val BLOCK_POS: TrackedData<BlockPos> =
            DataTracker.registerData(PushedBlockEntity::class.java, TrackedDataHandlerRegistry.BLOCK_POS)
        protected val DATA: TrackedData<NbtCompound> =
            DataTracker.registerData(PushedBlockEntity::class.java, TrackedDataHandlerRegistry.NBT_COMPOUND)
        protected val GRABBING_OWNER_UUID: TrackedData<Optional<UUID>> = DataTracker.registerData(
            PushedBlockEntity::class.java, TrackedDataHandlerRegistry.OPTIONAL_UUID
        )

        fun createEntity(
            world: World,
            pos: BlockPos,
            velocity: (entity: PushedBlockEntity) -> Vec3d = { Vec3d.ZERO },
        ): PushedBlockEntity {
            val state = world.getBlockState(pos)
            var spawnPos = Vec3d.ofBottomCenter(pos)
            val shape = state.getOutlineShape(world, pos)
            if (!shape.isEmpty) {
                val boundingBox = shape.getBoundingBox()
                val center = boundingBox.center
                spawnPos = Vec3d(center.x + pos.x, boundingBox.minY + pos.y, center.z + pos.z)
            }
            val pushedBlockEntity = PushedBlockEntity(
                world = world,
                pos = spawnPos,
                blockState = state,
            ).apply { this.velocity = velocity.invoke(this) }
            val offset: BlockPos
            val directionOffset: Direction
            val offsetState: BlockState
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity != null) {
                pushedBlockEntity.blockEntityData = blockEntity.createNbtWithIdentifyingData()
                world.removeBlockEntity(pos)
            }
            if (state.block is BedBlock) {
                val type = BedBlock.getBedPart(state)
                pushedBlockEntity.setPosition(
                    pushedBlockEntity.pos.offset(
                        state.get(Properties.HORIZONTAL_FACING),
                        if (type == DoubleBlockProperties.Type.FIRST) -0.5 else 0.5
                    )
                )
                directionOffset = BedBlock.getOppositePartDirection(state)
                when (type) {
                    DoubleBlockProperties.Type.FIRST -> {
                        world.removeBlock(pos, false)
                        offset = pos.offset(directionOffset)
                        offsetState = world.getBlockState(offset)
                        if (offsetState.block is BedBlock && BedBlock.getBedPart(offsetState) == DoubleBlockProperties.Type.SECOND) {
                            world.removeBlock(offset, false)
                        }
                    }

                    DoubleBlockProperties.Type.SECOND -> {
                        offset = pos.offset(directionOffset)
                        offsetState = world.getBlockState(offset)
                        if (offsetState.block is BedBlock && BedBlock.getBedPart(offsetState) == DoubleBlockProperties.Type.FIRST) {
                            world.removeBlock(offset, false)
                        }
                        world.removeBlock(pos, false)
                    }

                    else -> world.removeBlock(pos, false)
                }
            } else if (state.contains(Properties.DOUBLE_BLOCK_HALF)) {
                val half = state.get(Properties.DOUBLE_BLOCK_HALF)
                if (half == DoubleBlockHalf.UPPER) {
                    pushedBlockEntity.setPosition(pushedBlockEntity.pos.add(0.0, -1.0, 0.0))
                    directionOffset = Direction.DOWN
                    offset = pos.offset(directionOffset)
                    offsetState = world.getBlockState(offset)
                    if (offsetState.isOf(state.block) && offsetState.contains(Properties.DOUBLE_BLOCK_HALF) && offsetState.get(
                            Properties.DOUBLE_BLOCK_HALF
                        ) == DoubleBlockHalf.LOWER
                    ) {
                        world.removeBlock(offset, false)
                    }
                    world.removeBlock(pos, false)
                } else {
                    directionOffset = Direction.UP
                    offset = pos.offset(directionOffset)
                    offsetState = world.getBlockState(offset)
                    if (offsetState.isOf(state.block) && offsetState.contains(Properties.DOUBLE_BLOCK_HALF) && offsetState.get(
                            Properties.DOUBLE_BLOCK_HALF
                        ) == DoubleBlockHalf.UPPER
                    ) {
                        world.removeBlock(pos, false)
                        world.removeBlock(offset, false)
                    } else {
                        world.removeBlock(pos, false)
                    }
                }
            } else {
                world.removeBlock(pos, false)
            }
            return pushedBlockEntity
        }

        private fun BlockPos.getNearbyBlocks(range: Int): MutableList<BlockPos> {
            val blocks: MutableList<BlockPos> = ArrayList()
            for (x in -range..range) {
                for (y in -1..1) {
                    for (z in -range..range) {
                        blocks.add(add(x, y, z))
                    }
                }
            }
            blocks.sortWith(compareBy { pos: BlockPos -> pos.getSquaredDistance(this) }
                .thenComparing { pos: BlockPos -> pos.y })
            return blocks
        }

        fun calculateBlockBoundingBox(
            world: World,
            blockPos: BlockPos,
            blockState: BlockState?,
            pos: Vec3d,
            entity: Entity?,
        ): Box? {
            val shape = blockState?.getOutlineShape(
                world,
                blockPos,
                if (entity != null) ShapeContext.of(entity) else ShapeContext.absent()
            )
            if (shape?.isEmpty != false) {
                return null
            }
            val boundingBox = shape.getBoundingBox()
            var widthX = boundingBox.lengthX / 2.0
            var height = boundingBox.lengthY
            var widthZ = boundingBox.lengthZ / 2.0
            var x = pos.x
            val y = pos.y
            var z = pos.z
            val block = blockState.block
            if (blockState.contains(Properties.DOUBLE_BLOCK_HALF)) {
                height *= 2.0
            } else if (block is BedBlock) {
                val direction = BedBlock.getOppositePartDirection(blockState)
                val offsetX = direction.offsetX / 2.0
                val offsetZ = direction.offsetZ / 2.0
                if (offsetX > 0) {
                    widthX += offsetX
                } else {
                    widthX -= offsetX
                }
                if (offsetZ > 0) {
                    widthZ += offsetZ
                } else {
                    widthZ -= offsetZ
                }
            }
            if (block is DoorBlock || block is WallBannerBlock || block is TrapdoorBlock && blockState.get(
                    Properties.OPEN
                )
            ) {
                val vector = blockState.get(Properties.HORIZONTAL_FACING).vector
                val offset = 0.41
                x -= vector.x * offset
                z -= vector.z * offset
            }
            return Box(x - widthX, y, z - widthZ, x + widthX, y + height, z + widthZ)
        }
    }
}
