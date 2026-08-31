package com.trippoint.backend.booking.graphql.input

import com.trippoint.backend.booking.model.BookingStatus
import com.trippoint.backend.booking.model.BookingType

data class BookingFilterInput(
    val type: BookingType? = null,
    val status: BookingStatus? = null,
    val search: String? = null
)
