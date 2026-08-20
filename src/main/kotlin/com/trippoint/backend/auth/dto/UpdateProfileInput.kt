package com.trippoint.backend.auth.dto

data class UpdateProfileInput(
    val firstName: String? = null,
    val lastName: String? = null,
    val username: String? = null,
    val country: String? = null,
    val currency: String? = null,
    val language: String? = null,
    val timezone: String? = null
)
