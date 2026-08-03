package com.trippoint.backend.auth.dto

import java.time.OffsetDateTime
import java.util.UUID

data class AuthPayload(
    val user: UserResponse,
    val token: String,
    val refreshToken: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

data class UserDeviceResponse(
    val id: UUID,
    val deviceName: String?,
    val platform: String?,
    val appVersion: String?,
    val lastLoginAt: String?,
    val createdAt: String
)

