package com.imoonday.push_everything_away.init

import com.imoonday.push_everything_away.PushEverythingAway
import com.imoonday.push_everything_away.enchantments.EfficiencySlamEnchantment
import com.imoonday.push_everything_away.enchantments.PowerSlamEnchantment
import com.imoonday.push_everything_away.enchantments.RangeSlamEnchantment
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentLevelEntry
import net.minecraft.item.EnchantedBookItem
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object ModEnchantments {
    val RANGE_SLAM = RangeSlamEnchantment().register("range_slam")
    val EFFICIENCY_SLAM = EfficiencySlamEnchantment().register("efficiency_slam")
    val POWER_SLAM = PowerSlamEnchantment().register("power_slam")

    fun init() {

    }

    private fun Enchantment.register(name: String): Enchantment {
        val enchantment = Registry.register(Registries.ENCHANTMENT, Identifier(PushEverythingAway.MOD_ID, name), this)
        (minLevel..maxLevel)
            .map { EnchantedBookItem.forEnchantment(EnchantmentLevelEntry(this, it)) }
            .forEach { stack ->
                ItemGroupEvents.modifyEntriesEvent(PushEverythingAway.ITEM_GROUP_REGISTRY_KEY)
                    .register(ItemGroupEvents.ModifyEntries { it.add(stack) })
            }
        return enchantment
    }
}
