package com.trippoint.backend.notification.repository

import com.trippoint.backend.notification.entity.Notification
import com.trippoint.backend.notification.model.NotificationCategory
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface NotificationRepository : JpaRepository<Notification, UUID> {

    fun findByIdAndRecipientUserId(
        id: UUID,
        recipientUserId: UUID
    ): Notification?

    fun findAllByRecipientUserIdAndIsDeletedFalseOrderByCreatedAtDescIdDesc(
        recipientUserId: UUID,
        pageable: Pageable
    ): List<Notification>

    fun findAllByRecipientUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDescIdDesc(
        recipientUserId: UUID,
        pageable: Pageable
    ): List<Notification>

    fun findAllByRecipientUserIdAndCategoryAndIsDeletedFalseOrderByCreatedAtDescIdDesc(
        recipientUserId: UUID,
        category: NotificationCategory,
        pageable: Pageable
    ): List<Notification>

    fun countByRecipientUserIdAndIsReadFalseAndIsDeletedFalse(
        recipientUserId: UUID
    ): Long

    @Query(
        """
        SELECT n
        FROM Notification n
        WHERE n.recipientUserId = :userId
          AND n.isDeleted = false
          AND (
              n.createdAt < :createdAt
              OR (
                  n.createdAt = :createdAt
                  AND n.id < :notificationId
              )
          )
        ORDER BY n.createdAt DESC, n.id DESC
        """
    )
    fun findNotificationsBeforeCursor(
        @Param("userId") userId: UUID,
        @Param("createdAt") createdAt: LocalDateTime,
        @Param("notificationId") notificationId: UUID,
        pageable: Pageable
    ): List<Notification>

    @Query(
        """
        SELECT n
        FROM Notification n
        WHERE n.recipientUserId = :userId
          AND n.isDeleted = false
          AND n.category = :category
          AND (
              n.createdAt < :createdAt
              OR (
                  n.createdAt = :createdAt
                  AND n.id < :notificationId
              )
          )
        ORDER BY n.createdAt DESC, n.id DESC
        """
    )
    fun findNotificationsBeforeCursorByCategory(
        @Param("userId") userId: UUID,
        @Param("category") category: NotificationCategory,
        @Param("createdAt") createdAt: LocalDateTime,
        @Param("notificationId") notificationId: UUID,
        pageable: Pageable
    ): List<Notification>
}