package com.trippoint.backend.conversation.event

import com.trippoint.backend.conversation.entity.ChatMessage

data class ChatMessageCreatedEvent(
    val message: ChatMessage
)