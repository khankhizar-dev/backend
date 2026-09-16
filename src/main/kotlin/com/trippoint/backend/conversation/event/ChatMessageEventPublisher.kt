package com.trippoint.backend.conversation.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ChatMessageEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun publish(event: ChatMessageCreatedEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}