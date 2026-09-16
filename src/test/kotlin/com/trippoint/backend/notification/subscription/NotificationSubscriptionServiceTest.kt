package com.trippoint.backend.notification.subscription

import com.trippoint.backend.notification.entity.Notification
import com.trippoint.backend.notification.model.NotificationCategory
import com.trippoint.backend.notification.model.NotificationType
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import java.util.UUID
import kotlin.test.assertEquals

class NotificationSubscriptionServiceTest {

    private val service = NotificationSubscriptionService()

    private val userId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()

    @Test
    fun `subscriber should receive notification for same user`() {
        val notification = notification(userId)

        val flux = service.subscribe(userId)

        StepVerifier.create(flux.take(1))
            .then {
                service.publish(notification)
            }
            .expectNext(notification)
            .verifyComplete()
    }

    @Test
    fun `subscriber should not receive notification for another user`() {
        val notification = notification(otherUserId)

        val flux = service.subscribe(userId)

        StepVerifier.create(flux)
            .then {
                service.publish(notification)
            }
            .thenCancel()
            .verify()
    }

    @Test
    fun `multiple subscribers for same user should receive notification`() {
        val notification = notification(userId)

        val first = service.subscribe(userId)
        val second = service.subscribe(userId)

        StepVerifier.create(first.take(1))
            .then {
                StepVerifier.create(second.take(1))
                    .then {
                        service.publish(notification)
                    }
                    .expectNext(notification)
                    .verifyComplete()
            }
            .expectNext(notification)
            .verifyComplete()
    }

    @Test
    fun `publishing without subscriber should not fail`() {
        val notification = notification(userId)

        service.publish(notification)

        assertEquals(0, service.currentSubscriberCount(userId))
    }

    @Test
    fun `subscriber count should increase when subscribed`() {
        val flux = service.subscribe(userId)

        StepVerifier.create(flux)
            .then {
                assertEquals(
                    1,
                    service.currentSubscriberCount(userId)
                )
            }
            .thenCancel()
            .verify()
    }

    @Test
    fun `subscriber should be removed after cancellation`() {
        val flux = service.subscribe(userId)

        StepVerifier.create(flux)
            .thenCancel()
            .verify()

        assertEquals(
            0,
            service.currentSubscriberCount(userId)
        )
    }

    private fun notification(
        recipientUserId: UUID
    ): Notification {
        return Notification(
            recipientUserId = recipientUserId,
            category = NotificationCategory.TRIP,
            type = NotificationType.TRIP_UPDATED,
            title = "Trip updated",
            message = "Trip was updated"
        )
    }
}