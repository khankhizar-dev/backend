package com.trippoint.backend.booking.graphql.input

import com.fasterxml.jackson.databind.JsonNode
import com.trippoint.backend.booking.model.BookingType
import java.math.BigDecimal

data class CreateBookingInput(
    val itineraryDayId: String? = null,
    val type: BookingType,
    val title: String,
    val provider: String? = null,
    val bookingReference: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val location: String? = null,
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val notes: String? = null,
    val details: Map<String, Any?>? = null
)