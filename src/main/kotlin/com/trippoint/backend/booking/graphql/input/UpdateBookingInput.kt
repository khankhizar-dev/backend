package com.trippoint.backend.booking.graphql.input

import com.trippoint.backend.booking.model.BookingStatus
import com.trippoint.backend.booking.model.BookingType
import java.math.BigDecimal

data class UpdateBookingInput(
    val type: BookingType? = null,
    val title: String? = null,
    val provider: String? = null,
    val bookingReference: String? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val location: String? = null,
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val status: BookingStatus? = null,
    val notes: String? = null,
    val details: String? = null
)