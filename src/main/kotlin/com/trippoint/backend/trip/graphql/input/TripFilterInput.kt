package com.trippoint.backend.trip.graphql.input

import com.trippoint.backend.trip.model.TripStatus

data class TripFilterInput(
    val status: TripStatus? = null,
    val search: String? = null
)
