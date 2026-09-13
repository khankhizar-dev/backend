package com.trippoint.backend.checklist.service

import com.trippoint.backend.checklist.entity.Checklist
import com.trippoint.backend.checklist.entity.ChecklistItem
import com.trippoint.backend.checklist.entity.ChecklistSection
import com.trippoint.backend.checklist.model.ChecklistItemCategory
import com.trippoint.backend.checklist.model.ChecklistStatus
import com.trippoint.backend.checklist.repository.ChecklistItemRepository
import com.trippoint.backend.checklist.repository.ChecklistRepository
import com.trippoint.backend.checklist.repository.ChecklistSectionRepository
import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.repository.TripRepository
import com.trippoint.backend.trip.service.TripAccessService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class ChecklistServiceTest {

    private lateinit var checklistRepository: ChecklistRepository
    private lateinit var sectionRepository: ChecklistSectionRepository
    private lateinit var itemRepository: ChecklistItemRepository
    private lateinit var tripAccessService: TripAccessService

    private lateinit var service: ChecklistService

    private val userId = UUID.randomUUID()
    private val memberId = UUID.randomUUID()
    private val tripId = UUID.randomUUID()
    private val checklistId = UUID.randomUUID()
    private val sectionId = UUID.randomUUID()
    private val itemId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        checklistRepository = mock()
        sectionRepository = mock()
        itemRepository = mock()
        tripAccessService = mock()

        service = ChecklistService(
            checklistRepository = checklistRepository,
            checklistSectionRepository = sectionRepository,
            checklistItemRepository = itemRepository,
            tripAccessService = tripAccessService
        )
    }

    @Test
    fun `create checklist should create active checklist`() {

        whenever(checklistRepository.save(any<Checklist>()))
            .thenAnswer { invocation ->
                invocation.getArgument<Checklist>(0)
            }

        val result = service.createChecklist(
            userId = userId,
            tripId = tripId,
            name = "  Packing List  ",
            description = "Things to pack"
        )

        assertEquals(tripId, result.tripId)
        assertEquals(userId, result.createdBy)
        assertEquals("Packing List", result.name)
        assertEquals("Things to pack", result.description)
        assertEquals(ChecklistStatus.ACTIVE, result.status)

        verify(tripAccessService)
            .requireMemberAccess(tripId, userId)

        verify(checklistRepository)
            .save(any<Checklist>())
    }

    @Test
    fun `create checklist should reject blank name`() {
        assertThrows<IllegalArgumentException> {
            service.createChecklist(
                userId = userId,
                tripId = tripId,
                name = "   ",
                description = null
            )
        }

        verify(checklistRepository, never()).save(any())
    }

    @Test
    fun `get checklists should return active checklists`() {
        val checklist = Checklist(
            id = checklistId,
            tripId = tripId,
            createdBy = userId,
            name = "Packing"
        )

        whenever(
            checklistRepository.findAllByTripIdAndStatusOrderByCreatedAtDesc(
                tripId,
                ChecklistStatus.ACTIVE
            )
        ).thenReturn(listOf(checklist))

        val result = service.getChecklists(userId, tripId)

        assertEquals(1, result.size)
        assertEquals(checklistId, result.first().id)

        verify(tripAccessService).requireMemberAccess(tripId, userId)
    }

    @Test
    fun `get checklist should reject missing checklist`() {
        whenever(
            checklistRepository.findByIdAndTripId(checklistId, tripId)
        ).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            service.getChecklist(
                userId,
                tripId,
                checklistId
            )
        }
    }

    @Test
    fun `archive checklist should require owner access`() {
        val checklist = Checklist(
            id = checklistId,
            tripId = tripId,
            createdBy = userId,
            name = "Packing"
        )

        whenever(
            checklistRepository.findByIdAndTripId(checklistId, tripId)
        ).thenReturn(checklist)

        whenever(checklistRepository.save(any<Checklist>()))
            .thenAnswer { it.arguments[0] as Checklist }

        val result = service.archiveChecklist(
            userId,
            tripId,
            checklistId
        )

        assertEquals(ChecklistStatus.ARCHIVED, result.status)

        verify(tripAccessService)
            .requireOwnerAccess(tripId, userId)
    }

    @Test
    fun `member should not be able to archive checklist`() {

        val tripRepository = mock<TripRepository>()
        val realTripAccessService = TripAccessService(
            tripRepository = tripRepository,
            tripMemberRepository = mock()
        )

        val trip = Trip(
            id = tripId,
            ownerId = userId,
            name = "Goa Trip",
            destination = "Goa",
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 5)
        )

        whenever(
            tripRepository.findById(tripId)
        ).thenReturn(Optional.of(trip))

        val service = ChecklistService(
            checklistRepository = checklistRepository,
            checklistSectionRepository = sectionRepository,
            checklistItemRepository = itemRepository,
            tripAccessService = realTripAccessService
        )

        assertThrows<IllegalAccessException> {
            service.archiveChecklist(
                memberId,
                tripId,
                checklistId
            )
        }

        verify(tripRepository).findById(tripId)

        verify(checklistRepository, never())
            .findByIdAndTripId(any(), any())

        verify(checklistRepository, never())
            .save(any())
    }

    @Test
    fun `create section should append position`() {
        val checklist = Checklist(
            id = checklistId,
            tripId = tripId,
            createdBy = userId,
            name = "Packing"
        )

        val previousSection = ChecklistSection(
            id = UUID.randomUUID(),
            checklistId = checklistId,
            name = "Documents",
            position = 2
        )

        whenever(
            checklistRepository.findByIdAndTripId(checklistId, tripId)
        ).thenReturn(checklist)

        whenever(
            sectionRepository.findFirstByChecklistIdOrderByPositionDesc(checklistId)
        ).thenReturn(previousSection)

        whenever(sectionRepository.save(any<ChecklistSection>()))
            .thenAnswer { it.arguments[0] as ChecklistSection }

        val result = service.createSection(
            userId,
            tripId,
            checklistId,
            "Electronics"
        )

        assertEquals(3, result.position)
        assertEquals("Electronics", result.name)
    }

    @Test
    fun `add item should create incomplete item`() {
        val checklist = Checklist(
            id = checklistId,
            tripId = tripId,
            createdBy = userId,
            name = "Packing"
        )

        val section = ChecklistSection(
            id = sectionId,
            checklistId = checklistId,
            name = "Electronics",
            position = 0
        )

        whenever(
            checklistRepository.findByIdAndTripId(checklistId, tripId)
        ).thenReturn(checklist)

        whenever(
            sectionRepository.findByIdAndChecklistId(sectionId, checklistId)
        ).thenReturn(section)

        whenever(
            itemRepository.findFirstBySectionIdOrderByPositionDesc(sectionId)
        ).thenReturn(null)

        whenever(itemRepository.save(any<ChecklistItem>()))
            .thenAnswer { it.arguments[0] as ChecklistItem }

        val result = service.addItem(
            userId = userId,
            tripId = tripId,
            checklistId = checklistId,
            sectionId = sectionId,
            name = "Laptop charger",
            category = ChecklistItemCategory.ELECTRONICS,
            essential = true,
            dueDate = null
        )

        assertEquals("Laptop charger", result.name)
        assertEquals(ChecklistItemCategory.ELECTRONICS, result.category)
        assertTrue(result.essential)
        assertFalse(result.completed)
        assertEquals(0, result.position)
    }

    @Test
    fun `complete item should update completion status`() {
        val checklist = Checklist(
            id = checklistId,
            tripId = tripId,
            createdBy = userId,
            name = "Packing"
        )

        val section = ChecklistSection(
            id = sectionId,
            checklistId = checklistId,
            name = "Documents",
            position = 0
        )

        val item = ChecklistItem(
            id = itemId,
            sectionId = sectionId,
            createdBy = userId,
            name = "Passport",
            category = ChecklistItemCategory.DOCUMENTS,
            position = 0
        )

        whenever(
            checklistRepository.findByIdAndTripId(checklistId, tripId)
        ).thenReturn(checklist)

        whenever(
            sectionRepository.findByIdAndChecklistId(sectionId, checklistId)
        ).thenReturn(section)

        whenever(
            itemRepository.findByIdAndSectionId(itemId, sectionId)
        ).thenReturn(item)

        whenever(itemRepository.save(any<ChecklistItem>()))
            .thenAnswer { it.arguments[0] as ChecklistItem }

        val result = service.completeItem(
            userId,
            tripId,
            checklistId,
            sectionId,
            itemId,
            true
        )

        assertTrue(result.completed)

        verify(itemRepository).save(item)
    }

    @Test
    fun `delete item should delete existing item`() {
        val checklist = Checklist(
            id = checklistId,
            tripId = tripId,
            createdBy = userId,
            name = "Packing"
        )

        val section = ChecklistSection(
            id = sectionId,
            checklistId = checklistId,
            name = "Documents",
            position = 0
        )

        val item = ChecklistItem(
            id = itemId,
            sectionId = sectionId,
            createdBy = userId,
            name = "Passport",
            category = ChecklistItemCategory.DOCUMENTS,
            position = 0
        )

        whenever(
            checklistRepository.findByIdAndTripId(checklistId, tripId)
        ).thenReturn(checklist)

        whenever(
            sectionRepository.findByIdAndChecklistId(sectionId, checklistId)
        ).thenReturn(section)

        whenever(
            itemRepository.findByIdAndSectionId(itemId, sectionId)
        ).thenReturn(item)

        val result = service.deleteItem(
            userId,
            tripId,
            checklistId,
            sectionId,
            itemId
        )

        assertTrue(result)

        verify(itemRepository).delete(item)
    }

    @Test
    fun `progress should calculate percentage correctly`() {

        val checklist = Checklist(
            id = checklistId,
            tripId = tripId,
            name = "Travel Checklist",
            createdBy = userId,
            status = ChecklistStatus.ACTIVE
        )

        whenever(
            checklistRepository.findByIdAndTripId(
                checklistId,
                tripId
            )
        ).thenReturn(checklist)

        val section1 = ChecklistSection(
            id = UUID.randomUUID(),
            checklistId = checklistId,
            name = "Documents",
            position = 0
        )

        val section2 = ChecklistSection(
            id = UUID.randomUUID(),
            checklistId = checklistId,
            name = "Electronics",
            position = 1
        )

        whenever(
            sectionRepository.findAllByChecklistIdOrderByPositionAsc(
                checklistId
            )
        ).thenReturn(listOf(section1, section2))

        whenever(itemRepository.countBySectionId(section1.id))
            .thenReturn(4)

        whenever(
            itemRepository.countBySectionIdAndCompleted(
                section1.id,
                true
            )
        ).thenReturn(3)

        whenever(itemRepository.countBySectionId(section2.id))
            .thenReturn(6)

        whenever(
            itemRepository.countBySectionIdAndCompleted(
                section2.id,
                true
            )
        ).thenReturn(2)

        val result = service.getChecklistProgress(
            userId,
            tripId,
            checklistId
        )

        assertEquals(10, result.totalItems)
        assertEquals(5, result.completedItems)
        assertEquals(50, result.percentage)
    }

    @Test
    fun `empty checklist should have zero progress`() {

        val checklist = Checklist(
            id = checklistId,
            tripId = tripId,
            name = "Travel Checklist",
            createdBy = userId,
            status = ChecklistStatus.ACTIVE
        )

        whenever(
            checklistRepository.findByIdAndTripId(
                checklistId,
                tripId
            )
        ).thenReturn(checklist)

        whenever(
            sectionRepository.findAllByChecklistIdOrderByPositionAsc(
                checklistId
            )
        ).thenReturn(emptyList())

        val result = service.getChecklistProgress(
            userId,
            tripId,
            checklistId
        )

        assertEquals(0, result.totalItems)
        assertEquals(0, result.completedItems)
        assertEquals(0, result.percentage)
    }
}