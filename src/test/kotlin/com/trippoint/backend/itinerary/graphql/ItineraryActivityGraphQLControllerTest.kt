package com.trippoint.backend.itinerary.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.itinerary.graphql.input.CreateItineraryActivityInput
import com.trippoint.backend.itinerary.graphql.input.UpdateItineraryActivityInput
import com.trippoint.backend.itinerary.model.ActivityType
import com.trippoint.backend.itinerary.service.ItineraryActivityService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import java.time.LocalTime
import java.util.UUID

class ItineraryActivityGraphQLControllerTest {

    private lateinit var service: ItineraryActivityService
    private lateinit var controller: ItineraryActivityGraphQLController

    private val userId = UUID.randomUUID()
    private val tripId = UUID.randomUUID()
    private val itineraryDayId = UUID.randomUUID()
    private val activityId = UUID.randomUUID()

    private lateinit var authentication: UsernamePasswordAuthenticationToken

    @BeforeEach
    fun setup() {

        service = mockk()

        controller = ItineraryActivityGraphQLController(
            service
        )

        val principal = UserPrincipal(
            userId = userId,
            email = "test@example.com",
            isActive = true
        )

        authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.authorities
        )
    }

    @Test
    fun `create itinerary activity passes authenticated user to service`() {

        val input = CreateItineraryActivityInput(
            title = "Burj Khalifa",
            description = "Observation deck",
            type = ActivityType.SIGHTSEEING,
            startTime = "18:00",
            endTime = "20:00",
            location = "Dubai",
            latitude = 25.1972,
            longitude = 55.2744,
            sortOrder = 1
        )

        val expected = ItineraryActivityResponse(
            id = activityId,
            itineraryDayId = itineraryDayId,
            title = "Burj Khalifa",
            description = "Observation deck",
            type = ActivityType.SIGHTSEEING,
            startTime = LocalTime.of(18, 0),
            endTime = LocalTime.of(20, 0),
            location = "Dubai",
            latitude = 25.1972,
            longitude = 55.2744,
            sortOrder = 1,
            completed = false,
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now()
        )

        every {
            service.createActivity(
                userId,
                tripId,
                itineraryDayId,
                input
            )
        } returns expected

        val result = controller.createItineraryActivity(
            tripId = tripId,
            itineraryDayId = itineraryDayId,
            input = input,
            authentication = authentication
        )

        assertEquals(activityId, result.id)
        assertEquals("Burj Khalifa", result.title)

        verify(exactly = 1) {
            service.createActivity(
                userId,
                tripId,
                itineraryDayId,
                input
            )
        }
    }

    @Test
    fun `get itinerary activities passes authenticated user to service`() {

        val expected = listOf(
            mockk<ItineraryActivityResponse>()
        )

        every {
            service.getActivities(
                userId,
                tripId,
                itineraryDayId
            )
        } returns expected

        val result = controller.itineraryActivities(
            tripId = tripId,
            itineraryDayId = itineraryDayId,
            authentication = authentication
        )

        assertEquals(expected, result)

        verify(exactly = 1) {
            service.getActivities(
                userId,
                tripId,
                itineraryDayId
            )
        }
    }

    @Test
    fun `get itinerary activity passes arguments correctly`() {

        val expected = mockk<ItineraryActivityResponse>()

        every {
            service.getActivity(
                userId,
                tripId,
                itineraryDayId,
                activityId
            )
        } returns expected

        val result = controller.itineraryActivity(
            tripId = tripId,
            itineraryDayId = itineraryDayId,
            activityId = activityId,
            authentication = authentication
        )

        assertEquals(expected, result)

        verify(exactly = 1) {
            service.getActivity(
                userId,
                tripId,
                itineraryDayId,
                activityId
            )
        }
    }

    @Test
    fun `update itinerary activity passes arguments correctly`() {

        val input = UpdateItineraryActivityInput(
            title = "Updated Activity",
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

        val expected = mockk<ItineraryActivityResponse>()

        every {
            service.updateActivity(
                userId,
                tripId,
                itineraryDayId,
                activityId,
                input
            )
        } returns expected

        val result = controller.updateItineraryActivity(
            tripId = tripId,
            itineraryDayId = itineraryDayId,
            activityId = activityId,
            input = input,
            authentication = authentication
        )

        assertEquals(expected, result)

        verify(exactly = 1) {
            service.updateActivity(
                userId,
                tripId,
                itineraryDayId,
                activityId,
                input
            )
        }
    }

    @Test
    fun `delete itinerary activity returns service result`() {

        every {
            service.deleteActivity(
                userId,
                tripId,
                itineraryDayId,
                activityId
            )
        } returns true

        val result = controller.deleteItineraryActivity(
            tripId = tripId,
            itineraryDayId = itineraryDayId,
            activityId = activityId,
            authentication = authentication
        )

        assertEquals(true, result)

        verify(exactly = 1) {
            service.deleteActivity(
                userId,
                tripId,
                itineraryDayId,
                activityId
            )
        }
    }

    @Test
    fun `mark itinerary activity completed passes completed flag`() {

        val expected = mockk<ItineraryActivityResponse>()

        every {
            service.markActivityCompleted(
                userId,
                tripId,
                itineraryDayId,
                activityId,
                true
            )
        } returns expected

        val result = controller.markItineraryActivityCompleted(
            tripId = tripId,
            itineraryDayId = itineraryDayId,
            activityId = activityId,
            completed = true,
            authentication = authentication
        )

        assertEquals(expected, result)

        verify(exactly = 1) {
            service.markActivityCompleted(
                userId,
                tripId,
                itineraryDayId,
                activityId,
                true
            )
        }
    }

    @Test
    fun `create activity rejects missing authentication`() {

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
            controller.createItineraryActivity(
                tripId = tripId,
                itineraryDayId = itineraryDayId,
                input = input,
                authentication = null
            )
        }

        verify(exactly = 0) {
            service.createActivity(
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `create activity rejects invalid authentication principal`() {

        val invalidAuthentication =
            UsernamePasswordAuthenticationToken(
                "invalid-principal",
                null
            )

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
            controller.createItineraryActivity(
                tripId = tripId,
                itineraryDayId = itineraryDayId,
                input = input,
                authentication = invalidAuthentication
            )
        }

        verify(exactly = 0) {
            service.createActivity(
                any(),
                any(),
                any(),
                any()
            )
        }
    }

}