package com.imoonday.push_everything_away.enchantments

class PowerSlamEnchantment : SlamEnchantment() {
    override fun getPushStrength(level: Int) =
        (settings.basePushStrength + level * settings.pushStrengthPerLevel).coerceIn(
            0f,
            settings.maxPushStrength
        )

    override fun getMaxLevel() = 4
}
