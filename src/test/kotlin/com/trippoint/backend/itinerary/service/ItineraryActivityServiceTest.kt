package com.trippoint.backend.itinerary.service

import com.trippoint.backend.itinerary.entity.ItineraryActivity
import com.trippoint.backend.itinerary.entity.ItineraryDay
import com.trippoint.backend.itinerary.graphql.input.CreateItineraryActivityInput
import com.trippoint.backend.itinerary.graphql.input.UpdateItineraryActivityInput
import com.trippoint.backend.itinerary.model.ActivityType
import com.trippoint.backend.itinerary.repository.ItineraryActivityRepository
import com.trippoint.backend.itinerary.repository.ItineraryDayRepository
import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.entity.TripMember
import com.trippoint.backend.trip.model.TripMemberRole
import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import com.trippoint.backend.trip.service.TripAccessService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class ItineraryActivityServiceTest {

    private lateinit var activityRepository: ItineraryActivityRepository
    private lateinit var itineraryDayRepository: ItineraryDayRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var tripMemberRepository: TripMemberRepository

    private lateinit var service: ItineraryActivityService

    private val userId = UUID.randomUUID()
    private val tripId = UUID.randomUUID()
    private val itineraryDayId = UUID.randomUUID()
    private val activityId = UUID.randomUUID()

    @BeforeEach
    fun setup() {

        activityRepository = mockk()
        itineraryDayRepository = mockk()
        tripRepository = mockk()
        tripMemberRepository = mockk()

        val tripAccessService = TripAccessService(
            tripRepository = tripRepository,
            tripMemberRepository = tripMemberRepository
        )

        service = ItineraryActivityService(
            activityRepository,
            itineraryDayRepository,
            tripAccessService = tripAccessService
        )
    }

    private fun trip(): Trip =
        Trip(
            id = tripId,
            ownerId = userId,
            name = "Dubai Trip",
            destination = "Dubai",
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 5)
        )

    private fun itineraryDay(): ItineraryDay =
        ItineraryDay(
            id = itineraryDayId,
            tripId = tripId,
            dayNumber = 1,
            date = LocalDate.of(2026, 10, 1)
        )

    private fun activity(): ItineraryActivity =
        ItineraryActivity(
            id = activityId,
            itineraryDayId = itineraryDayId,
            title = "Burj Khalifa",
            description = "Visit observation deck",
            type = ActivityType.SIGHTSEEING,
            startTime = LocalTime.of(18, 0),
            endTime = LocalTime.of(20, 0),
            location = "Downtown Dubai",
            latitude = 25.1972,
            longitude = 55.2744,
            sortOrder = 1,
            completed = false
        )

    // ---------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------

    @Test
    fun `create activity successfully`() {

        val input = CreateItineraryActivityInput(
            title = "Burj Khalifa",
            description = "Visit observation deck",
            type = ActivityType.SIGHTSEEING,
            startTime = "18:00",
            endTime = "20:00",
            location = "Downtown Dubai",
            latitude = 25.1972,
            longitude = 55.2744,
            sortOrder = 1
        )

        every {
            tripRepository.findById(
                tripId
            )
        } returns Optional.of(trip())

        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.of(itineraryDay())

        every {
            activityRepository.save(any())
        } answers {
            firstArg()
        }

        val result = service.createActivity(
            userId,
            tripId,
            itineraryDayId,
            input
        )

        assertEquals("Burj Khalifa", result.title)
        assertEquals(ActivityType.SIGHTSEEING, result.type)
        assertEquals(LocalTime.of(18, 0), result.startTime)
        assertEquals(LocalTime.of(20, 0), result.endTime)
        assertEquals(1, result.sortOrder)
        assertFalse(result.completed)

        verify(exactly = 1) {
            activityRepository.save(any())
        }
    }

    @Test
    fun `create activity rejects non owner`() {

        every {
            tripRepository.findById(
                tripId
            )
        } returns Optional.empty()

        val input = CreateItineraryActivityInput(
            title = "Burj Khalifa",
            description = null,
            type = ActivityType.SIGHTSEEING,
            startTime = null,
            endTime = null,
            location = null,
            latitude = null,
            longitude = null,
            sortOrder = null
        )

        assertThrows<IllegalArgumentException> {
            service.createActivity(
                userId,
                tripId,
                itineraryDayId,
                input
            )
        }

        verify(exactly = 0) {
            activityRepository.save(any())
        }
    }

    @Test
    fun `create activity rejects day belonging to another trip`() {

        val wrongDay = itineraryDay().apply {
            tripId = UUID.randomUUID()
        }

        every {
            tripRepository.findById(
                tripId
            )
        } returns Optional.empty()

        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.of(wrongDay)

        val input = CreateItineraryActivityInput(
            title = "Burj Khalifa",
            description = null,
            type = ActivityType.SIGHTSEEING,
            startTime = null,
            endTime = null,
            location = null,
            latitude = null,
            longitude = null,
            sortOrder = null
        )

        assertThrows<IllegalArgumentException> {
            service.createActivity(
                userId,
                tripId,
                itineraryDayId,
                input
            )
        }

        verify(exactly = 0) {
            activityRepository.save(any())
        }
    }

    @Test
    fun `create activity rejects blank title`() {

        every {
            tripRepository.findById(
                tripId
            )
        } returns Optional.of(trip())

        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.of(itineraryDay())

        val input = CreateItineraryActivityInput(
            title = "   ",
            description = null,
            type = ActivityType.SIGHTSEEING,
            startTime = null,
            endTime = null,
            location = null,
            latitude = null,
            longitude = null,
            sortOrder = null
        )

        assertThrows<IllegalArgumentException> {
            service.createActivity(
                userId,
                tripId,
                itineraryDayId,
                input
            )
        }
    }

    @Test
    fun `create activity rejects end time before start time`() {

        every {
            tripRepository.findById(
                tripId
            )
        } returns Optional.of(trip())

        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.of(itineraryDay())

        val input = CreateItineraryActivityInput(
            title = "Dinner",
            description = null,
            type = ActivityType.MEAL,
            startTime = "20:00",
            endTime = "18:00",
            location = null,
            latitude = null,
            longitude = null,
            sortOrder = null
        )

        assertThrows<IllegalArgumentException> {
            service.createActivity(
                userId,
                tripId,
                itineraryDayId,
                input
            )
        }
    }

    @Test
    fun `create activity rejects negative sort order`() {

        every {
            tripRepository.findById(
                tripId
            )
        } returns Optional.of(trip())

        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.of(itineraryDay())

        val input = CreateItineraryActivityInput(
            title = "Dinner",
            description = null,
            type = ActivityType.MEAL,
            startTime = null,
            endTime = null,
            location = null,
            latitude = null,
            longitude = null,
            sortOrder = -1
        )

        assertThrows<IllegalArgumentException> {
            service.createActivity(
                userId,
                tripId,
                itineraryDayId,
                input
            )
        }
    }

    @Test
    fun `create activity rejects invalid latitude`() {

        every {
            tripRepository.findById(
                tripId
            )
        } returns Optional.of(trip())

        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.of(itineraryDay())

        val input = CreateItineraryActivityInput(
            title = "Location",
            description = null,
            type = ActivityType.SIGHTSEEING,
            startTime = null,
            endTime = null,
            location = null,
            latitude = 100.0,
            longitude = 50.0,
            sortOrder = null
        )

        assertThrows<IllegalArgumentException> {
            service.createActivity(
                userId,
                tripId,
                itineraryDayId,
                input
            )
        }
    }

