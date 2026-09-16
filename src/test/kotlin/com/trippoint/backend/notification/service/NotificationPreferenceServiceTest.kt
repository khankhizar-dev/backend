package com.trippoint.backend.notification.service

import com.trippoint.backend.notification.entity.NotificationPreference
import com.trippoint.backend.notification.repository.NotificationPreferenceRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NotificationPreferenceServiceTest {

    private lateinit var repository: NotificationPreferenceRepository
    private lateinit var service: NotificationPreferenceService

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        repository = mockk()
        service = NotificationPreferenceService(repository)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getOrCreate should return existing preferences`() {
        val preference = preference()

        every {
            repository.findByUserId(userId)
        } returns preference

        val result = service.getOrCreate(userId)

        assertEquals(preference, result)

        verify(exactly = 1) {
            repository.findByUserId(userId)
        }

        verify(exactly = 0) {
            repository.save(any())
        }
    }

    @Test
    fun `getOrCreate should create default preferences when none exist`() {
        every {
            repository.findByUserId(userId)
        } returns null

        every {
            repository.save(any())
        } answers {
            firstArg()
        }

        val result = service.getOrCreate(userId)

        assertNotNull(result)
        assertEquals(userId, result.userId)

        assertTrue(result.pushEnabled)
        assertTrue(result.emailEnabled)
        assertTrue(result.inAppEnabled)
        assertFalse(result.smsEnabled)
        assertFalse(result.digestEnabled)
        assertFalse(result.quietHoursEnabled)

        verify(exactly = 1) {
            repository.findByUserId(userId)
        }

        verify(exactly = 1) {
            repository.save(any())
        }
    }

    @Test
    fun `update should update all notification channels`() {
        val preference = preference()

        every {
            repository.findByUserId(userId)
        } returns preference

        every {
            repository.save(preference)
        } returns preference

        val result = service.update(
            userId = userId,
            pushEnabled = false,
            emailEnabled = false,
            inAppEnabled = false,
            smsEnabled = true,
            digestEnabled = true
        )

        assertFalse(result.pushEnabled)
        assertFalse(result.emailEnabled)
        assertFalse(result.inAppEnabled)
        assertTrue(result.smsEnabled)
        assertTrue(result.digestEnabled)

        verify(exactly = 1) {
            repository.save(preference)
        }
    }

    @Test
    fun `update should enable quiet hours`() {
        val preference = preference()

        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(7, 0)

        every {
            repository.findByUserId(userId)
        } returns preference

        every {
            repository.save(preference)
        } returns preference

        val result = service.update(
            userId = userId,
            quietHoursEnabled = true,
            quietHoursStart = start,
            quietHoursEnd = end
        )

        assertTrue(result.quietHoursEnabled)
        assertEquals(start, result.quietHoursStart)
        assertEquals(end, result.quietHoursEnd)

        verify(exactly = 1) {
            repository.save(preference)
        }
    }

    @Test
    fun `update should disable quiet hours`() {
        val preference = preference().apply {
            quietHoursEnabled = true
            quietHoursStart = LocalTime.of(22, 0)
            quietHoursEnd = LocalTime.of(7, 0)
        }

        every {
            repository.findByUserId(userId)
        } returns preference

        every {
            repository.save(preference)
        } returns preference

        val result = service.update(
            userId = userId,
            quietHoursEnabled = false
        )

        assertFalse(result.quietHoursEnabled)

        // Existing times remain because the current service
        // only changes fields explicitly supplied.
        assertEquals(
            LocalTime.of(22, 0),
            result.quietHoursStart
        )

        assertEquals(
            LocalTime.of(7, 0),
            result.quietHoursEnd
        )

        verify(exactly = 1) {
            repository.save(preference)
        }
    }

    @Test
    fun `update should support partial update`() {
        val preference = preference().apply {
            pushEnabled = true
            emailEnabled = true
            inAppEnabled = true
            smsEnabled = false
            digestEnabled = false
        }

        every {
            repository.findByUserId(userId)
        } returns preference

        every {
            repository.save(preference)
        } returns preference

        val result = service.update(
            userId = userId,
            pushEnabled = false
        )

        assertFalse(result.pushEnabled)

        // Other settings should remain unchanged.
        assertTrue(result.emailEnabled)
        assertTrue(result.inAppEnabled)
        assertFalse(result.smsEnabled)
        assertFalse(result.digestEnabled)

        verify(exactly = 1) {
            repository.save(preference)
        }
    }

    @Test
    fun `update should enable digest`() {
        val preference = preference()

        every {
            repository.findByUserId(userId)
        } returns preference

        every {
            repository.save(preference)
        } returns preference

        val result = service.update(
            userId = userId,
            digestEnabled = true
        )

        assertTrue(result.digestEnabled)

        verify(exactly = 1) {
            repository.save(preference)
        }
    }

    @Test
    fun `update should enable sms`() {
        val preference = preference()

        every {
            repository.findByUserId(userId)
        } returns preference

        every {
            repository.save(preference)
        } returns preference

        val result = service.update(
            userId = userId,
            smsEnabled = true
        )

        assertTrue(result.smsEnabled)

        verify(exactly = 1) {
            repository.save(preference)
        }
    }

    @Test
    fun `update should create preferences when user has none`() {
        every {
            repository.findByUserId(userId)
        } returns null

        every {
            repository.save(any())
        } answers {
            firstArg<NotificationPreference>()
        }

        val result = service.update(
            userId = userId,
            pushEnabled = false,
            emailEnabled = false
        )

        assertEquals(userId, result.userId)

        assertFalse(result.pushEnabled)
        assertFalse(result.emailEnabled)
        assertTrue(result.inAppEnabled)
        assertFalse(result.smsEnabled)
        assertFalse(result.digestEnabled)
        assertFalse(result.quietHoursEnabled)

        verify(exactly = 1) {
            repository.findByUserId(userId)
        }

        verify(exactly = 2) {
            repository.save(any())
        }
    }

    private fun preference(
        userId: UUID = this.userId
    ): NotificationPreference {
        return NotificationPreference(
            userId = userId
        )
    }
}