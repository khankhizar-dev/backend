package com.trippoint.backend.conversation.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "chat_attachments",
    indexes = [
        Index(
            name = "idx_chat_attachments_message",
            columnList = "message_id"
        )
    ]
)
class ChatAttachment(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "message_id", nullable = false)
    var messageId: UUID,

    @Column(nullable = false, length = 500)
    var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var url: String,

    @Column(name = "mime_type", nullable = false, length = 100)
    var mimeType: String,

    @Column(name = "file_size", nullable = false)
    var fileSize: Long,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)