@Test
fun `create activity rejects invalid longitude`() {

    every {
        tripRepository.findById(
            tripId
        )
    } returns Optional.of(trip())

    every {
        itineraryDayRepository.findById(itineraryDayId)
    } returns Optional.of(itineraryDay())

    val input = CreateItineraryActivityInput(
        title = "Location",
        description = null,
        type = ActivityType.SIGHTSEEING,
        startTime = null,
        endTime = null,
        location = null,
        latitude = 25.0,
        longitude = 200.0,
        sortOrder = null
    )

    assertThrows<IllegalArgumentException> {
        service.createActivity(
            userId,
            tripId,
            itineraryDayId,
            input
        )
    }
}


// ---------------------------------------------------------
// GET ACTIVITIES
// ---------------------------------------------------------

@Test
fun `get activities successfully`() {

    every {
        tripRepository.findById(
            tripId
        )
    } returns Optional.of(trip())

    every {
        itineraryDayRepository.findById(itineraryDayId)
    } returns Optional.of(itineraryDay())

    every {
        activityRepository.findAllByItineraryDayIdOrderBySortOrderAsc(
            itineraryDayId
        )
    } returns listOf(activity())

    val result = service.getActivities(
        userId,
        tripId,
        itineraryDayId
    )

    assertEquals(1, result.size)
    assertEquals(activityId, result[0].id)
    assertEquals("Burj Khalifa", result[0].title)
}

