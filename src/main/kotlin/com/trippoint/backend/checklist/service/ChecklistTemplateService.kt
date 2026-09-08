package com.trippoint.backend.checklist.service

import com.trippoint.backend.checklist.entity.Checklist
import com.trippoint.backend.checklist.entity.ChecklistItem
import com.trippoint.backend.checklist.entity.ChecklistSection
import com.trippoint.backend.checklist.entity.ChecklistTemplate
import com.trippoint.backend.checklist.entity.ChecklistTemplateItem
import com.trippoint.backend.checklist.entity.ChecklistTemplateSection
import com.trippoint.backend.checklist.model.ChecklistItemCategory
import com.trippoint.backend.checklist.model.ChecklistStatus
import com.trippoint.backend.checklist.model.ChecklistTemplateType
import com.trippoint.backend.checklist.repository.ChecklistItemRepository
import com.trippoint.backend.checklist.repository.ChecklistRepository
import com.trippoint.backend.checklist.repository.ChecklistSectionRepository
import com.trippoint.backend.checklist.repository.ChecklistTemplateItemRepository
import com.trippoint.backend.checklist.repository.ChecklistTemplateRepository
import com.trippoint.backend.checklist.repository.ChecklistTemplateSectionRepository
import com.trippoint.backend.trip.service.TripAccessService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ChecklistTemplateService(
    private val templateRepository: ChecklistTemplateRepository,
    private val templateSectionRepository: ChecklistTemplateSectionRepository,
    private val templateItemRepository: ChecklistTemplateItemRepository,
    private val checklistRepository: ChecklistRepository,
    private val checklistSectionRepository: ChecklistSectionRepository,
    private val checklistItemRepository: ChecklistItemRepository,
    private val tripAccessService: TripAccessService
) {

    // -------------------------------------------------------------------------
    // TEMPLATE
    // -------------------------------------------------------------------------

    @Transactional
    fun createTemplate(
        userId: UUID,
        name: String,
        description: String?
    ): ChecklistTemplate {

        val normalizedName = name.trim()

        require(normalizedName.isNotBlank()) {
            "Template name cannot be blank"
        }

        return templateRepository.save(
            ChecklistTemplate(
                createdBy = userId,
                name = normalizedName,
                description = description?.trim(),
                type = ChecklistTemplateType.USER,
                status = ChecklistStatus.ACTIVE
            )
        )
    }

    @Transactional(readOnly = true)
    fun getTemplates(userId: UUID): List<ChecklistTemplate> {

        val systemTemplates =
            templateRepository.findAllByTypeAndStatusOrderByCreatedAtDesc(
                type = ChecklistTemplateType.SYSTEM,
                status = ChecklistStatus.ACTIVE
            )

        val userTemplates =
            templateRepository.findAllByCreatedByAndStatusOrderByCreatedAtDesc(
                createdBy = userId,
                status = ChecklistStatus.ACTIVE
            )

        return systemTemplates + userTemplates
    }

    @Transactional(readOnly = true)
    fun getTemplate(
        userId: UUID,
        templateId: UUID
    ): ChecklistTemplate {

        val template = templateRepository.findByIdAndStatus(
            id = templateId,
            status = ChecklistStatus.ACTIVE
        ) ?: throw IllegalArgumentException("Checklist template not found")

        requireTemplateAccess(template, userId)

        return template
    }

    @Transactional
    fun updateTemplate(
        userId: UUID,
        templateId: UUID,
        name: String?,
        description: String?
    ): ChecklistTemplate {

        val template = getTemplate(userId, templateId)

        requireEditableTemplate(template, userId)

        name?.let {
            val normalizedName = it.trim()

            require(normalizedName.isNotBlank()) {
                "Template name cannot be blank"
            }

            template.name = normalizedName
        }

        description?.let {
            template.description = it.trim()
        }

        return templateRepository.save(template)
    }

    @Transactional
    fun archiveTemplate(
        userId: UUID,
        templateId: UUID
    ): ChecklistTemplate {

        val template = getTemplate(userId, templateId)

        requireEditableTemplate(template, userId)

        template.status = ChecklistStatus.ARCHIVED

        return templateRepository.save(template)
    }

    // -------------------------------------------------------------------------
    // TEMPLATE SECTIONS
    // -------------------------------------------------------------------------

    @Transactional
    fun createSection(
        userId: UUID,
        templateId: UUID,
        name: String
    ): ChecklistTemplateSection {

        val template = getTemplate(userId, templateId)

        requireEditableTemplate(template, userId)

        val normalizedName = name.trim()

        require(normalizedName.isNotBlank()) {
            "Template section name cannot be blank"
        }

        val lastSection =
            templateSectionRepository
                .findFirstByTemplateIdOrderByPositionDesc(templateId)

        val nextPosition =
            (lastSection?.position ?: -1) + 1

        return templateSectionRepository.save(
            ChecklistTemplateSection(
                templateId = templateId,
                name = normalizedName,
                position = nextPosition
            )
        )
    }

    @Transactional
    fun updateSection(
        userId: UUID,
        templateId: UUID,
        sectionId: UUID,
        name: String
    ): ChecklistTemplateSection {

        val template = getTemplate(userId, templateId)

        requireEditableTemplate(template, userId)

        val section =
            templateSectionRepository.findByIdAndTemplateId(
                id = sectionId,
                templateId = templateId
            ) ?: throw IllegalArgumentException(
                "Checklist template section not found"
            )

        val normalizedName = name.trim()

        require(normalizedName.isNotBlank()) {
            "Template section name cannot be blank"
        }

        section.name = normalizedName

        return templateSectionRepository.save(section)
    }

    @Transactional
    fun deleteSection(
        userId: UUID,
        templateId: UUID,
        sectionId: UUID
    ): Boolean {

        val template = getTemplate(userId, templateId)

        requireEditableTemplate(template, userId)

        val section =
            templateSectionRepository.findByIdAndTemplateId(
                id = sectionId,
                templateId = templateId
            ) ?: throw IllegalArgumentException(
                "Checklist template section not found"
            )

        templateSectionRepository.delete(section)

        return true
    }

    // -------------------------------------------------------------------------
    // TEMPLATE ITEMS
    // -------------------------------------------------------------------------

    @Transactional
    fun addItem(
        userId: UUID,
        templateId: UUID,
        sectionId: UUID,
        name: String,
        category: ChecklistItemCategory,
        essential: Boolean
    ): ChecklistTemplateItem {

        val template = getTemplate(userId, templateId)

        requireEditableTemplate(template, userId)

        getSection(templateId, sectionId)

        val normalizedName = name.trim()

        require(normalizedName.isNotBlank()) {
            "Template item name cannot be blank"
        }

        val lastItem =
            templateItemRepository
                .findFirstBySectionIdOrderByPositionDesc(sectionId)

        val nextPosition =
            (lastItem?.position ?: -1) + 1

        return templateItemRepository.save(
            ChecklistTemplateItem(
                sectionId = sectionId,
                name = normalizedName,
                category = category,
                essential = essential,
                position = nextPosition
            )
        )
    }

    @Transactional
    fun updateItem(
        userId: UUID,
        templateId: UUID,
        sectionId: UUID,
        itemId: UUID,
        name: String?,
        category: ChecklistItemCategory?,
        essential: Boolean?
    ): ChecklistTemplateItem {

        val template = getTemplate(userId, templateId)

        requireEditableTemplate(template, userId)

        getSection(templateId, sectionId)

        val item =
            templateItemRepository.findByIdAndSectionId(
                id = itemId,
                sectionId = sectionId
            ) ?: throw IllegalArgumentException(
                "Checklist template item not found"
            )

        name?.let {
            val normalizedName = it.trim()

            require(normalizedName.isNotBlank()) {
                "Template item name cannot be blank"
            }

            item.name = normalizedName
        }

        category?.let {
            item.category = it
        }

        essential?.let {
            item.essential = it
        }

        return templateItemRepository.save(item)
    }

    @Transactional
    fun deleteItem(
        userId: UUID,
        templateId: UUID,
        sectionId: UUID,
        itemId: UUID
    ): Boolean {

        val template = getTemplate(userId, templateId)

        requireEditableTemplate(template, userId)

        getSection(templateId, sectionId)

        val item =
            templateItemRepository.findByIdAndSectionId(
                id = itemId,
                sectionId = sectionId
            ) ?: throw IllegalArgumentException(
                "Checklist template item not found"
            )

        templateItemRepository.delete(item)

        return true
    }

    // -------------------------------------------------------------------------
    // READ STRUCTURE
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    fun getSections(
        userId: UUID,
        templateId: UUID
    ): List<ChecklistTemplateSection> {

        getTemplate(userId, templateId)

        return templateSectionRepository
            .findAllByTemplateIdOrderByPositionAsc(templateId)
    }

    @Transactional(readOnly = true)
    fun getItems(
        userId: UUID,
        templateId: UUID,
        sectionId: UUID
    ): List<ChecklistTemplateItem> {

        getTemplate(userId, templateId)

        getSection(templateId, sectionId)

        return templateItemRepository
            .findAllBySectionIdOrderByPositionAsc(sectionId)
    }

    // -------------------------------------------------------------------------
    // SAVE CHECKLIST AS TEMPLATE
    // -------------------------------------------------------------------------

    @Transactional
    fun saveChecklistAsTemplate(
        userId: UUID,
        tripId: UUID,
        checklistId: UUID,
        name: String,
        description: String?
    ): ChecklistTemplate {

        tripAccessService.requireMemberAccess(
            tripId,
            userId
        )

        val checklist =
            checklistRepository.findByIdAndTripId(
                id = checklistId,
                tripId = tripId
            ) ?: throw IllegalArgumentException(
                "Checklist not found"
            )

        require(checklist.status == ChecklistStatus.ACTIVE) {
            "Checklist is archived"
        }

        val normalizedName = name.trim()

        require(normalizedName.isNotBlank()) {
            "Template name cannot be blank"
        }

        val template =
            templateRepository.save(
                ChecklistTemplate(
                    createdBy = userId,
                    name = normalizedName,
                    description = description?.trim(),
                    type = ChecklistTemplateType.TRIP,
                    status = ChecklistStatus.ACTIVE
                )
            )

        val sections =
            checklistSectionRepository
                .findAllByChecklistIdOrderByPositionAsc(checklist.id)

        sections.forEach { sourceSection ->

            val templateSection =
                templateSectionRepository.save(
                    ChecklistTemplateSection(
                        templateId = template.id,
                        name = sourceSection.name,
                        position = sourceSection.position
                    )
                )

            val items =
                checklistItemRepository
                    .findAllBySectionIdOrderByPositionAsc(
                        sourceSection.id
                    )

            items.forEach { sourceItem ->

                templateItemRepository.save(
                    ChecklistTemplateItem(
                        sectionId = templateSection.id,
                        name = sourceItem.name,
                        category = sourceItem.category,
                        essential = sourceItem.essential,
                        position = sourceItem.position
                    )
                )
            }
        }

        return template
    }

    // -------------------------------------------------------------------------
    // CREATE CHECKLIST FROM TEMPLATE
    // -------------------------------------------------------------------------

    @Transactional
    fun createChecklistFromTemplate(
        userId: UUID,
        tripId: UUID,
        templateId: UUID,
        name: String?,
        description: String?
    ): Checklist {

        tripAccessService.requireMemberAccess(
            tripId,
            userId
        )

        val template =
            getTemplate(
                userId = userId,
                templateId = templateId
            )

        val checklist =
            checklistRepository.save(
                Checklist(
                    tripId = tripId,
                    createdBy = userId,
                    name = name?.trim()?.takeIf { it.isNotBlank() }
                        ?: template.name,
                    description = description?.trim()
                        ?: template.description,
                    status = ChecklistStatus.ACTIVE
                )
            )

        val sections =
            templateSectionRepository
                .findAllByTemplateIdOrderByPositionAsc(template.id)

        sections.forEach { sourceSection ->

            val checklistSection =
                checklistSectionRepository.save(
                    ChecklistSection(
                        checklistId = checklist.id,
                        name = sourceSection.name,
                        position = sourceSection.position
                    )
                )

            val items =
                templateItemRepository
                    .findAllBySectionIdOrderByPositionAsc(
                        sourceSection.id
                    )

            items.forEach { sourceItem ->

                checklistItemRepository.save(
                    ChecklistItem(
                        sectionId = checklistSection.id,
                        createdBy = userId,
                        name = sourceItem.name,
                        category = sourceItem.category,
                        essential = sourceItem.essential,
                        completed = false,
                        dueDate = null,
                        position = sourceItem.position
                    )
                )
            }
        }

        return checklist
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    private fun requireTemplateAccess(
        template: ChecklistTemplate,
        userId: UUID
    ) {

        if (template.type == ChecklistTemplateType.SYSTEM) {
            return
        }

        if (template.createdBy != userId) {
            throw IllegalAccessException(
                "You do not have access to this checklist template"
            )
        }
    }

    private fun requireEditableTemplate(
        template: ChecklistTemplate,
        userId: UUID
    ) {

        require(template.type != ChecklistTemplateType.SYSTEM) {
            "System templates cannot be modified"
        }

        require(template.createdBy == userId) {
            "Only the template creator can modify this template"
        }
    }

    private fun getSection(
        templateId: UUID,
        sectionId: UUID
    ): ChecklistTemplateSection {

        return templateSectionRepository
            .findByIdAndTemplateId(
                id = sectionId,
                templateId = templateId
            )
            ?: throw IllegalArgumentException(
                "Checklist template section not found"
            )
    }
}