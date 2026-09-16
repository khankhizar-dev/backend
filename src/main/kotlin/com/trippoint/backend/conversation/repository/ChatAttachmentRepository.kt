package com.trippoint.backend.conversation.repository

import com.trippoint.backend.conversation.entity.ChatAttachment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChatAttachmentRepository :
    JpaRepository<ChatAttachment, UUID> {

    fun findAllByMessageId(
        messageId: UUID
    ): List<ChatAttachment>

    fun findAllByMessageIdIn(
        messageIds: Collection<UUID>
    ): List<ChatAttachment>

    fun deleteAllByMessageId(
        messageId: UUID
    )
}