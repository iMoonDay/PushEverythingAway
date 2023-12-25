package com.imoonday.push_everything_away.entities

import com.imoonday.push_everything_away.init.ModEntities
import com.imoonday.push_everything_away.utils.Pushable
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageTypes
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.registry.tag.DamageTypeTags
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import kotlin.math.pow
import kotlin.math.roundToInt

open class ShockwaveEntity(type: EntityType<out ProjectileEntity>, world: World) : ProjectileEntity(type, world) {

    var power: Float
        get() = this.dataTracker.get(POWER)
        set(value) = this.dataTracker.set(POWER, value)

    constructor(world: World, pos: Vec3d, velocity: Vec3d, power: Float, owner: Entity?) : this(
        ModEntities.SHOCKWAVE,
        world
    ) {
        this.setPosition(pos)
        this.velocity = velocity
        this.prevX = pos.x
        this.prevY = pos.y
        this.prevZ = pos.z
        this.power = power
        this.owner = owner
    }

    override fun initDataTracker() = this.dataTracker.startTracking(POWER, 1.0f)

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        if (nbt.contains("Power", NbtElement.FLOAT_TYPE.toInt())) {
            power = nbt.getFloat("Power")
        }
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) = nbt.putFloat("Power", power)

    override fun tick() {
        var hitResult: HitResult?
        val entity = this.owner
        if (!world.isClient && (entity != null && entity.isRemoved || !world.isChunkLoaded(this.blockPos))) {
            this.discard()
            return
        }
        if (velocity.lengthSquared() < 0.01) {
            this.explode()
            return
        }
        super.tick()
        if (ProjectileUtil.getCollision(this) {
                this.canHit(it)
            }.also { hitResult = it }.type != HitResult.Type.MISS
        ) {
            this.onCollision(hitResult)
        }
        this.checkBlockCollision()
        ProjectileUtil.setRotationFromVelocity(this, 0.2f)
        this.setPosition(pos.add(velocity))
        if (age % 20 == 0) velocity = velocity.multiply(0.98)
    }

    override fun onEntityHit(entityHitResult: EntityHitResult) {
        super.onEntityHit(entityHitResult)
        entityHitResult.entity.takeIf { BoatEntity.canCollide(this, it) }?.addVelocity(velocity)
    }

    override fun onBlockHit(blockHitResult: BlockHitResult) {
        super.onBlockHit(blockHitResult)
        explode()
    }

    override fun damage(source: DamageSource, amount: Float) =
        (source.isIn(DamageTypeTags.IS_PROJECTILE)
                || source.isIn(DamageTypeTags.IS_EXPLOSION)
                || source.isOf(DamageTypes.PLAYER_ATTACK)) && explode()

    private fun explode(): Boolean {
        if (!world.isClient) {
            val blockPos = blockPos
            this.boundingBox.expand(1.0).mul((power - 1).coerceAtLeast(0.0f)).forEachSorted(
                inEllipse = true,
                comparator = compareByDescending<BlockPos> { it.y }.thenBy { it.getSquaredDistance(blockPos) }) {
                if (!Pushable.DEFAULT_PREDICATE.apply(world, it)) return@forEachSorted
                val entity = PushedBlockEntity.createEntity(world, it) { entity ->
                    entity.pos.subtract(pos).normalize().multiply(power.toDouble())
                }
                world.spawnEntity(entity)
            }
            world.createExplosion(this, x, y, z, power, World.ExplosionSourceType.NONE)
            this.discard()
            return true
        }
        return false
    }

    private fun Box.mul(value: Number): Box =
        this.expand(lengthX / 2 * value.toDouble(), lengthY / 2 * value.toDouble(), lengthZ / 2 * value.toDouble())

    private fun Box.forEachSorted(
        round: Boolean = false,
        inEllipse: Boolean = false,
        comparator: Comparator<BlockPos>,
        action: (pos: BlockPos) -> Unit,
    ) {
        val center = center
        val minX = this.minX.toInt(round)
        val minY = this.minY.toInt(round)
        val minZ = this.minZ.toInt(round)
        val maxX = this.maxX.toInt(round)
        val maxY = this.maxY.toInt(round)
        val maxZ = this.maxZ.toInt(round)
        val list = arrayListOf<BlockPos>()
        val a = lengthX / 2.0
        val b = lengthZ / 2.0
        for (x in minX..maxX)
            for (y in minY..maxY)
                for (z in minZ..maxZ) {
                    val transformedX = x - center.x
                    val transformedZ = z - center.z
                    val isInEllipse = (transformedX / a).pow(2) + (transformedZ / b).pow(2) <= 1.0
                    if (!inEllipse || isInEllipse) list.add(BlockPos(x, y, z))
                }
        list.sortedWith(comparator).forEach(action)
    }

    private fun Double.toInt(round: Boolean) = if (round) roundToInt() else toInt()

    companion object {
        private val POWER: TrackedData<Float> = DataTracker.registerData(
            ShockwaveEntity::class.java,
            TrackedDataHandlerRegistry.FLOAT
        )
    }
}