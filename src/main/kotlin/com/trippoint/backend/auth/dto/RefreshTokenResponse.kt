package com.trippoint.backend.auth.dto

data class RefreshTokenResponse(
    val token: String,
    val refreshToken: String
)
