package com.trippoint.backend.conversation.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.auth.service.UserService
import com.trippoint.backend.conversation.graphql.input.MessagePaginationInput
import com.trippoint.backend.conversation.graphql.input.SendMessageInput
import com.trippoint.backend.conversation.service.ConversationService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class ConversationGraphQLController(
    private val conversationService: ConversationService,
    private val userService: UserService
) {

    @QueryMapping
    fun messages(
        @Argument tripId: UUID,
        @Argument pagination: MessagePaginationInput?
    ): List<ChatMessageResponse> {

        val userId = currentUserId()

        val messages = conversationService.getMessages(
            userId = userId,
            tripId = tripId,
            limit = pagination?.limit,
            beforeCursor = pagination?.beforeCursor?.let(UUID::fromString)
        )

        val senderIds = messages
            .map { it.senderId }
            .toSet()

        val senders = userService.getProfiles(senderIds)

        val replyIds = messages
            .mapNotNull { it.replyToId }
            .toSet()

        val replies = conversationService.getMessagesByIds(
            tripId = tripId,
            messageIds = replyIds
        )

        val attachments = conversationService.getAttachmentsForMessages(
            userId = userId,
            tripId = tripId,
            messageIds = messages.map { it.id }
        )

        return messages.map { message ->

            val sender = senders[message.senderId]
                ?: throw IllegalArgumentException(
                    "Sender not found"
                )

            val replyContent = message.replyToId
                ?.let { replies[it]?.content }

            ChatMessageResponse.from(
                message = message,
                senderName = sender.fullName
                    ?: listOfNotNull(
                        sender.firstName,
                        sender.lastName
                    ).joinToString(" "),
                senderPhotoUrl = sender.profilePhotoUrl,
                replyToContent = replyContent,
                attachments = attachments[message.id].orEmpty(),
                currentUserId = userId
            )
        }
    }

    @MutationMapping
    fun sendMessage(
        @Argument tripId: UUID,
        @Argument input: SendMessageInput
    ): ChatMessageResponse {

        val userId = currentUserId()

        val message = conversationService.sendMessage(
            userId = userId,
            tripId = tripId,
            content = input.content,
            type = input.type,
            replyToId = input.replyToId?.let(UUID::fromString)
        )

        val sender = userService.getProfile(userId)

        val replyContent = message.replyToId?.let { replyId ->
            conversationService.getMessageForResponse(
                tripId = tripId,
                messageId = replyId
            )?.content
        }

        val attachments = conversationService.getAttachments(
            userId = userId,
            tripId = tripId,
            messageId = message.id
        )

        return ChatMessageResponse.from(
            message = message,
            senderName = sender.fullName
                ?: listOfNotNull(
                    sender.firstName,
                    sender.lastName
                ).joinToString(" "),
            senderPhotoUrl = sender.profilePhotoUrl,
            replyToContent = replyContent,
            attachments = attachments,
            currentUserId = userId
        )
    }

    @MutationMapping
    fun deleteMessage(
        @Argument tripId: UUID,
        @Argument messageId: UUID
    ): Boolean {

        return conversationService.deleteMessage(
            userId = currentUserId(),
            tripId = tripId,
            messageId = messageId
        )
    }

    private fun currentUserId(): UUID {

        val authentication =
            SecurityContextHolder.getContext().authentication

        val principal = authentication?.principal

        require(principal is UserPrincipal) {
            "Authentication required"
        }

        return principal.userId
    }
}