package com.imoonday.push_everything_away.mixin;

import com.imoonday.push_everything_away.network.NetworkHandler;
import com.imoonday.push_everything_away.utils.GrabbingManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;scrollInHotbar(D)V"), cancellable = true)
    public void beforeScrollInHotbar(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (this.client.player != null && GrabbingManager.grabbing && this.client.player.isSneaking()) {
            NetworkHandler.modifyGrabbingDistance(vertical > 0 ? -0.1 : 0.1);
            ci.cancel();
        }
    }
}
