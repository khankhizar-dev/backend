package com.trippoint.backend.checklist.graphql.input

data class UpdateChecklistInput(
    val name: String? = null,
    val description: String? = null
)
