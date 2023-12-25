package com.imoonday.push_everything_away

import com.imoonday.push_everything_away.config.Config
import com.imoonday.push_everything_away.init.ModEnchantments
import com.imoonday.push_everything_away.init.ModEntities
import com.imoonday.push_everything_away.init.ModItems
import com.imoonday.push_everything_away.network.NetworkHandler
import com.imoonday.push_everything_away.network.NetworkHandler.updateConfigToClient
import com.imoonday.push_everything_away.utils.HammerMaterial
import com.imoonday.push_everything_away.utils.Pushable.Companion.tryPushBlock
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.EndDataPackReload
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.Before
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.resource.LifecycledResourceManager
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class PushEverythingAway : ModInitializer {
    override fun onInitialize() {
        Config.load()
        ModEntities.init()
        ModItems.init()
        ModEnchantments.init()
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(EndDataPackReload { server: MinecraftServer?, _: LifecycledResourceManager?, success: Boolean ->
            if (success) {
                Config.load()
                PlayerLookup.all(server).forEach { it.updateConfigToClient() }
            }
        })
        PlayerBlockBreakEvents.BEFORE.register(Before { world: World, player: PlayerEntity, pos: BlockPos, _: BlockState?, _: BlockEntity? ->
            beforeBlockBreak(
                world,
                player,
                pos
            )
        })
        ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler: ServerPlayNetworkHandler, _: PacketSender?, _: MinecraftServer? ->
            handler.player.updateConfigToClient()
        })
        NetworkHandler.registerServer()
    }

    companion object {
        const val MOD_ID = "push_everything_away"

        @JvmField
        val ITEM_GROUP_REGISTRY_KEY: RegistryKey<ItemGroup> =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(MOD_ID, MOD_ID))
        val ITEM_GROUP: ItemGroup = Registry.register(
            Registries.ITEM_GROUP,
            ITEM_GROUP_REGISTRY_KEY.value,
            FabricItemGroup.builder().displayName(Text.translatable("group.$MOD_ID"))
                .icon { ItemStack(HammerMaterial.BEDROCK.item ?: ModItems.GRAVITY_GUN) }.build()
        )

        private fun beforeBlockBreak(world: World, player: PlayerEntity, pos: BlockPos): Boolean {
            val pushed = player.tryPushBlock(
                world = world,
                hand = Hand.MAIN_HAND,
                pos = pos,
                pushStrength = 1f,
                alwaysUpward = false,
                breakBlock = true
            )
            val accepted = pushed.isAccepted
            if (accepted && !world.isClient) {
                player.mainHandStack.damage(
                    1,
                    player
                ) { it.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND) }
            }
            return !accepted
        }
    }
}
