package com.imoonday.push_everything_away.network

import com.imoonday.push_everything_away.PushEverythingAway
import com.imoonday.push_everything_away.config.Config
import com.imoonday.push_everything_away.utils.ChargeManager
import com.imoonday.push_everything_away.utils.ChargeManager.onStartCooling
import com.imoonday.push_everything_away.utils.ChargeManager.onStoppedCharging
import com.imoonday.push_everything_away.utils.Grabbable
import com.imoonday.push_everything_away.utils.GrabbingManager
import com.imoonday.push_everything_away.utils.GrabbingManager.pushGrabbingEntity
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import kotlin.math.min
import kotlin.math.roundToInt

object NetworkHandler {
    private val STOP_CHARGING = Identifier(PushEverythingAway.MOD_ID, "stop_charging")
    private val START_COOLING = Identifier(PushEverythingAway.MOD_ID, "start_cooling")
    private val CONFIG_SYNC = Identifier(PushEverythingAway.MOD_ID, "config_sync")
    private val MODIFY_GRABBING_STATUS = Identifier(PushEverythingAway.MOD_ID, "modify_grabbing_status")
    private val PUSH_GRABBING_ENTITY = Identifier(PushEverythingAway.MOD_ID, "push_grabbing_entity")
    private val MODIFY_GRABBING_DISTANCE = Identifier(PushEverythingAway.MOD_ID, "modify_grabbing_distance")
    fun pushGrabbingEntity(pushStrength: Float) {
        val buf = PacketByteBufs.create()
            .writeFloat(pushStrength)
        ClientPlayNetworking.send(PUSH_GRABBING_ENTITY, buf)
    }

    @JvmStatic
    fun ServerPlayerEntity.modifyGrabbingStatus(grabbing: Boolean) {
        val buf = PacketByteBufs.create().writeBoolean(grabbing)
        ServerPlayNetworking.send(this, MODIFY_GRABBING_STATUS, buf)
    }

    fun stopCharging(stack: ItemStack, pos: BlockPos, hand: Hand, chargeTimes: Int) {
        if (!ChargeManager.cooling) {
            val buf = PacketByteBufs.create()
                .writeItemStack(stack)
                .writeBlockPos(pos)
                .writeEnumConstant(hand)
                .writeInt(chargeTimes)
            ClientPlayNetworking.send(STOP_CHARGING, buf)
        }
    }

    @JvmStatic
    fun modifyGrabbingDistance(value: Double) {
        val buf = PacketByteBufs.create().writeDouble(value)
        ClientPlayNetworking.send(MODIFY_GRABBING_DISTANCE, buf)
    }

    fun registerServer() {
        STOP_CHARGING.registerServerReceiver { server: MinecraftServer, player: ServerPlayerEntity, _: ServerPlayNetworkHandler, buf: PacketByteBuf, responseSender: PacketSender ->
            val stack = buf.readItemStack()
            val pos = buf.readBlockPos()
            val hand = buf.readEnumConstant(Hand::class.java)
            val chargeTimes = buf.readInt()
            server.execute {
                val cooldown = player.onStoppedCharging(
                    stack = stack,
                    world = player.world,
                    pos = pos,
                    hand = hand,
                    chargingTicks = chargeTimes
                )
                if (cooldown > 0) {
                    val responseBuf = PacketByteBufs.create()
                        .writeInt(cooldown)
                    responseSender.sendPacket(START_COOLING, responseBuf)
                }
            }
        }
        PUSH_GRABBING_ENTITY.registerServerReceiver { server: MinecraftServer, player: ServerPlayerEntity, _: ServerPlayNetworkHandler, buf: PacketByteBuf, _: PacketSender ->
            val pushStrength = buf.readFloat()
            server.execute { player.pushGrabbingEntity(pushStrength) }
        }
        MODIFY_GRABBING_DISTANCE.registerServerReceiver { server, entity, _, buf, _ ->
            val distance = buf.readDouble()
            server.execute {
                (entity as? Grabbable)?.let {
                    it.`pushEverythingAway$addGrabbingDistanceOffset`(distance)
                    entity.sendMessage(
                        Text.literal(
                            it.`pushEverythingAway$getGrabbingDistance`().times(10).roundToInt().toDouble().div(10)
                                .toString()
                        ), true
                    )
                }
            }
        }
    }

    private fun Identifier.registerServerReceiver(channelHandler: (MinecraftServer, ServerPlayerEntity, ServerPlayNetworkHandler, PacketByteBuf, PacketSender) -> Unit) =
        ServerPlayNetworking.registerGlobalReceiver(this, channelHandler)

    @JvmStatic
    fun registerClient() {
        START_COOLING.registerClientReceiver { client: MinecraftClient, _: ClientPlayNetworkHandler, buf: PacketByteBuf, _: PacketSender ->
            val cooldown = buf.readInt()
            client.execute { onStartCooling(cooldown) }
        }
        CONFIG_SYNC.registerClientReceiver { client: MinecraftClient, _: ClientPlayNetworkHandler, buf: PacketByteBuf, _: PacketSender ->
            var count = buf.readInt()
            val json = StringBuilder()
            while (count-- > 0) {
                json.append(buf.readString())
            }
            client.execute {
                val config = Config.fromJson(json.toString())
                Config.setInstance(config, false)
            }
        }
        MODIFY_GRABBING_STATUS.registerClientReceiver { client: MinecraftClient, _: ClientPlayNetworkHandler, buf: PacketByteBuf, _: PacketSender ->
            val grabbing = buf.readBoolean()
            client.execute { GrabbingManager.grabbing = grabbing }
        }
    }

    private fun Identifier.registerClientReceiver(channelHandler: (MinecraftClient, ClientPlayNetworkHandler, PacketByteBuf, PacketSender) -> Unit) =
        ClientPlayNetworking.registerGlobalReceiver(this, channelHandler)

    fun ServerPlayerEntity.updateConfigToClient() {
        val buf = PacketByteBufs.create()
        val json = Config.instance.toJson()
        val strings = json.split(32767)
        buf.writeInt(strings.size)
        for (string in strings) {
            buf.writeString(string)
        }
        ServerPlayNetworking.send(this, CONFIG_SYNC, buf)
    }

    private fun String.split(chunkSize: Int): List<String> {
        val len = length
        val splitStr: MutableList<String> = ArrayList((len + chunkSize - 1) / chunkSize)
        var i = 0
        while (i < len) {
            splitStr.add(substring(i, min(len.toDouble(), (i + chunkSize).toDouble()).toInt()))
            i += chunkSize
        }
        return splitStr
    }
}
