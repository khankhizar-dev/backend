package com.trippoint.backend.conversation.entity

import com.trippoint.backend.conversation.model.MessageType
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "chat_messages",
    indexes = [
        Index(
            name = "idx_chat_messages_trip_created",
            columnList = "trip_id,created_at"
        ),
        Index(
            name = "idx_chat_messages_trip_id",
            columnList = "trip_id,id"
        ),
        Index(
            name = "idx_chat_messages_reply_to",
            columnList = "reply_to_id"
        )
    ]
)
class ChatMessage(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "trip_id", nullable = false)
    var tripId: UUID,

    @Column(name = "sender_id", nullable = false)
    var senderId: UUID,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Enumerated(EnumType.STRING)
    @Column(
        name = "message_type",
        nullable = false,
        length = 20
    )
    var messageType: MessageType,

    @Column(name = "reply_to_id")
    var replyToId: UUID? = null,

    @Column(nullable = false)
    var deleted: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}