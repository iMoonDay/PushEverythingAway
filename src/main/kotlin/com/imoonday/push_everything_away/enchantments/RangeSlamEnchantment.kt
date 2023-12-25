package com.imoonday.push_everything_away.enchantments

class RangeSlamEnchantment : SlamEnchantment() {
    override fun getPushRange(level: Int) =
        (settings.basePushRange + level * settings.pushRangePerLevel).coerceIn(
            0,
            settings.maxPushRange
        )


    override fun getMaxLevel() = 4
}
