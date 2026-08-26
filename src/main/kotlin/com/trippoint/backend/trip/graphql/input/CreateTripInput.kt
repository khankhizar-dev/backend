package com.trippoint.backend.trip.graphql.input

data class CreateTripInput(
    val name: String,
    val destination: String,
    val startDate: String,
    val endDate: String
)
