package com.trippoint.backend.checklist.graphql

import com.trippoint.backend.checklist.entity.ChecklistTemplate
import com.trippoint.backend.checklist.entity.ChecklistTemplateItem
import com.trippoint.backend.checklist.entity.ChecklistTemplateSection
import java.time.format.DateTimeFormatter

private val TEMPLATE_DATE_FORMATTER =
    DateTimeFormatter.ISO_LOCAL_DATE_TIME

data class ChecklistTemplateResponse(
    val id: String,
    val createdBy: String?,
    val name: String,
    val description: String?,
    val type: String,
    val status: String,
    val sections: List<ChecklistTemplateSectionResponse>,
    val createdAt: String,
    val updatedAt: String
)

data class ChecklistTemplateSectionResponse(
    val id: String,
    val templateId: String,
    val name: String,
    val position: Int,
    val items: List<ChecklistTemplateItemResponse>,
    val createdAt: String,
    val updatedAt: String
)

data class ChecklistTemplateItemResponse(
    val id: String,
    val sectionId: String,
    val name: String,
    val category: String,
    val essential: Boolean,
    val position: Int,
    val createdAt: String,
    val updatedAt: String
)

fun ChecklistTemplateItem.toResponse() =
    ChecklistTemplateItemResponse(
        id = id.toString(),
        sectionId = sectionId.toString(),
        name = name,
        category = category.name,
        essential = essential,
        position = position,
        createdAt = createdAt.format(TEMPLATE_DATE_FORMATTER),
        updatedAt = updatedAt.format(TEMPLATE_DATE_FORMATTER)
    )

fun ChecklistTemplateSection.toResponse(
    items: List<ChecklistTemplateItem>
) =
    ChecklistTemplateSectionResponse(
        id = id.toString(),
        templateId = templateId.toString(),
        name = name,
        position = position,
        items = items.map { it.toResponse() },
        createdAt = createdAt.format(TEMPLATE_DATE_FORMATTER),
        updatedAt = updatedAt.format(TEMPLATE_DATE_FORMATTER)
    )

fun ChecklistTemplate.toResponse(
    sections: List<ChecklistTemplateSectionResponse>
) =
    ChecklistTemplateResponse(
        id = id.toString(),
        createdBy = createdBy?.toString(),
        name = name,
        description = description,
        type = type.name,
        status = status.name,
        sections = sections,
        createdAt = createdAt.format(TEMPLATE_DATE_FORMATTER),
        updatedAt = updatedAt.format(TEMPLATE_DATE_FORMATTER)
    )