package com.trippoint.backend.checklist.graphql.input

data class CreateChecklistTemplateInput(
    val name: String,
    val description: String? = null
)