@Test
fun `get activities rejects unauthorized trip`() {

    every {
        tripRepository.findById(
            tripId
        )
    } returns Optional.empty()

    assertThrows<IllegalArgumentException> {
        service.getActivities(
            userId,
            tripId,
            itineraryDayId
        )
    }

    verify(exactly = 0) {
        activityRepository.findAllByItineraryDayIdOrderBySortOrderAsc(any())
    }
}

// ---------------------------------------------------------
// GET ACTIVITY
// ---------------------------------------------------------

@Test
fun `get activity successfully`() {

    every {
        tripRepository.findById(
            tripId
        )
    } returns Optional.of(trip())

    every {
        itineraryDayRepository.findById(itineraryDayId)
    } returns Optional.of(itineraryDay())

    every {
        activityRepository.findByIdAndItineraryDayId(
            activityId,
            itineraryDayId
        )
    } returns activity()

    val result = service.getActivity(
        userId,
        tripId,
        itineraryDayId,
        activityId
    )

    assertEquals(activityId, result.id)
    assertEquals("Burj Khalifa", result.title)
}

@Test
fun `get activity rejects missing activity`() {

    every {
        tripRepository.findById(
            tripId
        )
    } returns Optional.of(trip())

    every {
        itineraryDayRepository.findById(itineraryDayId)
    } returns Optional.of(itineraryDay())

    every {
        activityRepository.findByIdAndItineraryDayId(
            activityId,
            itineraryDayId
        )
    } returns null

    assertThrows<IllegalArgumentException> {
        service.getActivity(
            userId,
            tripId,
            itineraryDayId,
            activityId
        )
    }
}

// ---------------------------------------------------------
// UPDATE
// ---------------------------------------------------------

@Test
fun `update activity successfully`() {

    val existingActivity = activity()

    every {
        tripRepository.findById(
            tripId
        )
    } returns Optional.of(trip())

    every {
        itineraryDayRepository.findById(itineraryDayId)
    } returns Optional.of(itineraryDay())

    every {
        activityRepository.findByIdAndItineraryDayId(
            activityId,
            itineraryDayId
        )
    } returns existingActivity

    every {
        activityRepository.save(any())
    } answers {
        firstArg()
    }

    val input = UpdateItineraryActivityInput(
        title = "Dubai Mall",
        description = null,
        type = ActivityType.SIGHTSEEING,
        startTime = "15:00",
        endTime = "18:00",
        location = "Dubai Mall",
        latitude = 25.1985,
        longitude = 55.2796,
        sortOrder = 2,
        completed = true
    )

    val result = service.updateActivity(
        userId,
        tripId,
        itineraryDayId,
        activityId,
        input
    )

    assertEquals("Dubai Mall", result.title)
    assertEquals(ActivityType.SIGHTSEEING, result.type)
    assertEquals(LocalTime.of(15, 0), result.startTime)
    assertEquals(LocalTime.of(18, 0), result.endTime)
    assertEquals(2, result.sortOrder)
    assertTrue(result.completed)

    verify(exactly = 1) {
        activityRepository.save(any())
    }
}

@Test
fun `update activity rejects missing activity`() {

    every {
        tripRepository.findById(
            tripId
        )
    } returns Optional.of(trip())

    every {
        itineraryDayRepository.findById(itineraryDayId)
    } returns Optional.of(itineraryDay())

    every {
        activityRepository.findByIdAndItineraryDayId(
            activityId,
            itineraryDayId
        )
    } returns null

    val input = UpdateItineraryActivityInput(
        title = "Updated",
        description = null,
        type = null,
        startTime = null,
        endTime = null,
        location = null,
        latitude = null,
        longitude = null,
        sortOrder = null,
        completed = null
    )

    assertThrows<IllegalArgumentException> {
        service.updateActivity(
            userId,
            tripId,
            itineraryDayId,
            activityId,
            input
        )
    }

    verify(exactly = 0) {
        activityRepository.save(any())
    }
}

// ---------------------------------------------------------
// DELETE
// ---------------------------------------------------------

