package com.trippoint.backend.booking.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "booking_travellers",
    indexes = [
        Index(
            name = "idx_booking_travellers_booking_id",
            columnList = "booking_id"
        )
    ]
)
class BookingTraveller(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "booking_id", nullable = false)
    var bookingId: UUID,

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String,

    @Column(name = "last_name", length = 100)
    var lastName: String? = null,

    @Column(length = 255)
    var email: String? = null,

    @Column(name = "phone_number", length = 30)
    var phoneNumber: String? = null,

    @Column(name = "date_of_birth")
    var dateOfBirth: LocalDate? = null,

    @Column(name = "ticket_number", length = 100)
    var ticketNumber: String? = null,

    @Column(name = "seat_number", length = 30)
    var seatNumber: String? = null,

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