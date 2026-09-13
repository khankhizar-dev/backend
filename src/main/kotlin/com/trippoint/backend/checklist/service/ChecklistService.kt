package com.trippoint.backend.checklist.service

import com.trippoint.backend.checklist.entity.Checklist
import com.trippoint.backend.checklist.entity.ChecklistItem
import com.trippoint.backend.checklist.entity.ChecklistSection
import com.trippoint.backend.checklist.model.ChecklistItemCategory
import com.trippoint.backend.checklist.model.ChecklistStatus
import com.trippoint.backend.checklist.repository.ChecklistItemRepository
import com.trippoint.backend.checklist.repository.ChecklistRepository
import com.trippoint.backend.checklist.repository.ChecklistSectionRepository
import com.trippoint.backend.trip.service.TripAccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ChecklistService(
    private val checklistRepository: ChecklistRepository,
    private val checklistSectionRepository: ChecklistSectionRepository,
    private val checklistItemRepository: ChecklistItemRepository,
    private val tripAccessService: TripAccessService
) {

    // -------------------------------------------------------------------------
    // CHECKLIST
    // -------------------------------------------------------------------------

    @Transactional
    fun createChecklist(
        userId: UUID,
        tripId: UUID,
        name: String,
        description: String?
    ): Checklist {

        tripAccessService.requireMemberAccess(tripId, userId)

        val normalizedName = name.trim()

        require(normalizedName.isNotBlank()) {
            "Checklist name cannot be blank"
        }

        val checklist = Checklist(
            tripId = tripId,
            createdBy = userId,
            name = normalizedName,
            description = description?.trim(),
            status = ChecklistStatus.ACTIVE
        )

        return checklistRepository.save(checklist)
    }

    @Transactional(readOnly = true)
    fun getChecklists(
        userId: UUID,
        tripId: UUID
    ): List<Checklist> {

        tripAccessService.requireMemberAccess(tripId, userId)

        return checklistRepository
            .findAllByTripIdAndStatusOrderByCreatedAtDesc(
                tripId = tripId,
                status = ChecklistStatus.ACTIVE
            )
    }

    @Transactional(readOnly = true)
    fun getChecklist(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID
    ): Checklist {

        tripAccessService.requireMemberAccess(tripId, userId)

        return checklistRepository
            .findByIdAndTripId(checklistId, tripId)
            ?: throw IllegalArgumentException("Checklist not found")
    }

    @Transactional
    fun updateChecklist(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID,
        name: String?,
        description: String?
    ): Checklist {

        tripAccessService.requireMemberAccess(tripId, userId)

        val checklist = getActiveChecklist(
            tripId = tripId,
            checklistId = checklistId
        )

        name?.let {
            val normalizedName = it.trim()

            require(normalizedName.isNotBlank()) {
                "Checklist name cannot be blank"
            }

            checklist.name = normalizedName
        }

        description?.let {
            checklist.description = it.trim()
        }

        return checklistRepository.save(checklist)
    }

    @Transactional
    fun archiveChecklist(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID
    ): Checklist {

        tripAccessService.requireOwnerAccess(tripId, userId)

        val checklist = checklistRepository
            .findByIdAndTripId(checklistId, tripId)
            ?: throw IllegalArgumentException("Checklist not found")

        require(checklist.status != ChecklistStatus.ARCHIVED) {
            "Checklist is already archived"
        }

        checklist.status = ChecklistStatus.ARCHIVED

        return checklistRepository.save(checklist)
    }

    // -------------------------------------------------------------------------
    // SECTIONS
    // -------------------------------------------------------------------------

    @Transactional
    fun createSection(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID,
        name: String
    ): ChecklistSection {

        tripAccessService.requireMemberAccess(tripId, userId)

        getActiveChecklist(
            tripId = tripId,
            checklistId = checklistId
        )

        val normalizedName = name.trim()

        require(normalizedName.isNotBlank()) {
            "Section name cannot be blank"
        }

        val lastSection =
            checklistSectionRepository
                .findFirstByChecklistIdOrderByPositionDesc(checklistId)

        val nextPosition = (lastSection?.position ?: -1) + 1

        val section = ChecklistSection(
            checklistId = checklistId,
            name = normalizedName,
            position = nextPosition
        )

        return checklistSectionRepository.save(section)
    }

    @Transactional
    fun updateSection(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID,
        sectionId: UUID,
        name: String
    ): ChecklistSection {

        tripAccessService.requireMemberAccess(tripId, userId)

        getActiveChecklist(
            tripId = tripId,
            checklistId = checklistId
        )

        val section = checklistSectionRepository
            .findByIdAndChecklistId(sectionId, checklistId)
            ?: throw IllegalArgumentException("Checklist section not found")

        val normalizedName = name.trim()

        require(normalizedName.isNotBlank()) {
            "Section name cannot be blank"
        }

        section.name = normalizedName

        return checklistSectionRepository.save(section)
    }

    @Transactional
    fun deleteSection(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID,
        sectionId: UUID
    ): Boolean {

        tripAccessService.requireMemberAccess(tripId, userId)

        getActiveChecklist(
            tripId = tripId,
            checklistId = checklistId
        )

        val section = checklistSectionRepository
            .findByIdAndChecklistId(sectionId, checklistId)
            ?: throw IllegalArgumentException("Checklist section not found")

        checklistSectionRepository.delete(section)

        return true
    }

    // -------------------------------------------------------------------------
    // ITEMS
    // -------------------------------------------------------------------------

    @Transactional
    fun addItem(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID,
        sectionId: UUID,
        name: String,
        category: ChecklistItemCategory,
        essential: Boolean,
        dueDate: LocalDateTime?
    ): ChecklistItem {

        tripAccessService.requireMemberAccess(tripId, userId)

        getActiveChecklist(
            tripId = tripId,
            checklistId = checklistId
        )

        getSection(
            checklistId = checklistId,
            sectionId = sectionId
        )

        val normalizedName = name.trim()

        require(normalizedName.isNotBlank()) {
            "Checklist item name cannot be blank"
        }

        val lastItem =
            checklistItemRepository
                .findFirstBySectionIdOrderByPositionDesc(sectionId)

        val nextPosition = (lastItem?.position ?: -1) + 1

        val item = ChecklistItem(
            sectionId = sectionId,
            createdBy = userId,
            name = normalizedName,
            category = category,
            essential = essential,
            completed = false,
            dueDate = dueDate,
            position = nextPosition
        )

        return checklistItemRepository.save(item)
    }

    @Transactional
    fun updateItem(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID,
        sectionId: UUID,
        itemId: UUID,
        name: String?,
        category: ChecklistItemCategory?,
        essential: Boolean?,
        dueDate: LocalDateTime?
    ): ChecklistItem {

        tripAccessService.requireMemberAccess(tripId, userId)

        getActiveChecklist(
            tripId = tripId,
            checklistId = checklistId
        )

        getSection(
            checklistId = checklistId,
            sectionId = sectionId
        )

        val item = checklistItemRepository
            .findByIdAndSectionId(itemId, sectionId)
            ?: throw IllegalArgumentException("Checklist item not found")

        name?.let {
            val normalizedName = it.trim()

            require(normalizedName.isNotBlank()) {
                "Checklist item name cannot be blank"
            }

            item.name = normalizedName
        }

        category?.let {
            item.category = it
        }

        essential?.let {
            item.essential = it
        }

        dueDate?.let {
            item.dueDate = it
        }

        return checklistItemRepository.save(item)
    }

    @Transactional
    fun completeItem(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID,
        sectionId: UUID,
        itemId: UUID,
        completed: Boolean
    ): ChecklistItem {

        tripAccessService.requireMemberAccess(tripId, userId)

        getActiveChecklist(
            tripId = tripId,
            checklistId = checklistId
        )

        getSection(
            checklistId = checklistId,
            sectionId = sectionId
        )

        val item = checklistItemRepository
            .findByIdAndSectionId(itemId, sectionId)
            ?: throw IllegalArgumentException("Checklist item not found")

        item.completed = completed

        return checklistItemRepository.save(item)
    }

    @Transactional
    fun deleteItem(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID,
        sectionId: UUID,
        itemId: UUID
    ): Boolean {

        tripAccessService.requireMemberAccess(tripId, userId)

        getActiveChecklist(
            tripId = tripId,
            checklistId = checklistId
        )

        getSection(
            checklistId = checklistId,
            sectionId = sectionId
        )

        val item = checklistItemRepository
            .findByIdAndSectionId(itemId, sectionId)
            ?: throw IllegalArgumentException("Checklist item not found")

        checklistItemRepository.delete(item)

        return true
    }

    // -------------------------------------------------------------------------
    // READ SECTIONS / ITEMS
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    fun getSections(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID
    ): List<ChecklistSection> {

        tripAccessService.requireMemberAccess(tripId, userId)

        getChecklist(
            userId = userId,
            tripId = tripId,
            checklistId = checklistId
        )

        return checklistSectionRepository
            .findAllByChecklistIdOrderByPositionAsc(checklistId)
    }

    @Transactional(readOnly = true)
    fun getItems(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID,
        sectionId: UUID
    ): List<ChecklistItem> {

        tripAccessService.requireMemberAccess(tripId, userId)

        getActiveChecklist(
            tripId = tripId,
            checklistId = checklistId
        )

        getSection(
            checklistId = checklistId,
            sectionId = sectionId
        )

        return checklistItemRepository
            .findAllBySectionIdOrderByPositionAsc(sectionId)
    }

    // -------------------------------------------------------------------------
    // PROGRESS
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    fun getChecklistProgress(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID
    ): ChecklistProgress {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        getActiveChecklist(
            tripId = tripId,
            checklistId = checklistId
        )

        val sections = checklistSectionRepository
            .findAllByChecklistIdOrderByPositionAsc(checklistId)

        if (sections.isEmpty()) {
            return ChecklistProgress(
                totalItems = 0,
                completedItems = 0,
                percentage = 0
            )
        }

        val totalItems = sections.sumOf { section ->
            checklistItemRepository
                .countBySectionId(section.id)
        }

        val completedItems = sections.sumOf { section ->
            checklistItemRepository
                .countBySectionIdAndCompleted(
                    section.id,
                    true
                )
        }

        val percentage =
            if (totalItems == 0L) {
                0
            } else {
                ((completedItems * 100) / totalItems).toInt()
            }

        return ChecklistProgress(
            totalItems = totalItems.toInt(),
            completedItems = completedItems.toInt(),
            percentage = percentage
        )
    }

    @Transactional(readOnly = true)
    fun getSectionProgress(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID,
        sectionId: UUID
    ): ChecklistProgress {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        getActiveChecklist(
            tripId = tripId,
            checklistId = checklistId
        )

        getSection(
            checklistId = checklistId,
            sectionId = sectionId
        )

        val totalItems =
            checklistItemRepository.countBySectionId(sectionId)

        val completedItems =
            checklistItemRepository.countBySectionIdAndCompleted(
                sectionId,
                true
            )

        val percentage =
            if (totalItems == 0L) {
                0
            } else {
                ((completedItems * 100) / totalItems).toInt()
            }

        return ChecklistProgress(
            totalItems = totalItems.toInt(),
            completedItems = completedItems.toInt(),
            percentage = percentage
        )
    }

    // -------------------------------------------------------------------------
    // INTERNAL HELPERS
    // -------------------------------------------------------------------------

    private fun getActiveChecklist(
        tripId: UUID,
        checklistId: UUID
    ): Checklist {

        val checklist = checklistRepository
            .findByIdAndTripId(checklistId, tripId)
            ?: throw IllegalArgumentException("Checklist not found")

        require(checklist.status == ChecklistStatus.ACTIVE) {
            "Checklist is archived"
        }

        return checklist
    }

    private fun getSection(
        checklistId: UUID,
        sectionId: UUID
    ): ChecklistSection {

        return checklistSectionRepository
            .findByIdAndChecklistId(sectionId, checklistId)
            ?: throw IllegalArgumentException("Checklist section not found")
    }
}

data class ChecklistProgress(
    val totalItems: Int,
    val completedItems: Int,
    val percentage: Int
)