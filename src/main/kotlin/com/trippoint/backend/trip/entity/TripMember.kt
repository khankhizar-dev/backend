package com.trippoint.backend.trip.entity

import com.trippoint.backend.trip.model.TripMemberRole
import com.trippoint.backend.trip.model.TripMemberStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "trip_members",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_trip_members_trip_user",
            columnNames = ["trip_id", "user_id"]
        )
    ],
    indexes = [
        Index(
            name = "idx_trip_members_trip_id",
            columnList = "trip_id"
        ),
        Index(
            name = "idx_trip_members_user_id",
            columnList = "user_id"
        ),
        Index(
            name = "idx_trip_members_trip_status",
            columnList = "trip_id,status"
        )
    ]
)
class TripMember(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "trip_id", nullable = false)
    var tripId: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: TripMemberRole,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TripMemberStatus,

    @Column(name = "invited_at", nullable = false)
    var invitedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "joined_at")
    var joinedAt: LocalDateTime? = null
)