package com.trippoint.backend.auth.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, unique = true)
    var email: String = "",

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",

    @Column(name = "first_name")
    var firstName: String? = null,

    @Column(name = "last_name")
    var lastName: String? = null,

    @Column(unique = true)
    var username: String? = null,

    @Column(name = "phone_number")
    var phoneNumber: String? = null,

    @Column(name = "date_of_birth")
    var dateOfBirth: LocalDate? = null,

    @Column(name = "nationality")
    var nationality: String? = null,

    @Column(name = "profile_image_url")
    var profileImageUrl: String? = null,

    @Column
    var country: String? = null,

    @Column(length = 3)
    var currency: String? = null,

    @Column(length = 10)
    var language: String? = null,

    @Column(length = 50)
    var timezone: String? = null,

    @Column(name = "is_email_verified")
    var emailVerified: Boolean = false,

    @Column(name = "is_active")
    var active: Boolean = true,

    @Column(name = "created_at")
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "tokens_valid_after")
    var tokensValidAfter: OffsetDateTime? = null,

    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null
)
