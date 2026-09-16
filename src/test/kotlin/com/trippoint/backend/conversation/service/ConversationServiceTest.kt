package com.trippoint.backend.conversation.service

import com.trippoint.backend.activity.event.ActivityLogEventPublisher
import com.trippoint.backend.activity.repository.ActivityLogRepository
import com.trippoint.backend.activity.service.ActivityLogService
import com.trippoint.backend.conversation.entity.ChatMessage
import com.trippoint.backend.conversation.event.ChatMessageEventPublisher
import com.trippoint.backend.conversation.model.MessageType
import com.trippoint.backend.conversation.repository.ChatAttachmentRepository
import com.trippoint.backend.conversation.repository.ChatMessageRepository
import com.trippoint.backend.trip.entity.Trip
import com.trippoint.backend.trip.entity.TripMember
import com.trippoint.backend.trip.model.TripMemberRole
import com.trippoint.backend.trip.model.TripMemberStatus
import com.trippoint.backend.trip.repository.TripMemberRepository
import com.trippoint.backend.trip.repository.TripRepository
import com.trippoint.backend.trip.service.TripAccessService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationServiceTest {

    private lateinit var chatMessageRepository: ChatMessageRepository
    private lateinit var chatAttachmentRepository: ChatAttachmentRepository
    private lateinit var activityLogRepository: ActivityLogRepository

    private lateinit var tripRepository: TripRepository
    private lateinit var tripMemberRepository: TripMemberRepository

    private lateinit var tripAccessService: TripAccessService
    private lateinit var activityLogService: ActivityLogService
    private lateinit var conversationService: ConversationService
    private lateinit var chatMessageEventPublisher: ChatMessageEventPublisher
    private lateinit var activityLogEventPublisher: ActivityLogEventPublisher

    private val tripId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()
    private val messageId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        chatMessageRepository = mockk()
        chatAttachmentRepository = mockk()
        activityLogRepository = mockk()

        tripRepository = mockk()
        tripMemberRepository = mockk()
        chatMessageEventPublisher = mockk()
        activityLogEventPublisher = mockk()

        tripAccessService = TripAccessService(
            tripRepository = tripRepository,
            tripMemberRepository = tripMemberRepository
        )

        activityLogService = ActivityLogService(
            activityLogRepository = activityLogRepository,
            tripAccessService = tripAccessService,
            activityLogEventPublisher = activityLogEventPublisher
        )

        conversationService = ConversationService(
            chatMessageRepository = chatMessageRepository,
            chatAttachmentRepository = chatAttachmentRepository,
            tripAccessService = tripAccessService,
            activityLogService = activityLogService,
            chatMessageEventPublisher = chatMessageEventPublisher
        )

        every {
            chatMessageEventPublisher.publish(any())
        } just Runs

        every {
            activityLogEventPublisher.publish(any())
        } just Runs
    }

    private fun trip(
        ownerId: UUID = userId
    ): Trip {
        return Trip(
            id = tripId,
            ownerId = ownerId,
            name = "Dubai Trip",
            destination = "Dubai",
            startDate = LocalDate.of(2026, 10, 15),
            endDate = LocalDate.of(2026, 10, 20)
        )
    }

    private fun mockOwnerAccess(
        ownerId: UUID = userId
    ) {
        every {
            tripRepository.findById(tripId)
        } returns Optional.of(
            trip(ownerId)
        )
    }

    private fun mockAcceptedMemberAccess(
        memberId: UUID = userId
    ) {
        every {
            tripRepository.findById(tripId)
        } returns Optional.of(
            trip(ownerId = otherUserId)
        )

        every {
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                memberId
            )
        } returns TripMember(
            id = UUID.randomUUID(),
            tripId = tripId,
            userId = memberId,
            role = TripMemberRole.MEMBER,
            status = TripMemberStatus.ACCEPTED
        )
    }

    @Test
    fun `owner can send text message`() {
        mockOwnerAccess()
        mockActivitySave()

        val message = ChatMessage(
            id = messageId,
            tripId = tripId,
            senderId = userId,
            content = "Our hotel is confirmed",
            messageType = MessageType.TEXT
        )

        every {
            chatMessageRepository.save(any())
        } returns message

        val result = conversationService.sendMessage(
            userId = userId,
            tripId = tripId,
            content = "Our hotel is confirmed",
            type = MessageType.TEXT
        )

        assertEquals(messageId, result.id)
        assertEquals("Our hotel is confirmed", result.content)
        assertEquals(MessageType.TEXT, result.messageType)

        verify(exactly = 1) {
            chatMessageRepository.save(any())
        }

        verify(exactly = 1) {
            activityLogRepository.save(any())
        }
    }

    @Test
    fun `accepted member can send text message`() {
        mockAcceptedMemberAccess()
        mockActivitySave()

        val message = ChatMessage(
            id = messageId,
            tripId = tripId,
            senderId = userId,
            content = "I will book the airport transfer",
            messageType = MessageType.TEXT
        )

        every {
            chatMessageRepository.save(any())
        } returns message

        val result = conversationService.sendMessage(
            userId = userId,
            tripId = tripId,
            content = "I will book the airport transfer",
            type = MessageType.TEXT
        )

        assertEquals(userId, result.senderId)
        assertEquals(
            "I will book the airport transfer",
            result.content
        )

        verify(exactly = 1) {
            chatMessageRepository.save(any())
        }
    }

    @Test
    fun `non member cannot send message`() {
        every {
            tripRepository.findById(tripId)
        } returns Optional.of(
            trip(ownerId = otherUserId)
        )

        every {
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                userId
            )
        } returns null

        assertThrows<IllegalAccessException> {
            conversationService.sendMessage(
                userId = userId,
                tripId = tripId,
                content = "Hello",
                type = MessageType.TEXT
            )
        }

        verify(exactly = 0) {
            chatMessageRepository.save(any())
        }
    }

    @Test
    fun `blank text message is rejected`() {
        mockOwnerAccess()

        assertThrows<IllegalArgumentException> {
            conversationService.sendMessage(
                userId = userId,
                tripId = tripId,
                content = "   ",
                type = MessageType.TEXT
            )
        }

        verify(exactly = 0) {
            chatMessageRepository.save(any())
        }
    }

    @Test
    fun `user can reply to message in same trip`() {
        mockOwnerAccess()
        mockActivitySave()

        val parentMessage = ChatMessage(
            id = messageId,
            tripId = tripId,
            senderId = otherUserId,
            content = "Let's leave at 8 AM",
            messageType = MessageType.TEXT
        )

        every {
            chatMessageRepository.findByIdAndTripId(
                messageId,
                tripId
            )
        } returns parentMessage

        val reply = ChatMessage(
            id = UUID.randomUUID(),
            tripId = tripId,
            senderId = userId,
            content = "Sounds good",
            messageType = MessageType.TEXT,
            replyToId = messageId
        )

        every {
            chatMessageRepository.save(any())
        } returns reply

        val result = conversationService.sendMessage(
            userId = userId,
            tripId = tripId,
            content = "Sounds good",
            type = MessageType.TEXT,
            replyToId = messageId
        )

        assertEquals(messageId, result.replyToId)

        verify(exactly = 1) {
            chatMessageRepository.findByIdAndTripId(
                messageId,
                tripId
            )
        }
    }

    @Test
    fun `reply to message from another trip is rejected`() {
        mockOwnerAccess()

        every {
            chatMessageRepository.findByIdAndTripId(
                messageId,
                tripId
            )
        } returns null

        assertThrows<IllegalArgumentException> {
            conversationService.sendMessage(
                userId = userId,
                tripId = tripId,
                content = "Reply",
                type = MessageType.TEXT,
                replyToId = messageId
            )
        }

        verify(exactly = 0) {
            chatMessageRepository.save(any())
        }
    }

    @Test
    fun `cannot reply to deleted message`() {
        mockOwnerAccess()

        val deletedMessage = ChatMessage(
            id = messageId,
            tripId = tripId,
            senderId = otherUserId,
            content = "Old message",
            messageType = MessageType.TEXT,
            deleted = true
        )

        every {
            chatMessageRepository.findByIdAndTripId(
                messageId,
                tripId
            )
        } returns deletedMessage

        assertThrows<IllegalArgumentException> {
            conversationService.sendMessage(
                userId = userId,
                tripId = tripId,
                content = "Reply",
                type = MessageType.TEXT,
                replyToId = messageId
            )
        }

        verify(exactly = 0) {
            chatMessageRepository.save(any())
        }
    }

    @Test
    fun `member can get messages`() {
        mockAcceptedMemberAccess()

        val older = ChatMessage(
            id = UUID.randomUUID(),
            tripId = tripId,
            senderId = otherUserId,
            content = "Older",
            messageType = MessageType.TEXT,
            createdAt = LocalDateTime.of(2026, 9, 13, 10, 0)
        )

        val newer = ChatMessage(
            id = UUID.randomUUID(),
            tripId = tripId,
            senderId = userId,
            content = "Newer",
            messageType = MessageType.TEXT,
            createdAt = LocalDateTime.of(2026, 9, 13, 10, 1)
        )

        every {
            chatMessageRepository
                .findAllByTripIdAndDeletedFalseOrderByCreatedAtDescIdDesc(
                    tripId,
                    any<Pageable>()
                )
        } returns listOf(newer, older)

        val result = conversationService.getMessages(
            userId = userId,
            tripId = tripId,
            limit = 50
        )

        assertEquals(2, result.size)
        assertEquals("Older", result[0].content)
        assertEquals("Newer", result[1].content)
    }

    @Test
    fun `non member cannot get messages`() {
        every {
            tripRepository.findById(tripId)
        } returns Optional.of(
            trip(ownerId = otherUserId)
        )

        every {
            tripMemberRepository.findByTripIdAndUserId(
                tripId,
                userId
            )
        } returns null

        assertThrows<IllegalAccessException> {
            conversationService.getMessages(
                userId = userId,
                tripId = tripId
            )
        }

        verify(exactly = 0) {
            chatMessageRepository
                .findAllByTripIdAndDeletedFalseOrderByCreatedAtDescIdDesc(
                    any(),
                    any()
                )
        }
    }

    @Test
    fun `sender can delete own message`() {
        mockOwnerAccess()
        mockActivitySave()

        val message = ChatMessage(
            id = messageId,
            tripId = tripId,
            senderId = userId,
            content = "Delete me",
            messageType = MessageType.TEXT
        )

        every {
            chatMessageRepository.findByIdAndTripId(
                messageId,
                tripId
            )
        } returns message

        every {
            chatMessageRepository.save(any())
        } returns message

        val result = conversationService.deleteMessage(
            userId = userId,
            tripId = tripId,
            messageId = messageId
        )

        assertTrue(result)
        assertTrue(message.deleted)

        verify(exactly = 1) {
            chatMessageRepository.save(message)
        }
    }

    @Test
    fun `owner can delete member message`() {
        mockOwnerAccess()
        mockActivitySave()

        val message = ChatMessage(
            id = messageId,
            tripId = tripId,
            senderId = otherUserId,
            content = "Member message",
            messageType = MessageType.TEXT
        )

        every {
            chatMessageRepository.findByIdAndTripId(
                messageId,
                tripId
            )
        } returns message

        every {
            chatMessageRepository.save(any())
        } returns message

        val result = conversationService.deleteMessage(
            userId = userId,
            tripId = tripId,
            messageId = messageId
        )

        assertTrue(result)
        assertTrue(message.deleted)
    }

    @Test
    fun `member cannot delete another member message`() {
        mockAcceptedMemberAccess()

        val message = ChatMessage(
            id = messageId,
            tripId = tripId,
            senderId = otherUserId,
            content = "Another member message",
            messageType = MessageType.TEXT
        )

        every {
            chatMessageRepository.findByIdAndTripId(
                messageId,
                tripId
            )
        } returns message

        assertThrows<IllegalArgumentException> {
            conversationService.deleteMessage(
                userId = userId,
                tripId = tripId,
                messageId = messageId
            )
        }

        verify(exactly = 0) {
            chatMessageRepository.save(any())
        }
    }

    private fun mockActivitySave() {
        every {
            activityLogRepository.save(any())
        } answers {
            firstArg()
        }
    }
}