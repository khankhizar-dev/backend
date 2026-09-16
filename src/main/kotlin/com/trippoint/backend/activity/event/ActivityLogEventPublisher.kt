package com.trippoint.backend.activity.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ActivityLogEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun publish(event: ActivityLogCreatedEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
