package com.imoonday.push_everything_away.mixin;

import com.imoonday.push_everything_away.entities.PushedBlockEntity;
import com.imoonday.push_everything_away.network.NetworkHandler;
import com.imoonday.push_everything_away.utils.CooledPushable;
import com.imoonday.push_everything_away.utils.Grabbable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends Entity implements CooledPushable, Grabbable {

    private PlayerEntityMixin(EntityType<?> type, World world) {
        super(type, world);
        throw new IllegalStateException();
    }

    @Shadow
    public abstract boolean isCreative();

    @Unique
    private static final TrackedData<Float> GRABBING_DISTANCE_OFFSET = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.FLOAT);

    @Unique
    private int pushCooldown;
    @Unique
    private PushedBlockEntity grabbingEntity;

    @Override
    public int pushEverythingAway$getPushCooldown() {
        return pushCooldown;
    }

    @Override
    public void pushEverythingAway$setPushCooldown(int pushCooldown) {
        this.pushCooldown = pushCooldown;
    }

    @Override
    public PushedBlockEntity pushEverythingAway$getGrabbingEntity() {
        return grabbingEntity;
    }

    @Override
    public void pushEverythingAway$setGrabbingEntity(PushedBlockEntity grabbingEntity) {
        this.grabbingEntity = grabbingEntity;
        PlayerEntity entity = (PlayerEntity) (Object) this;
        if (entity instanceof ServerPlayerEntity serverPlayer) {
            NetworkHandler.modifyGrabbingStatus(serverPlayer, this.grabbingEntity != null);
            if (this.grabbingEntity == null) {
                this.pushEverythingAway$setGrabbingDistanceOffset(0.0);
            }
        }
    }

    @Override
    public double pushEverythingAway$getGrabbingDistance() {
        return (this.isCreative() ? 3.0 : 2.5) - this.pushEverythingAway$getGrabbingDistanceOffset();
    }

    @Override
    public double pushEverythingAway$getGrabbingDistanceOffset() {
        return MathHelper.clamp(this.dataTracker.get(GRABBING_DISTANCE_OFFSET), 0, this.isCreative() ? 3.0 : 2.5);
    }

    @Override
    public void pushEverythingAway$setGrabbingDistanceOffset(double offset) {
        this.dataTracker.set(GRABBING_DISTANCE_OFFSET, MathHelper.clamp((float) offset, 0.0f, this.isCreative() ? 3.0f : 2.5f));
    }

    @Override
    public void pushEverythingAway$addGrabbingDistanceOffset(double value) {
        this.pushEverythingAway$setGrabbingDistanceOffset(this.pushEverythingAway$getGrabbingDistanceOffset() + value);
    }

    @Inject(method = "initDataTracker", at = @At("RETURN"))
    public void init(CallbackInfo ci) {
        this.dataTracker.startTracking(GRABBING_DISTANCE_OFFSET, 0.0f);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void tick(CallbackInfo ci) {
        int cooldown = this.pushEverythingAway$getPushCooldown();
        if (cooldown > 0) {
            this.pushEverythingAway$setPushCooldown(cooldown - 1);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("PushCooldown", this.pushEverythingAway$getPushCooldown());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("PushCooldown")) {
            this.pushEverythingAway$setPushCooldown(nbt.getInt("PushCooldown"));
        }
    }
}
