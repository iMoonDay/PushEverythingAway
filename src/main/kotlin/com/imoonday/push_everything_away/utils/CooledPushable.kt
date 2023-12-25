package com.imoonday.push_everything_away.utils

interface CooledPushable {
    fun `pushEverythingAway$getPushCooldown`(): Int {
        return 0
    }

    fun `pushEverythingAway$setPushCooldown`(cooldown: Int) {}
}
