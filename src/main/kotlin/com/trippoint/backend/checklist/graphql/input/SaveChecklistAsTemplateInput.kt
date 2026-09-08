package com.trippoint.backend.checklist.graphql.input

data class SaveChecklistAsTemplateInput(
    val name: String,
    val description: String? = null
)
