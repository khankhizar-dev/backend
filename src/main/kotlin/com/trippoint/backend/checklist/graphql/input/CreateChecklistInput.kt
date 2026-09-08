package com.trippoint.backend.checklist.graphql.input

data class CreateChecklistInput(
    val name: String,
    val description: String? = null
)
