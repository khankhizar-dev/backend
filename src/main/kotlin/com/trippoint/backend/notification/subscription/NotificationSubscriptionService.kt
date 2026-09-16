package com.trippoint.backend.notification.subscription

import com.trippoint.backend.notification.entity.Notification
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class NotificationSubscriptionService {

    private val sinks = ConcurrentHashMap<UUID, Sinks.Many<Notification>>()

    fun subscribe(userId: UUID): Flux<Notification> {
        val sink = sinks.computeIfAbsent(userId) {
            Sinks.many().multicast().onBackpressureBuffer()
        }

        return sink.asFlux()
            .doFinally {
                removeIfUnused(userId, sink)
            }
    }

    fun publish(notification: Notification) {
        sinks[notification.recipientUserId]
            ?.tryEmitNext(notification)
    }

    fun currentSubscriberCount(): Int {
        return sinks.values.sumOf { it.currentSubscriberCount() }
    }

    fun currentSubscriberCount(userId: UUID): Int {
        return sinks[userId]?.currentSubscriberCount() ?: 0
    }

    private fun removeIfUnused(
        userId: UUID,
        sink: Sinks.Many<Notification>
    ) {
        if (sink.currentSubscriberCount() == 0) {
            sinks.remove(userId, sink)
        }
    }

    @PreDestroy
    fun shutdown() {
        sinks.values.forEach { it.tryEmitComplete() }
        sinks.clear()
    }
}