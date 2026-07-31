package com.trippoint.backend.auth.dto

import com.trippoint.backend.auth.dto.UserResponse

data class AuthPayload(
    val user: UserResponse,
    val token: String,
    val refreshToken: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

