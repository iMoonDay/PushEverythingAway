package com.imoonday.push_everything_away.enchantments

class EfficiencySlamEnchantment : SlamEnchantment() {
    override fun getPushSpeed(level: Int) =
        (settings.basePushSpeed + level * settings.pushSpeedPerLevel).coerceIn(
            0f,
            settings.maxPushSpeed
        )

    override fun getMaxLevel() = 4
}
