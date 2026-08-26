package com.trippoint.backend.trip.graphql.input

import com.trippoint.backend.trip.model.TripStatus

data class UpdateTripInput(
    val name: String? = null,
    val destination: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: TripStatus? = null
)
