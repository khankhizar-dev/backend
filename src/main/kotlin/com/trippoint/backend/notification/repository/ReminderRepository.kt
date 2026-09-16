package com.trippoint.backend.notification.repository

import com.trippoint.backend.notification.entity.Reminder
import com.trippoint.backend.notification.model.ReminderStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface ReminderRepository : JpaRepository<Reminder, UUID> {

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID
    ): Reminder?

    fun findAllByUserIdOrderByDueAtAscIdAsc(
        userId: UUID,
        pageable: Pageable
    ): List<Reminder>

    fun findAllByUserIdAndStatusOrderByDueAtAscIdAsc(
        userId: UUID,
        status: ReminderStatus,
        pageable: Pageable
    ): List<Reminder>

    fun findAllByUserIdAndTripIdOrderByDueAtAscIdAsc(
        userId: UUID,
        tripId: UUID,
        pageable: Pageable
    ): List<Reminder>

    fun findAllByStatusAndDueAtBefore(
        status: ReminderStatus,
        dueAt: LocalDateTime
    ): List<Reminder>
}