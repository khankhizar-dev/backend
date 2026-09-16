package com.trippoint.backend.notification.graphql

import com.trippoint.backend.notification.entity.Notification
import com.trippoint.backend.notification.subscription.NotificationSubscriptionService
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import java.util.UUID

@Controller
class NotificationSubscriptionGraphQLController(
    private val notificationSubscriptionService: NotificationSubscriptionService
) {

    @SubscriptionMapping
    fun notificationReceived(): Flux<Notification> {
        val authentication =
            org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .authentication
                ?: throw IllegalStateException("Unauthenticated")

        val userId = UUID.fromString(authentication.name)

        return notificationSubscriptionService.subscribe(userId)
    }
}