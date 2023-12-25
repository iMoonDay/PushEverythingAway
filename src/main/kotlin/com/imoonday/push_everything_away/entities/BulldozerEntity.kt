package com.imoonday.push_everything_away.entities

import com.imoonday.push_everything_away.init.ModEntities
import com.imoonday.push_everything_away.init.ModItems
import com.imoonday.push_everything_away.utils.Pushable
import net.minecraft.entity.*
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageTypes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameRules
import net.minecraft.world.World
import org.joml.Vector3f

open class BulldozerEntity(type: EntityType<*>, world: World) : Entity(type, world) {
    constructor(world: World, pos: Vec3d) : this(ModEntities.BULLDOZER, world) {
        this.setPosition(pos)
        prevX = pos.x
        prevY = pos.y
        prevZ = pos.z
    }

    override fun initDataTracker() {}
    override fun readCustomDataFromNbt(nbt: NbtCompound) {}
    override fun writeCustomDataToNbt(nbt: NbtCompound) {}
    override fun damage(source: DamageSource, amount: Float): Boolean {
        if (!source.isOf(DamageTypes.PLAYER_ATTACK)) {
            return false
        }
        if (hasPassengers()) {
            return false
        }
        if (world.isClient || this.isRemoved) {
            return true
        }
        kill()
        val attacker = source.attacker
        val creative = attacker is PlayerEntity && attacker.abilities.creativeMode
        if (!creative && world.gameRules.getBoolean(GameRules.DO_ENTITY_DROPS)) {
            val itemStack = ItemStack(ModItems.BULLDOZER)
            if (hasCustomName()) {
                itemStack.setCustomName(this.customName)
            }
            this.dropStack(itemStack)
        }
        return true
    }

    override fun interact(player: PlayerEntity, hand: Hand): ActionResult {
        if (player.shouldCancelInteraction()) {
            return ActionResult.PASS
        }
        if (hasPassengers()) {
            return ActionResult.PASS
        }
        return if (!world.isClient) {
            if (player.startRiding(this)) ActionResult.CONSUME else ActionResult.PASS
        } else ActionResult.SUCCESS
    }

    override fun tick() {
        super.tick()
        if (!hasNoGravity()) {
            velocity = velocity.add(0.0, -0.04, 0.0)
        }
        val passenger: Entity? = this.controllingPassenger
        if (passenger is PlayerEntity) {
            val world = world
            travelControlled(passenger)
            if (!world.isClient) {
                val collisions: MutableSet<BlockPos> = HashSet()
                BlockPos.stream(boundingBox.expand(0.2, 0.0, 0.2)).forEach {
                    if (Pushable.DEFAULT_PREDICATE.apply(world, it) && it.y >= this.blockY) {
                        collisions.add(BlockPos(it))
                    }
                }
                BlockPos.stream(passenger.getBoundingBox().expand(0.2, 0.0, 0.2)).forEach {
                    if (Pushable.DEFAULT_PREDICATE.apply(world, it) && it.y >= this.blockY) {
                        collisions.add(BlockPos(it))
                    }
                }
                for (pos in collisions) {
                    val entity: PushedBlockEntity = PushedBlockEntity.createEntity(world, pos) {
                        it.pos.subtract(this.pos).normalize().multiply(0.5)
                    }
                    world.spawnEntity(entity)
                }
            }
        } else {
            move(MovementType.SELF, velocity)
        }
        velocity = velocity.multiply(0.98)
    }

    override fun getControllingPassenger(): LivingEntity? =
        this.firstPassenger as? LivingEntity ?: super.getControllingPassenger()

    override fun getPassengerAttachmentPos(
        passenger: Entity,
        dimensions: EntityDimensions,
        scaleFactor: Float,
    ) = Vector3f(0f, dimensions.height / 2, 0f)

    override fun collidesWith(other: Entity) = BoatEntity.canCollide(this, other)

    override fun canHit() = true

    private fun travelControlled(controllingPlayer: PlayerEntity) {
        val vec3d = getControlledMovementInput(controllingPlayer)
        tickControlled(controllingPlayer)
        if (vec3d.x != 0.0 || vec3d.z != 0.0) {
            move(MovementType.SELF, vec3d)
        }
    }

    private fun tickControlled(controllingPlayer: PlayerEntity) {
        val vec2f = getControlledRotation(controllingPlayer)
        setRotation(vec2f.y, vec2f.x)
        prevYaw = yaw
        controllingPlayer.setBodyYaw(controllingPlayer.getHeadYaw())
    }

    private fun getControlledRotation(controllingPassenger: LivingEntity): Vec2f {
        return Vec2f(0f, controllingPassenger.getYaw(0.5f))
    }

    private fun getControlledMovementInput(controllingPlayer: PlayerEntity): Vec3d {
        val y = velocity.y
        val forwardSpeed = controllingPlayer.forwardSpeed
        val vector = this.rotationVector.normalize()
        return if (forwardSpeed > 0) {
            vector.multiply(0.15).withAxis(Direction.Axis.Y, y)
        } else if (forwardSpeed < 0) {
            vector.multiply(-0.1).withAxis(Direction.Axis.Y, y)
        } else {
            Vec3d(0.0, y, 0.0)
        }
    }
}
