package com.trippoint.backend.notification.service

import com.trippoint.backend.notification.entity.Notification
import com.trippoint.backend.notification.model.NotificationCategory
import com.trippoint.backend.notification.repository.NotificationRepository
import com.trippoint.backend.notification.subscription.NotificationSubscriptionService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationSubscriptionService: NotificationSubscriptionService
) {

    @Transactional
    fun create(
        recipientUserId: UUID,
        actorUserId: UUID? = null,
        tripId: UUID? = null,
        category: NotificationCategory,
        type: com.trippoint.backend.notification.model.NotificationType,
        title: String,
        message: String,
        targetType: String? = null,
        targetId: UUID? = null,
        targetName: String? = null
    ): Notification {

        val notification = Notification(
            recipientUserId = recipientUserId,
            actorUserId = actorUserId,
            tripId = tripId,
            category = category,
            type = type,
            title = title,
            message = message,
            targetType = targetType,
            targetId = targetId,
            targetName = targetName
        )

        val savedNotification = notificationRepository.save(notification)

        notificationSubscriptionService.publish(savedNotification)

        return savedNotification
    }

    @Transactional(readOnly = true)
    fun getNotification(
        notificationId: UUID,
        userId: UUID
    ): Notification {
        return notificationRepository
            .findByIdAndRecipientUserId(notificationId, userId)
            ?.takeIf { !it.isDeleted }
            ?: throw IllegalArgumentException("Notification not found")
    }

    @Transactional(readOnly = true)
    fun getNotifications(
        userId: UUID,
        limit: Int = 20,
        category: NotificationCategory? = null
    ): List<Notification> {

        val safeLimit = limit.coerceIn(1, 100)
        val pageable = PageRequest.of(0, safeLimit)

        return if (category == null) {
            notificationRepository
                .findAllByRecipientUserIdAndIsDeletedFalseOrderByCreatedAtDescIdDesc(
                    userId,
                    pageable
                )
        } else {
            notificationRepository
                .findAllByRecipientUserIdAndCategoryAndIsDeletedFalseOrderByCreatedAtDescIdDesc(
                    userId,
                    category,
                    pageable
                )
        }
    }

    @Transactional(readOnly = true)
    fun getNotificationsBeforeCursor(
        userId: UUID,
        cursorNotificationId: UUID,
        limit: Int = 20,
        category: NotificationCategory? = null
    ): List<Notification> {

        val cursor = getNotification(cursorNotificationId, userId)
        val safeLimit = limit.coerceIn(1, 100)
        val pageable = PageRequest.of(0, safeLimit)

        return if (category == null) {
            notificationRepository.findNotificationsBeforeCursor(
                userId = userId,
                createdAt = cursor.createdAt,
                notificationId = cursor.id,
                pageable = pageable
            )
        } else {
            notificationRepository.findNotificationsBeforeCursorByCategory(
                userId = userId,
                category = category,
                createdAt = cursor.createdAt,
                notificationId = cursor.id,
                pageable = pageable
            )
        }
    }

    @Transactional(readOnly = true)
    fun getUnreadCount(userId: UUID): Long {
        return notificationRepository
            .countByRecipientUserIdAndIsReadFalseAndIsDeletedFalse(userId)
    }

    @Transactional
    fun markAsRead(
        notificationId: UUID,
        userId: UUID
    ): Notification {

        val notification = getNotification(notificationId, userId)

        if (!notification.isRead) {
            notification.isRead = true
            notification.readAt = LocalDateTime.now()
        }

        return notificationRepository.save(notification)
    }

    @Transactional
    fun markAllAsRead(userId: UUID): Int {

        val notifications = notificationRepository
            .findAllByRecipientUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDescIdDesc(
                userId,
                PageRequest.of(0, 1000)
            )

        if (notifications.isEmpty()) {
            return 0
        }

        val now = LocalDateTime.now()

        notifications.forEach {
            it.isRead = true
            it.readAt = now
        }

        notificationRepository.saveAll(notifications)

        return notifications.size
    }

    @Transactional
    fun archive(
        notificationId: UUID,
        userId: UUID
    ): Notification {

        val notification = getNotification(notificationId, userId)

        if (!notification.isArchived) {
            notification.isArchived = true
            notification.archivedAt = LocalDateTime.now()
        }

        return notificationRepository.save(notification)
    }

    @Transactional
    fun snooze(
        notificationId: UUID,
        userId: UUID,
        snoozedUntil: LocalDateTime
    ): Notification {

        require(snoozedUntil.isAfter(LocalDateTime.now())) {
            "Snooze time must be in the future"
        }

        val notification = getNotification(notificationId, userId)

        notification.snoozedUntil = snoozedUntil

        return notificationRepository.save(notification)
    }

    @Transactional
    fun clearSnooze(
        notificationId: UUID,
        userId: UUID
    ): Notification {

        val notification = getNotification(notificationId, userId)

        notification.snoozedUntil = null

        return notificationRepository.save(notification)
    }

    @Transactional
    fun delete(
        notificationId: UUID,
        userId: UUID
    ): Notification {

        val notification = getNotification(notificationId, userId)

        notification.isDeleted = true
        notification.deletedAt = LocalDateTime.now()

        return notificationRepository.save(notification)
    }

    @Transactional
    fun clearAll(userId: UUID): Int {

        val notifications = notificationRepository
            .findAllByRecipientUserIdAndIsDeletedFalseOrderByCreatedAtDescIdDesc(
                userId,
                PageRequest.of(0, 1000)
            )

        if (notifications.isEmpty()) {
            return 0
        }

        val now = LocalDateTime.now()

        notifications.forEach {
            it.isDeleted = true
            it.deletedAt = now
        }

        notificationRepository.saveAll(notifications)

        return notifications.size
    }
}