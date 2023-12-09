package com.imoonday.push_everything_away.client;

import com.imoonday.push_everything_away.PushEverythingAway;
import com.imoonday.push_everything_away.client.renderer.PushedBlockEntityRenderer;
import com.imoonday.push_everything_away.items.GroundHammerItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class PushEverythingAwayClient implements ClientModInitializer {

    public static final KeyBinding DISPLAY_SELECTION_OUTLINES = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.displaySelectionOutlines", GLFW.GLFW_KEY_O, "group.push_everything_away"));
    public static boolean displaySelectionOutlines = true;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(PushEverythingAway.PUSHED_BLOCK, PushedBlockEntityRenderer::new);
        WorldRenderEvents.LAST.register(GroundHammerItem::renderSelectionBoxes);
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hitResult) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return true;
            }
            ClientPlayerEntity player = client.player;
            if (player == null) {
                return true;
            }
            if (!(player.getMainHandStack().getItem() instanceof GroundHammerItem || player.getOffHandStack().getItem() instanceof GroundHammerItem)) {
                return true;
            }
            return client.crosshairTarget == null || !client.crosshairTarget.equals(hitResult);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean pressed = false;
            while (DISPLAY_SELECTION_OUTLINES.wasPressed()) {
                displaySelectionOutlines = !displaySelectionOutlines;
                pressed = true;
            }
            if (pressed && client.player != null) {
                client.player.sendMessage(Text.translatable("config.push_everything_away.displaySelectionOutlines." + (displaySelectionOutlines ? "on" : "off")).formatted(displaySelectionOutlines ? Formatting.GREEN : Formatting.RED), true);
            }
        });
    }
}
