package com.trippoint.backend.conversation.service

import com.trippoint.backend.activity.model.ActivityTarget
import com.trippoint.backend.activity.service.ActivityLogService
import com.trippoint.backend.conversation.entity.ChatAttachment
import com.trippoint.backend.conversation.entity.ChatMessage
import com.trippoint.backend.conversation.event.ChatMessageCreatedEvent
import com.trippoint.backend.conversation.event.ChatMessageEventPublisher
import com.trippoint.backend.conversation.model.MessageType
import com.trippoint.backend.conversation.repository.ChatAttachmentRepository
import com.trippoint.backend.conversation.repository.ChatMessageRepository
import com.trippoint.backend.trip.service.TripAccessService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ConversationService(
    private val chatMessageRepository: ChatMessageRepository,
    private val chatAttachmentRepository: ChatAttachmentRepository,
    private val tripAccessService: TripAccessService,
    private val activityLogService: ActivityLogService,
    private val chatMessageEventPublisher: ChatMessageEventPublisher
) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 50
        private const val MAX_PAGE_SIZE = 100
    }

    @Transactional
    fun sendMessage(
        userId: UUID,
        tripId: UUID,
        content: String,
        type: MessageType,
        replyToId: UUID? = null
    ): ChatMessage {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        val normalizedContent = content.trim()

        when (type) {
            MessageType.TEXT,
            MessageType.SYSTEM -> {
                require(normalizedContent.isNotBlank()) {
                    "Message content cannot be blank"
                }
            }

            MessageType.IMAGE,
            MessageType.FILE -> {
                require(normalizedContent.isNotBlank()) {
                    "Message content cannot be blank"
                }
            }
        }

        val replyMessage = replyToId?.let { id ->
            chatMessageRepository.findByIdAndTripId(
                id = id,
                tripId = tripId
            ) ?: throw IllegalArgumentException(
                "Reply message not found"
            )
        }

        if (replyMessage != null) {
            require(!replyMessage.deleted) {
                "Cannot reply to a deleted message"
            }
        }

        val message = chatMessageRepository.save(
            ChatMessage(
                tripId = tripId,
                senderId = userId,
                content = normalizedContent,
                messageType = type,
                replyToId = replyToId
            )
        )

        activityLogService.log(
            tripId = tripId,
            userId = userId,
            action = "sent a message",
            targetType = ActivityTarget.CHAT,
            targetName = "Chat"
        )

        chatMessageEventPublisher.publish(
            ChatMessageCreatedEvent(message)
        )

        return message
    }

    @Transactional(readOnly = true)
    fun getMessages(
        userId: UUID,
        tripId: UUID,
        limit: Int? = null,
        beforeCursor: UUID? = null
    ): List<ChatMessage> {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        val pageSize = normalizePageSize(limit)

        val messages = if (beforeCursor == null) {

            chatMessageRepository
                .findAllByTripIdAndDeletedFalseOrderByCreatedAtDescIdDesc(
                    tripId = tripId,
                    pageable = PageRequest.of(0, pageSize)
                )

        } else {

            val cursorMessage =
                chatMessageRepository.findByIdAndTripId(
                    id = beforeCursor,
                    tripId = tripId
                ) ?: throw IllegalArgumentException(
                    "Message cursor not found"
                )

            chatMessageRepository.findMessagesBeforeCursor(
                tripId = tripId,
                createdAt = cursorMessage.createdAt,
                messageId = cursorMessage.id,
                pageable = PageRequest.of(0, pageSize)
            )
        }

        /*
         * Database returns newest -> oldest.
         *
         * UI conversation should receive oldest -> newest
         * within the current page.
         */
        return messages.asReversed()
    }

    @Transactional(readOnly = true)
    fun getMessage(
        userId: UUID,
        tripId: UUID,
        messageId: UUID
    ): ChatMessage {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        return chatMessageRepository.findByIdAndTripId(
            id = messageId,
            tripId = tripId
        ) ?: throw IllegalArgumentException(
            "Message not found"
        )
    }

    @Transactional
    fun deleteMessage(
        userId: UUID,
        tripId: UUID,
        messageId: UUID
    ): Boolean {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        val message = chatMessageRepository.findByIdAndTripId(
            id = messageId,
            tripId = tripId
        ) ?: throw IllegalArgumentException(
            "Message not found"
        )

        if (message.deleted) {
            return true
        }

        /*
         * MVP rule:
         * - sender can delete their own message
         * - trip owner can delete any message
         */
        require(
            message.senderId == userId ||
                    tripAccessService.isOwner(
                        tripId = tripId,
                        userId = userId
                    )
        ) {
            "You do not have permission to delete this message"
        }

        message.deleted = true
        chatMessageRepository.save(message)

        activityLogService.log(
            tripId = tripId,
            userId = userId,
            action = "deleted a message",
            targetType = ActivityTarget.CHAT,
            targetName = "Chat"
        )

        return true
    }

    @Transactional(readOnly = true)
    fun getAttachments(
        userId: UUID,
        tripId: UUID,
        messageId: UUID
    ): List<ChatAttachment> {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        val message = chatMessageRepository.findByIdAndTripId(
            id = messageId,
            tripId = tripId
        ) ?: throw IllegalArgumentException(
            "Message not found"
        )

        require(!message.deleted) {
            "Message has been deleted"
        }

        return chatAttachmentRepository.findAllByMessageId(
            messageId
        )
    }

    private fun normalizePageSize(limit: Int?): Int {

        val requested = limit ?: DEFAULT_PAGE_SIZE

        require(requested > 0) {
            "Limit must be greater than zero"
        }

        return requested.coerceAtMost(MAX_PAGE_SIZE)
    }

    @Transactional(readOnly = true)
    fun getReplyMessage(
        userId: UUID,
        tripId: UUID,
        replyToId: UUID
    ): ChatMessage? {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        return chatMessageRepository.findByIdAndTripId(
            id = replyToId,
            tripId = tripId
        )
    }

    @Transactional(readOnly = true)
    fun getMessageForResponse(
        tripId: UUID,
        messageId: UUID
    ): ChatMessage? {
        return chatMessageRepository.findByIdAndTripId(
            id = messageId,
            tripId = tripId
        )
    }

    @Transactional(readOnly = true)
    fun getAttachmentsForMessages(
        userId: UUID,
        tripId: UUID,
        messageIds: Collection<UUID>
    ): Map<UUID, List<ChatAttachment>> {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        if (messageIds.isEmpty()) {
            return emptyMap()
        }

        return chatAttachmentRepository
            .findAllByMessageIdIn(messageIds)
            .groupBy { it.messageId }
    }

    @Transactional(readOnly = true)
    fun getMessagesByIds(
        tripId: UUID,
        messageIds: Collection<UUID>
    ): Map<UUID, ChatMessage> {

        if (messageIds.isEmpty()) {
            return emptyMap()
        }

        return chatMessageRepository
            .findAllById(messageIds)
            .filter { it.tripId == tripId }
            .associateBy { it.id }
    }
}