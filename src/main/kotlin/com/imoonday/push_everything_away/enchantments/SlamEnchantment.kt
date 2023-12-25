package com.imoonday.push_everything_away.enchantments

import com.imoonday.push_everything_away.config.Config
import com.imoonday.push_everything_away.config.EnchantmentSettings
import com.imoonday.push_everything_away.utils.PushableObject
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.entity.EquipmentSlot

open class SlamEnchantment : Enchantment, PushableObject {
    protected constructor(
        rarity: Rarity?,
        target: EnchantmentTarget?,
        slotTypes: Array<EquipmentSlot?>?,
    ) : super(rarity, target, slotTypes)

    protected constructor() : super(
        Rarity.RARE,
        EnchantmentTarget.WEAPON,
        arrayOf<EquipmentSlot>(EquipmentSlot.MAINHAND)
    )

    companion object {
        val settings: EnchantmentSettings
            get() = Config.instance.enchantmentSettings
    }
}