@Test
fun `delete activity successfully`() {

    val existingActivity = activity()

    every {
        tripRepository.findById(
            tripId
        )
    } returns Optional.of(trip())

    every {
        itineraryDayRepository.findById(itineraryDayId)
    } returns Optional.of(itineraryDay())

    every {
        activityRepository.findByIdAndItineraryDayId(
            activityId,
            itineraryDayId
        )
    } returns existingActivity

    every {
        activityRepository.delete(existingActivity)
    } just runs

    val result = service.deleteActivity(
        userId,
        tripId,
        itineraryDayId,
        activityId
    )

    assertTrue(result)

    verify(exactly = 1) {
        activityRepository.delete(existingActivity)
    }
}

@Test
fun `delete activity rejects missing activity`() {

    every {
        tripRepository.findById(
            tripId
        )
    } returns Optional.of(trip())

    every {
        itineraryDayRepository.findById(itineraryDayId)
    } returns Optional.of(itineraryDay())

    every {
        activityRepository.findByIdAndItineraryDayId(
            activityId,
            itineraryDayId
        )
    } returns null

    assertThrows<IllegalArgumentException> {
        service.deleteActivity(
            userId,
            tripId,
            itineraryDayId,
            activityId
        )
    }

    verify(exactly = 0) {
        activityRepository.delete(any())
    }
}

// ---------------------------------------------------------
// COMPLETE / UNCOMPLETE
// ---------------------------------------------------------

@Test
fun `mark activity completed successfully`() {

    val existingActivity = activity()

    every {
        tripRepository.findById(
            tripId
        )
    } returns Optional.of(trip())

    every {
        itineraryDayRepository.findById(itineraryDayId)
    } returns Optional.of(itineraryDay())

    every {
        activityRepository.findByIdAndItineraryDayId(
            activityId,
            itineraryDayId
        )
    } returns existingActivity

    every {
        activityRepository.save(any())
    } answers {
        firstArg()
    }

    val result = service.markActivityCompleted(
        userId,
        tripId,
        itineraryDayId,
        activityId,
        true
    )

    assertTrue(result.completed)

    verify(exactly = 1) {
        activityRepository.save(existingActivity)
    }
}

