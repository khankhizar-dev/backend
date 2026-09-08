package com.trippoint.backend.checklist.graphql.input

data class CreateChecklistFromTemplateInput(
    val name: String? = null,
    val description: String? = null
)