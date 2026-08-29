package com.trippoint.backend.itinerary.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "itinerary_days",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_itinerary_days_trip_day",
            columnNames = ["trip_id", "day_number"]
        ),
        UniqueConstraint(
            name = "uk_itinerary_days_trip_date",
            columnNames = ["trip_id", "date"]
        )
    ],
    indexes = [
        Index(
            name = "idx_itinerary_days_trip_id",
            columnList = "trip_id"
        )
    ]
)
class ItineraryDay(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "trip_id", nullable = false)
    var tripId: UUID,

    @Column(name = "day_number", nullable = false)
    var dayNumber: Int,

    @Column(nullable = false)
    var date: LocalDate,

    @Column(length = 200)
    var title: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

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