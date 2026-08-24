package com.trippoint.backend.auth.dto

data class UserPreferencesResponse(
    val currency: String,
    val language: String,
    val dateFormat: String,
    val units: String,
    val theme: String,
    val timezone: String
)
