package com.trippoint.backend.trip.entity

import com.trippoint.backend.trip.model.TripStatus
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "trips",
    indexes = [
        Index(name = "idx_trips_owner_id", columnList = "owner_id"),
        Index(name = "idx_trips_owner_status", columnList = "owner_id,status"),
        Index(name = "idx_trips_start_date", columnList = "start_date")
    ]
)
class Trip(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "owner_id", nullable = false)
    var ownerId: UUID,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false, length = 255)
    var destination: String,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: TripStatus = TripStatus.DRAFT,

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