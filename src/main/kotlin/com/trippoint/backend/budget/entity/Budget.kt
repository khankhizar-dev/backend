package com.trippoint.backend.budget.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "budgets",
    indexes = [
        Index(
            name = "idx_budgets_trip_id",
            columnList = "trip_id"
        ),
        Index(
            name = "idx_budgets_created_by",
            columnList = "created_by"
        )
    ]
)
class Budget(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "trip_id", nullable = false, unique = true)
    var tripId: UUID,

    @Column(
        name = "total_amount",
        nullable = false,
        precision = 14,
        scale = 2
    )
    var totalAmount: BigDecimal,

    @Column(nullable = false, length = 3)
    var currency: String,

    @Column(nullable = false)
    var locked: Boolean = false,

    @Column(name = "created_by", nullable = false)
    var createdBy: UUID,

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