package com.imoonday.push_everything_away.client

import com.imoonday.push_everything_away.config.ClientConfig
import com.imoonday.push_everything_away.config.ClientConfig.Companion.load
import com.imoonday.push_everything_away.init.ModEntities.initClient
import com.imoonday.push_everything_away.items.GroundHammerItem
import com.imoonday.push_everything_away.network.NetworkHandler.registerClient
import com.imoonday.push_everything_away.utils.ChargeManager.checkChargingStatus
import com.imoonday.push_everything_away.utils.GrabbingManager.checkGrabbingStatus
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.BeforeBlockOutline
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.hit.HitResult
import org.lwjgl.glfw.GLFW

class PushEverythingAwayClient : ClientModInitializer {
    override fun onInitializeClient() {
        load()
        initClient()
        WorldRenderEvents.LAST.register(WorldRenderEvents.Last(GroundHammerItem.Companion::renderSelectionBoxes))
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(BeforeBlockOutline { _: WorldRenderContext?, hitResult: HitResult? ->
            hitResult.shouldDisplayBlockOutline()
        })
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            client.registerDisplaySelectionOutlinesKey()
            client.checkChargingStatus()
            client.checkGrabbingStatus()
        })
        registerClient()
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher: CommandDispatcher<FabricClientCommandSource>, _: CommandRegistryAccess? ->
            dispatcher.registerClientReloadCommand()
        })
    }

    companion object {
        private val DISPLAY_SELECTION_OUTLINES: KeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.displaySelectionOutlines",
                GLFW.GLFW_KEY_O,
                "group.push_everything_away"
            )
        )
        val CHARGE: KeyBinding =
            KeyBindingHelper.registerKeyBinding(KeyBinding("key.charge", GLFW.GLFW_KEY_R, "group.push_everything_away"))

        private fun CommandDispatcher<FabricClientCommandSource>.registerClientReloadCommand() =
            register(
                ClientCommandManager.literal("pea-client-reload")
                    .executes { context: CommandContext<FabricClientCommandSource> ->
                        load()
                        context.source.sendFeedback(Text.translatable("commands.reload.success"))
                        1
                    })


        private fun MinecraftClient.registerDisplaySelectionOutlinesKey() {
            var pressed = false
            while (DISPLAY_SELECTION_OUTLINES.wasPressed()) {
                ClientConfig.instance.displaySelectionOutlines = !ClientConfig.instance.displaySelectionOutlines
                pressed = true
            }
            if (pressed) {
                player?.sendMessage(
                    Text.translatable("config.push_everything_away.displaySelectionOutlines." + if (ClientConfig.instance.displaySelectionOutlines) "on" else "off")
                        .formatted(if (ClientConfig.instance.displaySelectionOutlines) Formatting.GREEN else Formatting.RED),
                    true
                )
            }
        }

        private fun HitResult?.shouldDisplayBlockOutline(): Boolean {
            val client = MinecraftClient.getInstance() ?: return true
            val player = client.player ?: return true
            return if (!(player.mainHandStack.item is GroundHammerItem || player.offHandStack.item is GroundHammerItem)) {
                true
            } else client.crosshairTarget == null || client.crosshairTarget != this
        }
    }
}
