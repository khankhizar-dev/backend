package com.trippoint.backend.conversation.graphql.input

data class MessagePaginationInput(
    val limit: Int? = null,
    val beforeCursor: String? = null
)
