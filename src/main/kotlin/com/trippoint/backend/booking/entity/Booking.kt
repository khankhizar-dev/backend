package com.trippoint.backend.booking.entity

import com.fasterxml.jackson.databind.JsonNode
import com.trippoint.backend.booking.model.BookingSource
import com.trippoint.backend.booking.model.BookingStatus
import com.trippoint.backend.booking.model.BookingType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "bookings",
    indexes = [
        Index(
            name = "idx_bookings_trip_id",
            columnList = "trip_id"
        ),
        Index(
            name = "idx_bookings_trip_status",
            columnList = "trip_id,status"
        ),
        Index(
            name = "idx_bookings_trip_type",
            columnList = "trip_id,type"
        ),
        Index(
            name = "idx_bookings_reference",
            columnList = "booking_reference"
        ),
        Index(
            name = "idx_bookings_start_at",
            columnList = "start_at"
        )
    ]
)
class Booking(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "trip_id", nullable = false)
    var tripId: UUID,

    @Column(name = "itinerary_day_id")
    var itineraryDayId: UUID? = null,

    @Column(name = "created_by", nullable = false)
    var createdBy: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var type: BookingType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: BookingStatus = BookingStatus.PENDING,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(length = 200)
    var provider: String? = null,

    @Column(name = "booking_reference", length = 100)
    var bookingReference: String? = null,

    @Column(name = "start_at")
    var startAt: LocalDateTime? = null,

    @Column(name = "end_at")
    var endAt: LocalDateTime? = null,

    @Column(length = 500)
    var location: String? = null,

    @Column(precision = 12, scale = 2)
    var amount: BigDecimal? = null,

    @Column(length = 3)
    var currency: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var source: BookingSource = BookingSource.MANUAL,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var details: JsonNode? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}