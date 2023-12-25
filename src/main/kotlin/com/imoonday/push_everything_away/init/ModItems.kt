package com.imoonday.push_everything_away.init

import com.imoonday.push_everything_away.PushEverythingAway
import com.imoonday.push_everything_away.items.BulldozerItem
import com.imoonday.push_everything_away.items.DestructionStaffItem
import com.imoonday.push_everything_away.items.GravityStaffItem
import com.imoonday.push_everything_away.items.GroundHammerItem
import com.imoonday.push_everything_away.utils.HammerMaterial
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.Items
import net.minecraft.registry.RegistryKey
import net.minecraft.util.Identifier

object ModItems {
    @JvmField
    val HAMMERS: MutableMap<HammerMaterial, GroundHammerItem> = LinkedHashMap()

    @JvmField
    val BULLDOZER = BulldozerItem(FabricItemSettings().maxCount(1).requires()).register(name = "bulldozer", null)

    @JvmField
    val GRAVITY_GUN = GravityStaffItem(FabricItemSettings().maxCount(1).maxDamage(100)).register("gravity_staff")

    @JvmField
    val DESTRUCTION_GUN =
        DestructionStaffItem(FabricItemSettings().maxCount(1).maxDamage(100)).register("destruction_staff")

    fun init() {
        for (material in HammerMaterial.entries) {
            HAMMERS[material] = material.register()
        }
    }

    private fun HammerMaterial.register(): GroundHammerItem {
        return GroundHammerItem(this, FabricItemSettings()).register(
            asString() + "_ground_hammer"
        ) as GroundHammerItem
    }

    private fun Item.register(
        name: String,
        vararg itemGroups: RegistryKey<ItemGroup>? = arrayOf(PushEverythingAway.ITEM_GROUP_REGISTRY_KEY),
    ): Item {
        itemGroups.filterNotNull().forEach {
            ItemGroupEvents.modifyEntriesEvent(it)
                .register(ItemGroupEvents.ModifyEntries { entries -> entries.add(this) })
        }
        return Items.register(Identifier(PushEverythingAway.MOD_ID, name), this)
    }
}
