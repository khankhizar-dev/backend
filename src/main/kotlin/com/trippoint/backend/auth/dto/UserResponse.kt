package com.trippoint.backend.auth.dto

import java.time.LocalDate
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val fullName: String? = null,
    val username: String? = null,
    val phoneNumber: String? = null,
    val dateOfBirth: LocalDate? = null,
    val nationality: String? = null,
    val profilePhotoUrl: String? = null,
    val country: String? = null,
    val currency: String? = null,
    val language: String? = null,
    val timezone: String? = null
)