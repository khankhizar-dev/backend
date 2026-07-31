package com.trippoint.backend.auth.dto

import java.util.UUID

data class UserResponse(
    val id: UUID,
    val email: String,
    val firstName: String?,
    val lastName: String?
)
