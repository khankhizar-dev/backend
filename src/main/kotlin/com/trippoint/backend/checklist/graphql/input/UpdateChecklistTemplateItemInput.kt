package com.trippoint.backend.checklist.graphql.input

import com.trippoint.backend.checklist.model.ChecklistItemCategory

data class UpdateChecklistTemplateItemInput(
    val name: String? = null,
    val category: ChecklistItemCategory? = null,
    val essential: Boolean? = null
)
