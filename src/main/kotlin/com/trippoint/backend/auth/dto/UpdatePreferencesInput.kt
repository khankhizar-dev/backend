package com.trippoint.backend.auth.dto

data class UpdatePreferencesInput(
    val currency: String?,
    val language: String?,
    val dateFormat: String?,
    val units: String?,
    val theme: String?
)
