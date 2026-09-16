package com.trippoint.backend.conversation.service

import com.trippoint.backend.conversation.entity.ChatMessage
import com.trippoint.backend.conversation.event.ChatMessageCreatedEvent
import com.trippoint.backend.conversation.model.MessageType
import com.trippoint.backend.trip.service.TripAccessService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import java.util.UUID

class ChatSubscriptionServiceTest {

    private val tripAccessService = mockk<TripAccessService>()

    private val service = ChatSubscriptionService(
        tripAccessService = tripAccessService
    )

    private val tripId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @AfterEach
    fun cleanup() {
        // Nothing required for now.
    }

    @Test
    fun `accepted member can subscribe and receives new message`() {
        every {
            tripAccessService.requireMemberAccess(
                tripId = tripId,
                userId = userId
            )
        } just Runs

        val flux = service.subscribe(
            userId = userId,
            tripId = tripId
        )

        val message = ChatMessage(
            id = UUID.randomUUID(),
            tripId = tripId,
            senderId = userId,
            content = "Hello TripPoint",
            messageType = MessageType.TEXT
        )

        StepVerifier.create(flux)
            .then {
                service.onChatMessageCreated(
                    ChatMessageCreatedEvent(message)
                )
            }
            .assertNext { received ->
                assertEquals(message.id, received.id)
                assertEquals(tripId, received.tripId)
                assertEquals(userId, received.senderId)
                assertEquals("Hello TripPoint", received.content)
                assertEquals(MessageType.TEXT, received.messageType)
            }
            .thenCancel()
            .verify()
    }

    @Test
    fun `subscription requires trip member access`() {
        every {
            tripAccessService.requireMemberAccess(
                tripId = tripId,
                userId = userId
            )
        } throws IllegalAccessException(
            "You do not have access to this trip"
        )

        val exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalAccessException::class.java
        ) {
            service.subscribe(
                userId = userId,
                tripId = tripId
            )
        }

        assertEquals(
            "You do not have access to this trip",
            exception.message
        )
    }

    @Test
    fun `message for another trip is not received`() {
        every {
            tripAccessService.requireMemberAccess(
                tripId = tripId,
                userId = userId
            )
        } just Runs

        val flux = service.subscribe(
            userId = userId,
            tripId = tripId
        )

        val otherTripMessage = ChatMessage(
            id = UUID.randomUUID(),
            tripId = UUID.randomUUID(),
            senderId = UUID.randomUUID(),
            content = "Other trip message",
            messageType = MessageType.TEXT
        )

        StepVerifier.create(flux)
            .then {
                service.onChatMessageCreated(
                    ChatMessageCreatedEvent(otherTripMessage)
                )
            }
            .thenCancel()
            .verify()
    }

    @Test
    fun `multiple subscribers to same trip receive the message`() {
        every {
            tripAccessService.requireMemberAccess(
                tripId = tripId,
                userId = any()
            )
        } just Runs

        val user1 = UUID.randomUUID()
        val user2 = UUID.randomUUID()

        val flux1 = service.subscribe(
            userId = user1,
            tripId = tripId
        )

        val flux2 = service.subscribe(
            userId = user2,
            tripId = tripId
        )

        val message = ChatMessage(
            id = UUID.randomUUID(),
            tripId = tripId,
            senderId = user1,
            content = "Shared message",
            messageType = MessageType.TEXT
        )

        val verifier1 = StepVerifier.create(flux1)
            .then {
                service.onChatMessageCreated(
                    ChatMessageCreatedEvent(message)
                )
            }
            .expectNextMatches {
                it.id == message.id
            }
            .thenCancel()

        val verifier2 = StepVerifier.create(flux2)
            .thenCancel()

        verifier1.verify()
        verifier2.verify()
    }
}