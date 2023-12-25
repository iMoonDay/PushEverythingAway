package com.imoonday.push_everything_away.utils

import com.imoonday.push_everything_away.config.Config
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item.ItemStack
import java.util.function.BiFunction

interface PushableObject {
    fun getPushStrength(level: Int) = Config.instance.enchantmentSettings.basePushStrength

    fun getPushRange(level: Int) = Config.instance.enchantmentSettings.basePushRange

    fun getPushSpeed(level: Int) = Config.instance.enchantmentSettings.basePushSpeed

    companion object {
        @JvmStatic
        fun ItemStack.getMaxAttribute(attributeGetter: BiFunction<PushableObject, Int, Number>): Number {
            val enchantments = EnchantmentHelper.get(this)
            var value = attributeGetter.apply(DEFAULT, 1)
            for ((enchantment, level) in enchantments) {
                if (enchantment is PushableObject) {
                    val pushStrength = attributeGetter.apply(enchantment, level)
                    value = maxOf(value.toDouble(), pushStrength.toDouble())
                }
            }
            return value
        }

        private val DEFAULT: PushableObject = object : PushableObject {}
    }
}
