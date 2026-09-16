package com.trippoint.backend.notification.graphql

import com.trippoint.backend.auth.security.UserPrincipal
import com.trippoint.backend.notification.model.NotificationCategory
import com.trippoint.backend.notification.service.NotificationService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.time.LocalDateTime
import java.util.UUID

@Controller
class NotificationGraphQLController(
    private val notificationService: NotificationService
) {

    private fun currentUserId(): UUID {
        val principal = SecurityContextHolder
            .getContext()
            .authentication
            ?.principal as? UserPrincipal
            ?: throw IllegalStateException("User is not authenticated")

        return principal.userId
    }

    @QueryMapping
    fun notifications(
        @Argument limit: Int?,
        @Argument category: NotificationCategory?,
        @Argument beforeCursor: UUID?
    ): List<NotificationResponse> {

        val userId = currentUserId()

        val notifications = if (beforeCursor == null) {
            notificationService.getNotifications(
                userId = userId,
                limit = limit ?: 20,
                category = category
            )
        } else {
            notificationService.getNotificationsBeforeCursor(
                userId = userId,
                cursorNotificationId = beforeCursor,
                limit = limit ?: 20,
                category = category
            )
        }

        return notifications.map(NotificationResponse::from)
    }

    @QueryMapping
    fun notification(
        @Argument id: UUID
    ): NotificationResponse {

        val notification = notificationService.getNotification(
            notificationId = id,
            userId = currentUserId()
        )

        return NotificationResponse.from(notification)
    }

    @QueryMapping
    fun unreadNotificationCount(): Int {
        return notificationService
            .getUnreadCount(currentUserId())
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    @MutationMapping
    fun markNotificationRead(
        @Argument id: UUID
    ): NotificationResponse {

        return NotificationResponse.from(
            notificationService.markAsRead(
                notificationId = id,
                userId = currentUserId()
            )
        )
    }

    @MutationMapping
    fun markAllNotificationsRead(): NotificationActionResult {

        val count = notificationService.markAllAsRead(
            currentUserId()
        )

        return NotificationActionResult(
            success = true,
            count = count
        )
    }

    @MutationMapping
    fun archiveNotification(
        @Argument id: UUID
    ): NotificationResponse {

        return NotificationResponse.from(
            notificationService.archive(
                notificationId = id,
                userId = currentUserId()
            )
        )
    }

    @MutationMapping
    fun snoozeNotification(
        @Argument id: UUID,
        @Argument snoozedUntil: String
    ): NotificationResponse {

        val snoozeTime = try {
            LocalDateTime.parse(snoozedUntil)
        } catch (ex: Exception) {
            throw IllegalArgumentException(
                "Invalid snoozedUntil. Expected ISO-8601 local date-time."
            )
        }

        return NotificationResponse.from(
            notificationService.snooze(
                notificationId = id,
                userId = currentUserId(),
                snoozedUntil = snoozeTime
            )
        )
    }

    @MutationMapping
    fun clearNotificationSnooze(
        @Argument id: UUID
    ): NotificationResponse {

        return NotificationResponse.from(
            notificationService.clearSnooze(
                notificationId = id,
                userId = currentUserId()
            )
        )
    }

    @MutationMapping
    fun deleteNotification(
        @Argument id: UUID
    ): NotificationResponse {

        return NotificationResponse.from(
            notificationService.delete(
                notificationId = id,
                userId = currentUserId()
            )
        )
    }

    @MutationMapping
    fun clearAllNotifications(): NotificationActionResult {

        val count = notificationService.clearAll(
            currentUserId()
        )

        return NotificationActionResult(
            success = true,
            count = count
        )
    }
}