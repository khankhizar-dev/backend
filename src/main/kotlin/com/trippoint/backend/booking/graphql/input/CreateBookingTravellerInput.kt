package com.trippoint.backend.booking.graphql.input

data class CreateBookingTravellerInput(
    val firstName: String,
    val lastName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null,
    val ticketNumber: String? = null,
    val seatNumber: String? = null
)
