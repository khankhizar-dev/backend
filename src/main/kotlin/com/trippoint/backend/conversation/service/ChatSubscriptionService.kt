package com.trippoint.backend.conversation.service

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.conversation.entity.ChatMessage
import com.trippoint.backend.conversation.event.ChatMessageCreatedEvent
import com.trippoint.backend.trip.service.TripAccessService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class ChatSubscriptionService(
    private val tripAccessService: TripAccessService
) {

    private val sinks =
        ConcurrentHashMap<UUID, Sinks.Many<ChatMessage>>()

    fun subscribe(
        userId: UUID,
        tripId: UUID
    ): Flux<ChatMessage> {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        val sink = sinks.computeIfAbsent(tripId) {
            Sinks.many().multicast().directBestEffort()
        }

        return sink.asFlux()
            .doFinally {
                removeSinkIfUnused(tripId, sink)
            }
    }

    @EventListener
    fun onChatMessageCreated(
        event: ChatMessageCreatedEvent
    ) {

        val tripId = event.message.tripId

        sinks[tripId]?.tryEmitNext(
            event.message
        )
    }

    private fun removeSinkIfUnused(
        tripId: UUID,
        sink: Sinks.Many<ChatMessage>
    ) {

        if (sink.currentSubscriberCount() == 0) {
            sinks.remove(tripId, sink)
        }
    }
}