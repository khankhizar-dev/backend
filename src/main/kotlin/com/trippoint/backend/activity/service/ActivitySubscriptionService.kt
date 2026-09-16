package com.trippoint.backend.activity.service

import com.trippoint.backend.activity.entity.ActivityLog
import com.trippoint.backend.activity.event.ActivityLogCreatedEvent
import com.trippoint.backend.trip.service.TripAccessService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class ActivitySubscriptionService(
    private val tripAccessService: TripAccessService
) {

    private val sinks =
        ConcurrentHashMap<UUID, Sinks.Many<ActivityLog>>()

    fun subscribe(
        userId: UUID,
        tripId: UUID
    ): Flux<ActivityLog> {

        tripAccessService.requireMemberAccess(
            tripId = tripId,
            userId = userId
        )

        val sink = sinks.computeIfAbsent(tripId) {
            Sinks.many()
                .multicast()
                .directBestEffort()
        }

        return sink
            .asFlux()
            .doFinally {
                removeSinkIfUnused(
                    tripId = tripId,
                    sink = sink
                )
            }
    }

    @EventListener
    fun onActivityLogCreated(
        event: ActivityLogCreatedEvent
    ) {
        val tripId = event.activity.tripId

        sinks[tripId]?.tryEmitNext(
            event.activity
        )
    }

    private fun removeSinkIfUnused(
        tripId: UUID,
        sink: Sinks.Many<ActivityLog>
    ) {
        if (sink.currentSubscriberCount() == 0) {
            sinks.remove(
                tripId,
                sink
            )
        }
    }
}