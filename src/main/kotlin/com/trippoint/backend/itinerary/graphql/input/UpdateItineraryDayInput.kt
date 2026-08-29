package com.trippoint.backend.itinerary.graphql.input

data class UpdateItineraryDayInput(
    val dayNumber: Int?,
    val date: String?,
    val title: String?,
    val notes: String?
)
