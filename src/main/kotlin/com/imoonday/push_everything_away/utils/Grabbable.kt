package com.imoonday.push_everything_away.utils

import com.imoonday.push_everything_away.entities.PushedBlockEntity

interface Grabbable {
    fun `pushEverythingAway$getGrabbingEntity`(): PushedBlockEntity? = null

    fun `pushEverythingAway$setGrabbingEntity`(entity: PushedBlockEntity?) {}

    fun `pushEverythingAway$getGrabbingDistance`(): Double = 0.0

    fun `pushEverythingAway$getGrabbingDistanceOffset`(): Double = 0.0

    fun `pushEverythingAway$setGrabbingDistanceOffset`(offset: Double) {}

    fun `pushEverythingAway$addGrabbingDistanceOffset`(value: Double) {}
}
