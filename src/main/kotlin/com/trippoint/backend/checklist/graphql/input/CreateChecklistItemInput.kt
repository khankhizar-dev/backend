package com.trippoint.backend.checklist.graphql.input

import com.trippoint.backend.checklist.model.ChecklistItemCategory

data class CreateChecklistItemInput(
    val name: String,
    val category: ChecklistItemCategory,
    val essential: Boolean? = null,
    val dueDate: String? = null
)