@Test
fun `mark activity incomplete successfully`() {

    val existingActivity = activity().apply {
        completed = true
    }

    every {
        tripRepository.findById(
            tripId
        )
    } returns Optional.of(trip())

    every {
        itineraryDayRepository.findById(itineraryDayId)
    } returns Optional.of(itineraryDay())

    every {
        activityRepository.findByIdAndItineraryDayId(
            activityId,
            itineraryDayId
        )
    } returns existingActivity

    every {
        activityRepository.save(any())
    } answers {
        firstArg()
    }

    val result = service.markActivityCompleted(
        userId,
        tripId,
        itineraryDayId,
        activityId,
        false
    )

    assertFalse(result.completed)

    verify(exactly = 1) {
        activityRepository.save(existingActivity)
    }
}

    @Test
    fun `create activity rejects missing itinerary day`() {

        every {
            tripRepository.findById(
                tripId
            )
        } returns Optional.of(trip())

        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.empty()

        val input = CreateItineraryActivityInput(
            title = "Burj Khalifa",
            description = null,
            type = ActivityType.SIGHTSEEING,
            startTime = null,
            endTime = null,
            location = null,
            latitude = null,
            longitude = null,
            sortOrder = null
        )

        assertThrows<IllegalArgumentException> {
            service.createActivity(
                userId,
                tripId,
                itineraryDayId,
                input
            )
        }

        verify(exactly = 0) {
            activityRepository.save(any())
        }
    }

    @Test
    fun `update activity rejects end time before start time`() {

        val existingActivity = activity()

        every {
            tripRepository.findById(
                tripId
            )
        } returns Optional.of(trip())

        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.of(itineraryDay())

        every {
            activityRepository.findByIdAndItineraryDayId(
                activityId,
                itineraryDayId
            )
        } returns existingActivity

        val input = UpdateItineraryActivityInput(
            title = null,
            description = null,
            type = null,
            startTime = "20:00",
            endTime = "18:00",
            location = null,
            latitude = null,
            longitude = null,
            sortOrder = null,
            completed = null
        )

        assertThrows<IllegalArgumentException> {
            service.updateActivity(
                userId,
                tripId,
                itineraryDayId,
                activityId,
                input
            )
        }

        verify(exactly = 0) {
            activityRepository.save(any())
        }
    }

    @Test
    fun `update activity rejects invalid latitude`() {

        val existingActivity = activity()

        every {
            tripRepository.findById(
                tripId
            )
        } returns Optional.of(trip())

        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.of(itineraryDay())

        every {
            activityRepository.findByIdAndItineraryDayId(
                activityId,
                itineraryDayId
            )
        } returns existingActivity

        val input = UpdateItineraryActivityInput(
            title = null,
            description = null,
            type = null,
            startTime = null,
            endTime = null,
            location = null,
            latitude = 100.0,
            longitude = null,
            sortOrder = null,
            completed = null
        )

        assertThrows<IllegalArgumentException> {
            service.updateActivity(
                userId,
                tripId,
                itineraryDayId,
                activityId,
                input
            )
        }

        verify(exactly = 0) {
            activityRepository.save(any())
        }
    }

    @Test
    fun `update activity rejects negative sort order`() {

        val existingActivity = activity()

        every {
            tripRepository.findById(
                tripId
            )
        } returns Optional.of(trip())

        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.of(itineraryDay())

        every {
            activityRepository.findByIdAndItineraryDayId(
                activityId,
                itineraryDayId
            )
        } returns existingActivity

        val input = UpdateItineraryActivityInput(
            title = null,
            description = null,
            type = null,
            startTime = null,
            endTime = null,
            location = null,
            latitude = null,
            longitude = null,
            sortOrder = -1,
            completed = null
        )

        assertThrows<IllegalArgumentException> {
            service.updateActivity(
                userId,
                tripId,
                itineraryDayId,
                activityId,
                input
            )
        }

        verify(exactly = 0) {
            activityRepository.save(any())
        }
    }

    @Test
    fun `accepted member can get activities`() {

        val memberUserId = UUID.randomUUID()

        val sharedTrip = trip().apply {
            ownerId = UUID.randomUUID()
        }

        every {
            tripRepository.findById(tripId)
        } returns Optional.of(sharedTrip)

        every {
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                memberUserId
            )
        } returns TripMember(
            tripId = tripId,
            userId = memberUserId,
            role = TripMemberRole.MEMBER,
            status = TripMemberStatus.ACCEPTED
        )

        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.of(itineraryDay())

        every {
            activityRepository.findAllByItineraryDayIdOrderBySortOrderAsc(
                itineraryDayId
            )
        } returns listOf(activity())

        val result = service.getActivities(
            memberUserId,
            tripId,
            itineraryDayId
        )

        assertEquals(1, result.size)
        assertEquals(activityId, result[0].id)
        assertEquals("Burj Khalifa", result[0].title)
    }

    @Test
    fun `get activity rejects activity belonging to another day`() {

        val anotherDayId = UUID.randomUUID()

        val existingActivity = activity().apply {
            itineraryDayId = anotherDayId
        }

        every {
            tripRepository.findById(tripId)
        } returns Optional.of(trip())

        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.of(itineraryDay())

        every {
            activityRepository.findByIdAndItineraryDayId(
                activityId,
                itineraryDayId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            service.getActivity(
                userId,
                tripId,
                itineraryDayId,
                activityId
            )
        }

        verify(exactly = 0) {
            activityRepository.save(any())
        }
    }

    @Test
    fun `accepted member cannot access activity belonging to another day`() {

        val memberUserId = UUID.randomUUID()
        val anotherDayId = UUID.randomUUID()

        val sharedTrip = trip().apply {
            ownerId = UUID.randomUUID()
        }

        every {
            tripRepository.findById(tripId)
        } returns Optional.of(sharedTrip)

        every {
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                memberUserId
            )
        } returns TripMember(
            tripId = tripId,
            userId = memberUserId,
            role = TripMemberRole.MEMBER,
            status = TripMemberStatus.ACCEPTED
        )

        // The requested day belongs to this trip.
        every {
            itineraryDayRepository.findById(itineraryDayId)
        } returns Optional.of(itineraryDay())

        // Activity does NOT belong to the requested day.
        every {
            activityRepository.findByIdAndItineraryDayId(
                activityId,
                itineraryDayId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            service.getActivity(
                memberUserId,
                tripId,
                itineraryDayId,
                activityId
            )
        }

        verify(exactly = 0) {
            activityRepository.save(any())
        }

        verify(exactly = 0) {
            activityRepository.delete(any())
        }
    }
}