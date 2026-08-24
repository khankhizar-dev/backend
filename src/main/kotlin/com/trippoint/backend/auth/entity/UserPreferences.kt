package com.trippoint.backend.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "user_preferences",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id"])
    ]
)
class UserPreferences(

    @Id
    @GeneratedValue
    var id: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false, length = 10)
    var currency: String = "INR",

    @Column(nullable = false, length = 10)
    var language: String = "en",

    @Column(name = "date_format", nullable = false, length = 30)
    var dateFormat: String = "DD/MM/YYYY",

    @Column(nullable = false, length = 20)
    var units: String = "METRIC",

    @Column(nullable = false, length = 20)
    var theme: String = "LIGHT",

    @Column(nullable = false, length = 50)
    var timezone: String = "Asia/Kolkata"
)
