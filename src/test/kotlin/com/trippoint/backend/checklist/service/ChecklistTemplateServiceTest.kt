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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class ChecklistTemplateServiceTest {

    private lateinit var templateRepository: ChecklistTemplateRepository
    private lateinit var templateSectionRepository: ChecklistTemplateSectionRepository
    private lateinit var templateItemRepository: ChecklistTemplateItemRepository
    private lateinit var checklistRepository: ChecklistRepository
    private lateinit var checklistSectionRepository: ChecklistSectionRepository
    private lateinit var checklistItemRepository: ChecklistItemRepository
    private lateinit var tripAccessService: TripAccessService

    private lateinit var service: ChecklistTemplateService

    private val userId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()
    private val tripId = UUID.randomUUID()
    private val templateId = UUID.randomUUID()
    private val sectionId = UUID.randomUUID()
    private val itemId = UUID.randomUUID()
    private val checklistId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        templateRepository = mock()
        templateSectionRepository = mock()
        templateItemRepository = mock()
        checklistRepository = mock()
        checklistSectionRepository = mock()
        checklistItemRepository = mock()
        tripAccessService = mock()

        service = ChecklistTemplateService(
            templateRepository = templateRepository,
            templateSectionRepository = templateSectionRepository,
            templateItemRepository = templateItemRepository,
            checklistRepository = checklistRepository,
            checklistSectionRepository = checklistSectionRepository,
            checklistItemRepository = checklistItemRepository,
            tripAccessService = tripAccessService
        )
    }

    @Test
    fun `create template should create user template`() {
        whenever(templateRepository.save(any<ChecklistTemplate>()))
            .thenAnswer { it.arguments[0] as ChecklistTemplate }

        val result = service.createTemplate(
            userId = userId,
            name = "  Europe Packing  ",
            description = "Things to pack"
        )

        assertEquals(userId, result.createdBy)
        assertEquals("Europe Packing", result.name)
        assertEquals("Things to pack", result.description)
        assertEquals(ChecklistTemplateType.USER, result.type)
        assertEquals(ChecklistStatus.ACTIVE, result.status)

        verify(templateRepository).save(any<ChecklistTemplate>())
    }

    @Test
    fun `create template should reject blank name`() {
        assertThrows<IllegalArgumentException> {
            service.createTemplate(
                userId = userId,
                name = "   ",
                description = null
            )
        }

        verify(templateRepository, never())
            .save(any<ChecklistTemplate>())
    }

    @Test
    fun `system template should be readable by any user`() {
        val template = ChecklistTemplate(
            id = templateId,
            createdBy = null,
            name = "International Travel",
            type = ChecklistTemplateType.SYSTEM,
            status = ChecklistStatus.ACTIVE
        )

        whenever(
            templateRepository.findByIdAndStatus(
                templateId,
                ChecklistStatus.ACTIVE
            )
        ).thenReturn(template)

        val result = service.getTemplate(
            userId = userId,
            templateId = templateId
        )

        assertEquals(templateId, result.id)
        assertEquals(ChecklistTemplateType.SYSTEM, result.type)
    }

    @Test
    fun `user should be able to access own template`() {
        val template = ChecklistTemplate(
            id = templateId,
            createdBy = userId,
            name = "My Template",
            type = ChecklistTemplateType.USER,
            status = ChecklistStatus.ACTIVE
        )

        whenever(
            templateRepository.findByIdAndStatus(
                templateId,
                ChecklistStatus.ACTIVE
            )
        ).thenReturn(template)

        val result = service.getTemplate(
            userId = userId,
            templateId = templateId
        )

        assertEquals(templateId, result.id)
    }

    @Test
    fun `user should not access another user's template`() {
        val template = ChecklistTemplate(
            id = templateId,
            createdBy = otherUserId,
            name = "Private Template",
            type = ChecklistTemplateType.USER,
            status = ChecklistStatus.ACTIVE
        )

        whenever(
            templateRepository.findByIdAndStatus(
                templateId,
                ChecklistStatus.ACTIVE
            )
        ).thenReturn(template)

        assertThrows<IllegalAccessException> {
            service.getTemplate(
                userId = userId,
                templateId = templateId
            )
        }
    }

    @Test
    fun `system template should not be editable`() {
        val template = ChecklistTemplate(
            id = templateId,
            createdBy = null,
            name = "System Template",
            type = ChecklistTemplateType.SYSTEM,
            status = ChecklistStatus.ACTIVE
        )

        whenever(
            templateRepository.findByIdAndStatus(
                templateId,
                ChecklistStatus.ACTIVE
            )
        ).thenReturn(template)

        assertThrows<IllegalArgumentException> {
            service.updateTemplate(
                userId = userId,
                templateId = templateId,
                name = "Changed",
                description = null
            )
        }

        verify(templateRepository, never())
            .save(any<ChecklistTemplate>())
    }

    @Test
    fun `user should be able to update own template`() {
        val template = ChecklistTemplate(
            id = templateId,
            createdBy = userId,
            name = "Old Name",
            description = "Old Description",
            type = ChecklistTemplateType.USER,
            status = ChecklistStatus.ACTIVE
        )

        whenever(
            templateRepository.findByIdAndStatus(
                templateId,
                ChecklistStatus.ACTIVE
            )
        ).thenReturn(template)

        whenever(templateRepository.save(any<ChecklistTemplate>()))
            .thenAnswer { it.arguments[0] as ChecklistTemplate }

        val result = service.updateTemplate(
            userId = userId,
            templateId = templateId,
            name = "  New Name  ",
            description = "New Description"
        )

        assertEquals("New Name", result.name)
        assertEquals("New Description", result.description)

        verify(templateRepository).save(template)
    }

    @Test
    fun `create template section should append position`() {
        val template = ChecklistTemplate(
            id = templateId,
            createdBy = userId,
            name = "Travel",
            type = ChecklistTemplateType.USER,
            status = ChecklistStatus.ACTIVE
        )

        val previousSection = ChecklistTemplateSection(
            id = UUID.randomUUID(),
            templateId = templateId,
            name = "Documents",
            position = 2
        )

        whenever(
            templateRepository.findByIdAndStatus(
                templateId,
                ChecklistStatus.ACTIVE
            )
        ).thenReturn(template)

        whenever(
            templateSectionRepository
                .findFirstByTemplateIdOrderByPositionDesc(templateId)
        ).thenReturn(previousSection)

        whenever(templateSectionRepository.save(any<ChecklistTemplateSection>()))
            .thenAnswer { it.arguments[0] as ChecklistTemplateSection }

        val result = service.createSection(
            userId = userId,
            templateId = templateId,
            name = "Electronics"
        )

        assertEquals(3, result.position)
        assertEquals("Electronics", result.name)
    }

    @Test
    fun `add template item should create item`() {
        val template = ChecklistTemplate(
            id = templateId,
            createdBy = userId,
            name = "Travel",
            type = ChecklistTemplateType.USER,
            status = ChecklistStatus.ACTIVE
        )

        val section = ChecklistTemplateSection(
            id = sectionId,
            templateId = templateId,
            name = "Documents",
            position = 0
        )

        whenever(
            templateRepository.findByIdAndStatus(
                templateId,
                ChecklistStatus.ACTIVE
            )
        ).thenReturn(template)

        whenever(
            templateSectionRepository.findByIdAndTemplateId(
                sectionId,
                templateId
            )
        ).thenReturn(section)

        whenever(
            templateItemRepository
                .findFirstBySectionIdOrderByPositionDesc(sectionId)
        ).thenReturn(null)

        whenever(templateItemRepository.save(any<ChecklistTemplateItem>()))
            .thenAnswer { it.arguments[0] as ChecklistTemplateItem }

        val result = service.addItem(
            userId = userId,
            templateId = templateId,
            sectionId = sectionId,
            name = "Passport",
            category = ChecklistItemCategory.DOCUMENTS,
            essential = true
        )

        assertEquals("Passport", result.name)
        assertEquals(ChecklistItemCategory.DOCUMENTS, result.category)
        assertTrue(result.essential)
        assertEquals(0, result.position)
    }

    @Test
    fun `archive template should archive own template`() {
        val template = ChecklistTemplate(
            id = templateId,
            createdBy = userId,
            name = "Travel",
            type = ChecklistTemplateType.USER,
            status = ChecklistStatus.ACTIVE
        )

        whenever(
            templateRepository.findByIdAndStatus(
                templateId,
                ChecklistStatus.ACTIVE
            )
        ).thenReturn(template)

        whenever(templateRepository.save(any<ChecklistTemplate>()))
            .thenAnswer { it.arguments[0] as ChecklistTemplate }

        val result = service.archiveTemplate(
            userId = userId,
            templateId = templateId
        )

        assertEquals(ChecklistStatus.ARCHIVED, result.status)

        verify(templateRepository).save(template)
    }

    @Test
    fun `save checklist as template should copy sections and items`() {
        val checklist = Checklist(
            id = checklistId,
            tripId = tripId,
            createdBy = userId,
            name = "Europe Trip",
            status = ChecklistStatus.ACTIVE
        )

        val sourceSection = ChecklistSection(
            id = sectionId,
            checklistId = checklistId,
            name = "Documents",
            position = 0
        )

        val sourceItem = ChecklistItem(
            id = itemId,
            sectionId = sectionId,
            createdBy = userId,
            name = "Passport",
            category = ChecklistItemCategory.DOCUMENTS,
            essential = true,
            completed = true,
            position = 0
        )

        whenever(
            checklistRepository.findByIdAndTripId(
                checklistId,
                tripId
            )
        ).thenReturn(checklist)

        whenever(
            templateRepository.save(any<ChecklistTemplate>())
        ).thenAnswer { it.arguments[0] as ChecklistTemplate }

        whenever(
            checklistSectionRepository
                .findAllByChecklistIdOrderByPositionAsc(checklistId)
        ).thenReturn(listOf(sourceSection))

        whenever(
            checklistItemRepository
                .findAllBySectionIdOrderByPositionAsc(sectionId)
        ).thenReturn(listOf(sourceItem))

        whenever(
            templateSectionRepository.save(any<ChecklistTemplateSection>())
        ).thenAnswer { it.arguments[0] as ChecklistTemplateSection }

        whenever(
            templateItemRepository.save(any<ChecklistTemplateItem>())
        ).thenAnswer { it.arguments[0] as ChecklistTemplateItem }

        val result = service.saveChecklistAsTemplate(
            userId = userId,
            tripId = tripId,
            checklistId = checklistId,
            name = "Europe Template",
            description = "Reusable list"
        )

        assertEquals("Europe Template", result.name)
        assertEquals(ChecklistTemplateType.TRIP, result.type)
        assertEquals(userId, result.createdBy)

        verify(templateRepository).save(any<ChecklistTemplate>())
        verify(templateSectionRepository).save(any<ChecklistTemplateSection>())
        verify(templateItemRepository).save(any<ChecklistTemplateItem>())
    }

    @Test
    fun `create checklist from template should reset runtime item state`() {
        val template = ChecklistTemplate(
            id = templateId,
            createdBy = userId,
            name = "Europe Packing",
            description = "Packing list",
            type = ChecklistTemplateType.USER,
            status = ChecklistStatus.ACTIVE
        )

        val templateSection = ChecklistTemplateSection(
            id = UUID.randomUUID(),
            templateId = templateId,
            name = "Documents",
            position = 0
        )

        val templateItem = ChecklistTemplateItem(
            id = UUID.randomUUID(),
            sectionId = templateSection.id,
            name = "Passport",
            category = ChecklistItemCategory.DOCUMENTS,
            essential = true,
            position = 0
        )

        whenever(
            templateRepository.findByIdAndStatus(
                templateId,
                ChecklistStatus.ACTIVE
            )
        ).thenReturn(template)

        whenever(
            checklistRepository.save(any<Checklist>())
        ).thenAnswer { invocation ->
            val checklist = invocation.arguments[0] as Checklist
            checklist.id = checklistId
            checklist
        }

        whenever(
            templateSectionRepository
                .findAllByTemplateIdOrderByPositionAsc(templateId)
        ).thenReturn(listOf(templateSection))

        whenever(
            templateItemRepository
                .findAllBySectionIdOrderByPositionAsc(templateSection.id)
        ).thenReturn(listOf(templateItem))

        whenever(
            checklistSectionRepository.save(any<ChecklistSection>())
        ).thenAnswer { invocation ->
            invocation.arguments[0] as ChecklistSection
        }

        whenever(
            checklistItemRepository.save(any<ChecklistItem>())
        ).thenAnswer { invocation ->
            invocation.arguments[0] as ChecklistItem
        }

        val result = service.createChecklistFromTemplate(
            userId = userId,
            tripId = tripId,
            templateId = templateId,
            name = null,
            description = null
        )

        assertEquals("Europe Packing", result.name)
        assertEquals("Packing list", result.description)

        val itemCaptor =
            org.mockito.kotlin.argumentCaptor<ChecklistItem>()

        verify(checklistItemRepository).save(itemCaptor.capture())

        val createdItem = itemCaptor.firstValue

        assertEquals("Passport", createdItem.name)
        assertEquals(ChecklistItemCategory.DOCUMENTS, createdItem.category)
        assertTrue(createdItem.essential)
        assertFalse(createdItem.completed)
        assertEquals(null, createdItem.dueDate)
        assertEquals(0, createdItem.position)
    }
}