package com.trippoint.backend.conversation.graphql

import com.trippoint.backend.conversation.entity.ChatAttachment
import com.trippoint.backend.conversation.entity.ChatMessage
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ChatMessageResponse(
    val id: UUID,
    val tripId: UUID,
    val senderId: UUID,
    val senderName: String,
    val senderPhotoUrl: String?,
    val content: String,
    val type: String,
    val attachments: List<ChatAttachmentResponse>,
    val replyToId: UUID?,
    val replyToContent: String?,
    val timestamp: String,
    val isMe: Boolean,
    val deleted: Boolean
) {

    companion object {

        private val formatter =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(
            message: ChatMessage,
            senderName: String,
            senderPhotoUrl: String?,
            replyToContent: String?,
            attachments: List<ChatAttachment>,
            currentUserId: UUID
        ): ChatMessageResponse {

            return ChatMessageResponse(
                id = message.id,
                tripId = message.tripId,
                senderId = message.senderId,
                senderName = senderName,
                senderPhotoUrl = senderPhotoUrl,
                content = message.content,
                type = message.messageType.name,
                attachments = attachments.map(
                    ChatAttachmentResponse::from
                ),
                replyToId = message.replyToId,
                replyToContent = replyToContent,
                timestamp = message.createdAt.format(formatter),
                isMe = message.senderId == currentUserId,
                deleted = message.deleted
            )
        }
    }
}

data class ChatAttachmentResponse(
    val id: UUID,
    val name: String,
    val url: String,
    val mimeType: String,
    val fileSize: Long
) {
    companion object {
        fun from(
            attachment: ChatAttachment
        ): ChatAttachmentResponse {
            return ChatAttachmentResponse(
                id = attachment.id,
                name = attachment.name,
                url = attachment.url,
                mimeType = attachment.mimeType,
                fileSize = attachment.fileSize
            )
        }
    }
}