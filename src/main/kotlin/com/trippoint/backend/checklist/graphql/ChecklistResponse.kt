package com.trippoint.backend.checklist.graphql

import com.trippoint.backend.checklist.entity.Checklist
import com.trippoint.backend.checklist.entity.ChecklistItem
import com.trippoint.backend.checklist.entity.ChecklistSection
import com.trippoint.backend.checklist.service.ChecklistProgress
import java.time.format.DateTimeFormatter

data class ChecklistResponse(
    val id: String,
    val tripId: String,
    val createdBy: String,
    val name: String,
    val description: String?,
    val status: String,
    val totalItems: Int,
    val completedItems: Int,
    val progress: Int,
    val sections: List<ChecklistSectionResponse>,
    val createdAt: String,
    val updatedAt: String
)

data class ChecklistSectionResponse(
    val id: String,
    val checklistId: String,
    val name: String,
    val position: Int,
    val totalItems: Int,
    val completedItems: Int,
    val progress: Int,
    val items: List<ChecklistItemResponse>,
    val createdAt: String,
    val updatedAt: String
)

data class ChecklistItemResponse(
    val id: String,
    val sectionId: String,
    val createdBy: String,
    val name: String,
    val category: String,
    val essential: Boolean,
    val completed: Boolean,
    val dueDate: String?,
    val position: Int,
    val createdAt: String,
    val updatedAt: String
)

private val CHECKLIST_DATE_FORMATTER =
    DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun ChecklistItem.toResponse(): ChecklistItemResponse =
    ChecklistItemResponse(
        id = id.toString(),
        sectionId = sectionId.toString(),
        createdBy = createdBy.toString(),
        name = name,
        category = category.name,
        essential = essential,
        completed = completed,
        dueDate = dueDate?.format(CHECKLIST_DATE_FORMATTER),
        position = position,
        createdAt = createdAt.format(CHECKLIST_DATE_FORMATTER),
        updatedAt = updatedAt.format(CHECKLIST_DATE_FORMATTER)
    )

fun ChecklistSection.toResponse(
    items: List<ChecklistItem>,
    progress: ChecklistProgress
): ChecklistSectionResponse =
    ChecklistSectionResponse(
        id = id.toString(),
        checklistId = checklistId.toString(),
        name = name,
        position = position,
        totalItems = progress.totalItems,
        completedItems = progress.completedItems,
        progress = progress.percentage,
        items = items.map { it.toResponse() },
        createdAt = createdAt.format(CHECKLIST_DATE_FORMATTER),
        updatedAt = updatedAt.format(CHECKLIST_DATE_FORMATTER)
    )

fun Checklist.toResponse(
    sections: List<ChecklistSectionResponse>,
    progress: ChecklistProgress
): ChecklistResponse =
    ChecklistResponse(
        id = id.toString(),
        tripId = tripId.toString(),
        createdBy = createdBy.toString(),
        name = name,
        description = description,
        status = status.name,
        totalItems = progress.totalItems,
        completedItems = progress.completedItems,
        progress = progress.percentage,
        sections = sections,
        createdAt = createdAt.format(CHECKLIST_DATE_FORMATTER),
        updatedAt = updatedAt.format(CHECKLIST_DATE_FORMATTER)
    )