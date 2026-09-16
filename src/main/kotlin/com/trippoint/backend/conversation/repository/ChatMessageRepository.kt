package com.trippoint.backend.conversation.repository

import com.trippoint.backend.conversation.entity.ChatMessage
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface ChatMessageRepository : JpaRepository<ChatMessage, UUID> {

    fun findByIdAndTripId(
        id: UUID,
        tripId: UUID
    ): ChatMessage?

    fun findAllByTripIdAndDeletedFalseOrderByCreatedAtDescIdDesc(
        tripId: UUID,
        pageable: Pageable
    ): List<ChatMessage>

    @Query(
        """
        SELECT m
        FROM ChatMessage m
        WHERE m.tripId = :tripId
          AND m.deleted = false
          AND (
              m.createdAt < :createdAt
              OR (
                  m.createdAt = :createdAt
                  AND m.id < :messageId
              )
          )
        ORDER BY m.createdAt DESC, m.id DESC
        """
    )
    fun findMessagesBeforeCursor(
        @Param("tripId") tripId: UUID,
        @Param("createdAt") createdAt: LocalDateTime,
        @Param("messageId") messageId: UUID,
        pageable: Pageable
    ): List<ChatMessage>
}