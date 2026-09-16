package com.trippoint.backend.conversation.graphql.input

import com.trippoint.backend.conversation.model.MessageType

data class SendMessageInput(
    val content: String,
    val type: MessageType,
    val replyToId: String? = null
)
