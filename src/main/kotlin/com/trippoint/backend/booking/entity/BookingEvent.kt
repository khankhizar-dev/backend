package com.trippoint.backend.booking.entity

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "booking_events",
    indexes = [
        Index(
            name = "idx_booking_events_booking_id",
            columnList = "booking_id"
        ),
        Index(
            name = "idx_booking_events_booking_created",
            columnList = "booking_id,created_at"
        )
    ]
)
class BookingEvent(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "booking_id", nullable = false)
    var bookingId: UUID,

    @Column(name = "event_type", nullable = false, length = 50)
    var eventType: String,

    @Column(length = 500)
    var description: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var metadata: JsonNode? = null,

    @Column(name = "created_by")
    var createdBy: UUID? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)