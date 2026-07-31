package com.trippoint.backend.auth.dto

data class AuthPayload(
    val user: UserResponse,
    val token: String,
    val refreshToken: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

