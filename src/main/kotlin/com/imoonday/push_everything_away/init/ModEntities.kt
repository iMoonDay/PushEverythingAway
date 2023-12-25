package com.imoonday.push_everything_away.init

import com.imoonday.push_everything_away.PushEverythingAway
import com.imoonday.push_everything_away.client.model.BulldozerEntityModel
import com.imoonday.push_everything_away.client.renderer.BulldozerEntityRenderer
import com.imoonday.push_everything_away.client.renderer.PushedBlockEntityRenderer
import com.imoonday.push_everything_away.client.renderer.ShockwaveEntityRenderer
import com.imoonday.push_everything_away.entities.BulldozerEntity
import com.imoonday.push_everything_away.entities.PushedBlockEntity
import com.imoonday.push_everything_away.entities.ShockwaveEntity
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.minecraft.client.render.entity.model.EntityModelLayer
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object ModEntities {
    @JvmField
    val PUSHED_BLOCK = Registry.register(
        Registries.ENTITY_TYPE, Identifier(PushEverythingAway.MOD_ID, "pushed_block"), EntityType.Builder.create(
            ::PushedBlockEntity, SpawnGroup.MISC
        ).setDimensions(0.98f, 0.98f).maxTrackingRange(10).trackingTickInterval(5).build("pushed_block")
    )!!

    @JvmField
    val BULLDOZER = Registry.register(
        Registries.ENTITY_TYPE, Identifier(PushEverythingAway.MOD_ID, "bulldozer"), EntityType.Builder.create(
            ::BulldozerEntity, SpawnGroup.MISC
        ).setDimensions(1.125f, 0.625f).maxTrackingRange(8).build("bulldozer")
    )!!

    @JvmField
    val SHOCKWAVE = Registry.register(
        Registries.ENTITY_TYPE, Identifier(PushEverythingAway.MOD_ID, "shockwave"), EntityType.Builder.create(
            ::ShockwaveEntity, SpawnGroup.MISC
        ).setDimensions(1.0f, 1.0f).maxTrackingRange(4).trackingTickInterval(10).build("shockwave")
    )!!

    @JvmField
    val BULLDOZER_MODEL_LAYER = EntityModelLayer(Identifier(PushEverythingAway.MOD_ID, "bulldozer"), "main")
    fun init() {}

    @JvmStatic
    fun initClient() {
        EntityRendererRegistry.register(PUSHED_BLOCK, ::PushedBlockEntityRenderer)
        EntityRendererRegistry.register(BULLDOZER, ::BulldozerEntityRenderer)
        EntityRendererRegistry.register(SHOCKWAVE, ::ShockwaveEntityRenderer)
        EntityModelLayerRegistry.registerModelLayer(BULLDOZER_MODEL_LAYER, BulldozerEntityModel::texturedModelData)
    }
}
